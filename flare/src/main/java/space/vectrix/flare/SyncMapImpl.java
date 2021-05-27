/*
 * This file is part of flare, licensed under the MIT License (MIT).
 *
 * Copyright (c) vectrix.space <https://vectrix.space/>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package space.vectrix.flare;

import static java.util.Objects.requireNonNull;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Consumer;
import java.util.function.IntFunction;

/* package */ final class SyncMapImpl<K, V> extends AbstractMap<K, V> implements SyncMap<K, V> {
  private final Object lock = new Object();
  private final IntFunction<Map<K, ExpungingValue<V>>> function;
  private final int initialCapacity;
  private volatile Map<K, ExpungingValue<V>> read;
  private volatile boolean readAmended;
  private int readMisses;
  private Map<K, ExpungingValue<V>> dirty;
  private EntrySet entrySet;

  /* package */ SyncMapImpl(final @NonNull IntFunction<Map<K, ExpungingValue<V>>> function, final int initialCapacity) {
    this.function = function;
    this.read = function.apply(initialCapacity);
    this.initialCapacity = initialCapacity;
  }

  @Override
  public int size() {
    this.promoteIfNeeded();
    return this.getSize(this.read);
  }

  private int getSize(final @NonNull Map<K, ExpungingValue<V>> map) {
    int size = 0;
    for(final ExpungingValue<V> value : map.values()) {
      if(value.exists()) size++;
    }
    return size;
  }

  @SuppressWarnings("SuspiciousMethodCalls")
  private @Nullable ExpungingValue<V> getValue(final @Nullable Object key) {
    ExpungingValue<V> entry = this.read.get(key);
    if(entry == null && this.readAmended) {
      synchronized(this.lock) {
        if((entry = this.read.get(key)) == null && this.readAmended && this.dirty != null) {
          entry = this.dirty.get(key);
          // The slow path should be avoided, even if the value does
          // not match or is present. So we mark a miss, to eventually
          // promote and take a faster path.
          this.missLocked();
        }
      }
    }
    return entry;
  }

  @Override
  public boolean containsKey(final @Nullable Object key) {
    final ExpungingValue<V> entry = this.getValue(key);
    return entry != null && entry.exists();
  }

  @Override
  public V get(final @Nullable Object key) {
    final ExpungingValue<V> entry = this.getValue(key);
    return entry != null ? entry.get() : null;
  }

  @Override
  public V put(final @Nullable K key, final @NonNull V value) {
    requireNonNull(value, "value");
    final ExpungingValue<V> entry = this.read.get(key);
    final V previous = entry != null ? entry.get() : null;
    if(entry != null && entry.trySet(value)) return previous;
    return this.putDirty(key, value, false);
  }

  private V putDirty(final @Nullable K key, final @NonNull V value, final boolean present) {
    ExpungingValue<V> entry;
    V previous = null;
    synchronized(this.lock) {
      if((entry = this.read.get(key)) != null) {
        previous = entry.get();
        if(entry.tryUnexpungeAndSet(value)) {
          this.dirty.put(key, entry);
        } else {
          entry.set(value);
        }
      } else {
        entry = this.dirty != null ? this.dirty.get(key) : null;
        if(entry != null) {
          previous = entry.get();
          entry.set(value);
          this.missLocked();
        } else if(!present) {
          if(!this.readAmended) {
            this.dirtyLocked();
            this.readAmended = true;
          }
          if(this.dirty != null) {
            entry = this.dirty.put(key, new ExpungingValueImpl<>(value));
            if(entry != null) {
              previous = entry.get();
            }
          }
        }
      }
    }
    return previous;
  }

  @Override
  @SuppressWarnings("SuspiciousMethodCalls")
  public V remove(final @Nullable Object key) {
    ExpungingValue<V> entry = this.read.get(key);
    if(entry == null && this.readAmended) {
      synchronized(this.lock) {
        if((entry = this.read.get(key)) == null && this.readAmended && this.dirty != null) {
          entry = this.dirty.remove(key);
          // The slow path should be avoided, even if the value does
          // not match or is present. So we mark a miss, to eventually
          // promote and take a faster path.
          this.missLocked();
        }
      }
    }
    return entry != null ? entry.clear() : null;
  }

  @Override
  @SuppressWarnings("SuspiciousMethodCalls")
  public boolean remove(final @Nullable Object key, final @NonNull Object value) {
    requireNonNull(value, "value");
    ExpungingValue<V> entry = this.read.get(key);
    if(entry == null && this.readAmended) {
      synchronized(this.lock) {
        if((entry = this.read.get(key)) == null && this.readAmended && this.dirty != null) {
          if((entry = this.dirty.get(key)) != null && entry.replace(value, null)) {
            entry = this.dirty.remove(key);
          } else {
            entry = null;
          }
          // The slow path should be avoided, even if the value does
          // not match or is present. So we mark a miss, to eventually
          // promote and take a faster path.
          this.missLocked();
          return entry != null;
        }
      }
    }
    return entry != null && entry.replace(value, null);
  }

  @Override
  public V putIfAbsent(final @Nullable K key, final @NonNull V value) {
    requireNonNull(value, "value");
    ExpungingValue<V> entry = this.read.get(key);
    Map.Entry<Boolean, V> result = null;
    if(entry != null) {
      result = entry.putIfAbsent(value);
      if(result.getKey() == Boolean.TRUE) {
        return result.getValue();
      }
    }
    synchronized(this.lock) {
      if((entry = this.read.get(key)) != null) {
        if(entry.tryUnexpungeAndSet(value)) {
          this.dirty.put(key, entry);
        }
      } else {
        entry = this.dirty != null ? this.dirty.get(key) : null;
        if(entry != null) {
          result = entry.putIfAbsent(value);
          this.missLocked();
        } else {
          if(!this.readAmended) {
            this.dirtyLocked();
            this.readAmended = true;
          }
          if(this.dirty != null) {
            this.dirty.put(key, new ExpungingValueImpl<>(value));
          }
        }
      }
    }
    return result != null ? result.getValue() : null;
  }

  @Override
  public V replace(final @Nullable K key, final @NonNull V value) {
    requireNonNull(value, "value");
    final ExpungingValue<V> entry = this.read.get(key);
    final V previous = entry != null ? entry.get() : null;
    if((entry != null && entry.trySet(value)) || !this.readAmended) return previous;
    return this.putDirty(key, value, true);
  }

  @Override
  public boolean replace(final @Nullable K key, final @NonNull V oldValue, final @NonNull V newValue) {
    requireNonNull(oldValue, "oldValue");
    requireNonNull(newValue, "newValue");
    ExpungingValue<V> entry = this.read.get(key);
    if(entry == null && this.readAmended) {
      synchronized(this.lock) {
        if((entry = this.read.get(key)) == null && this.readAmended && this.dirty != null) {
          if((entry = this.dirty.get(key)) != null && !entry.replace(oldValue, newValue)) {
            entry = null;
          }
          // The slow path should be avoided, even if the value does
          // not match or is present. So we mark a miss, to eventually
          // promote and take a faster path.
          this.missLocked();
          return entry != null;
        }
      }
    }
    return entry != null && entry.replace(oldValue, newValue);
  }

  @Override
  public void clear() {
    synchronized(this.lock) {
      this.read = this.function.apply(this.initialCapacity);
      this.dirty = null;
      this.readMisses = 0;
      this.readAmended = false;
    }
  }

  @Override
  public @NonNull Set<Entry<K, V>> entrySet() {
    if(this.entrySet != null) return this.entrySet;
    return this.entrySet = new EntrySet();
  }

  private void promoteIfNeeded() {
    if(this.readAmended) {
      synchronized(this.lock) {
        if(this.readAmended && this.dirty != null) {
          this.promoteLocked();
        }
      }
    }
  }

  private void promoteLocked() {
    if(this.dirty != null) {
      this.read = this.dirty;
    }
    this.dirty = null;
    this.readMisses = 0;
    this.readAmended = false;
  }

  private void missLocked() {
    this.readMisses++;
    final int length = this.dirty != null ? this.dirty.size() : 0;
    if(this.readMisses > length) {
      this.promoteLocked();
    }
  }

  private void dirtyLocked() {
    if(this.dirty == null) {
      this.dirty = this.function.apply(this.read.size());
      for(final Map.Entry<K, ExpungingValue<V>> entry : this.read.entrySet()) {
        if(!entry.getValue().tryMarkExpunged()) {
          this.dirty.put(entry.getKey(), entry.getValue());
        }
      }
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private final static class ExpungingValueImpl<V> implements SyncMap.ExpungingValue<V> {
    private static final Object EXPUNGED = new Object();
    private static final AtomicReferenceFieldUpdater<ExpungingValueImpl, Object> valueUpdater =
      AtomicReferenceFieldUpdater.newUpdater(ExpungingValueImpl.class, Object.class, "value");
    private volatile Object value;

    private ExpungingValueImpl(final @NonNull V value) {
      this.value = value;
    }

    @Override
    public @Nullable V get() {
      final Object value = ExpungingValueImpl.valueUpdater.get(this);
      return value == ExpungingValueImpl.EXPUNGED ? null : (V) value;
    }

    @Override
    public @NonNull Entry<Boolean, V> putIfAbsent(final @NonNull V value) {
      for(; ; ) {
        final Object previous = ExpungingValueImpl.valueUpdater.get(this);
        if(previous == ExpungingValueImpl.EXPUNGED) {
          return new AbstractMap.SimpleImmutableEntry<>(Boolean.FALSE, null);
        }
        if(previous != null) {
          return new AbstractMap.SimpleImmutableEntry<>(Boolean.TRUE, (V) previous);
        }
        if(ExpungingValueImpl.valueUpdater.compareAndSet(this, null, value)) {
          return new AbstractMap.SimpleImmutableEntry<>(Boolean.TRUE, null);
        }
      }
    }

    @Override
    public boolean expunged() {
      return ExpungingValueImpl.valueUpdater.get(this) == ExpungingValueImpl.EXPUNGED;
    }

    @Override
    public boolean exists() {
      final Object value = ExpungingValueImpl.valueUpdater.get(this);
      return value != null && value != ExpungingValueImpl.EXPUNGED;
    }

    @Override
    public void set(final @NonNull V value) {
      ExpungingValueImpl.valueUpdater.set(this, value);
    }

    @Override
    public boolean replace(final @NonNull Object compare, final @Nullable V newValue) {
      for(; ; ) {
        final Object value = ExpungingValueImpl.valueUpdater.get(this);
        if(value == ExpungingValueImpl.EXPUNGED || !Objects.equals(value, compare)) return false;
        if(ExpungingValueImpl.valueUpdater.compareAndSet(this, value, newValue)) return true;
      }
    }

    @Override
    public @Nullable V clear() {
      for(; ; ) {
        final Object value = ExpungingValueImpl.valueUpdater.get(this);
        if(value == null || value == ExpungingValueImpl.EXPUNGED) return null;
        if(ExpungingValueImpl.valueUpdater.compareAndSet(this, value, null)) return (V) value;
      }
    }

    @Override
    public boolean trySet(final @NonNull V value) {
      for(; ; ) {
        final Object present = ExpungingValueImpl.valueUpdater.get(this);
        if(present == ExpungingValueImpl.EXPUNGED) return false;
        if(ExpungingValueImpl.valueUpdater.compareAndSet(this, present, value)) return true;
      }
    }

    @Override
    public boolean tryMarkExpunged() {
      Object value = ExpungingValueImpl.valueUpdater.get(this);
      while(value == null) {
        if(ExpungingValueImpl.valueUpdater.compareAndSet(this, null, ExpungingValueImpl.EXPUNGED)) return true;
        value = ExpungingValueImpl.valueUpdater.get(this);
      }
      return false;
    }

    @Override
    public boolean tryUnexpungeAndSet(final @Nullable V value) {
      return ExpungingValueImpl.valueUpdater.compareAndSet(this, ExpungingValueImpl.EXPUNGED, value);
    }

    @Override
    public String toString() {
      return "SyncMapImpl.ExpungingValue{value=" + this.get() + "}";
    }
  }

  private final class MapEntry implements Map.Entry<K, V> {
    private final K key;

    private MapEntry(final Map.@NonNull Entry<K, ExpungingValue<V>> entry) {
      this.key = entry.getKey();
    }

    @Override
    public @NonNull K getKey() {
      return this.key;
    }

    @Override
    public @Nullable V getValue() {
      return SyncMapImpl.this.get(this.key);
    }

    @Override
    public @Nullable V setValue(final @NonNull V value) {
      return SyncMapImpl.this.put(this.key, value);
    }

    @Override
    public @NonNull String toString() {
      return "SyncMapImpl.MapEntry{key=" + this.getKey() + ", value=" + this.getValue() + "}";
    }

    @Override
    public boolean equals(final @Nullable Object other) {
      if(this == other) return true;
      if(!(other instanceof Map.Entry)) return false;
      final Map.Entry<?, ?> that = (Map.Entry<?, ?>) other;
      return Objects.equals(this.getKey(), that.getKey())
        && Objects.equals(this.getValue(), that.getValue());
    }

    @Override
    public int hashCode() {
      return Objects.hash(this.getKey(), this.getValue());
    }
  }

  private final class EntrySet extends AbstractSet<Map.Entry<K, V>> {
    @Override
    public int size() {
      return SyncMapImpl.this.size();
    }

    @Override
    public boolean contains(final @Nullable Object entry) {
      if(!(entry instanceof Map.Entry)) return false;
      final Map.Entry<?, ?> mapEntry = (Entry<?, ?>) entry;
      final V value = SyncMapImpl.this.get(mapEntry.getKey());
      return value != null && Objects.equals(value, mapEntry.getValue());
    }

    @Override
    public boolean remove(final @Nullable Object entry) {
      if(!(entry instanceof Map.Entry)) return false;
      final Map.Entry<?, ?> mapEntry = (Entry<?, ?>) entry;
      return SyncMapImpl.this.remove(mapEntry.getKey()) != null;
    }

    @Override
    public void clear() {
      SyncMapImpl.this.clear();
    }

    @Override
    public @NonNull Iterator<Map.Entry<K, V>> iterator() {
      SyncMapImpl.this.promoteIfNeeded();
      return new EntryIterator(SyncMapImpl.this.read.entrySet().iterator());
    }
  }

  private final class EntryIterator implements Iterator<Map.Entry<K, V>> {
    private final Iterator<Map.Entry<K, ExpungingValue<V>>> backingIterator;
    private Map.Entry<K, V> next;
    private Map.Entry<K, V> current;

    private EntryIterator(final @NonNull Iterator<Map.Entry<K, ExpungingValue<V>>> backingIterator) {
      this.backingIterator = backingIterator;
      final Map.Entry<K, ExpungingValue<V>> entry = this.getNextValue();
      this.next = (entry != null ? new MapEntry(entry) : null);
    }

    private Map.Entry<K, ExpungingValue<V>> getNextValue() {
      Map.Entry<K, ExpungingValue<V>> entry = null;
      while(this.backingIterator.hasNext() && entry == null) {
        final ExpungingValue<V> value = (entry = this.backingIterator.next()).getValue();
        if(!value.exists()) {
          entry = null;
        }
      }
      return entry;
    }

    @Override
    public boolean hasNext() {
      return this.next != null;
    }

    @Override
    public Map.@NonNull Entry<K, V> next() {
      this.current = this.next;
      final Map.Entry<K, ExpungingValue<V>> entry = this.getNextValue();
      this.next = (entry != null ? new MapEntry(entry) : null);
      if(this.current == null) throw new NoSuchElementException();
      return this.current;
    }

    @Override
    public void remove() {
      if(this.current == null) return;
      SyncMapImpl.this.remove(this.current.getKey());
    }

    @Override
    public void forEachRemaining(final @NonNull Consumer<? super Map.Entry<K, V>> action) {
      if(this.next != null) action.accept(this.next);
      this.backingIterator.forEachRemaining(entry -> {
        if(entry.getValue().exists()) {
          action.accept(new MapEntry(entry));
        }
      });
    }
  }
}

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
  /**
   * A single implicit lock, when dealing with {@code dirty} mutations.
   */
  private transient final Object lock = new Object();

  /**
   * The read only map, that does not require a lock and does not allow mutations.
   */
  private transient volatile Map<K, ExpungingValue<V>> read;

  /**
   * Represents whether the {@code dirty} map has changes the {@code read} map,
   * does not have yet.
   */
  private transient volatile boolean amended;

  /**
   * The read/write map, that requires a lock and allows mutations.
   */
  private transient Map<K, ExpungingValue<V>> dirty;

  /**
   * Represents the amount of times an attempt has been made to access the
   * {@code dirty} map while {@code amended} is {@code true}.
   */
  private transient int misses;

  private transient EntrySetView entrySet;

  private final IntFunction<Map<K, ExpungingValue<V>>> function;

  /* package */ SyncMapImpl(final @NonNull IntFunction<Map<K, ExpungingValue<V>>> function, final int initialCapacity) {
    this.function = function;
    this.read = function.apply(initialCapacity);
  }

  @Override
  public int size() {
    this.promoteIfNeeded();
    int size = 0;
    for(final ExpungingValue<V> value : this.read.values()) {
      if(value.exists()) size++;
    }
    return size;
  }

  @Override
  public boolean isEmpty() {
    this.promoteIfNeeded();
    for(final ExpungingValue<V> value : this.read.values()) {
      if(value.exists()) return false;
    }
    return true;
  }

  @Override
  public @Nullable V get(final @Nullable Object key) {
    ExpungingValue<V> entry;
    if((entry = this.read.get(key)) == null && this.amended) {
      synchronized(this.lock) {
        if((entry = this.read.get(key)) == null && this.amended && this.dirty != null) {
          entry = this.dirty.get(key);
          // The slow path should be avoided, even if the value does
          // not match or is present. So we mark a miss, to eventually
          // promote and take a faster path.
          this.missLocked();
        }
      }
    }
    return entry != null ? entry.get() : null;
  }

  @Override
  @SuppressWarnings("SuspiciousMethodCalls")
  public boolean containsKey(final @Nullable Object key) {
    ExpungingValue<V> entry;
    if((entry = this.read.get(key)) == null && this.amended) {
      synchronized(this.lock) {
        if((entry = this.read.get(key)) == null && this.amended && this.dirty != null) {
          entry = this.dirty.get(key);
          // The slow path should be avoided, even if the value does
          // not match or is present. So we mark a miss, to eventually
          // promote and take a faster path.
          this.missLocked();
        }
      }
    }
    return entry != null && entry.exists();
  }

  @Override
  public boolean containsValue(final @Nullable Object value) {
    for(final Entry<K, V> entry : this.entrySet()) {
      if(Objects.equals(entry.getValue(), value)) return true;
    }
    return false;
  }

  @Override
  public @Nullable V put(final @Nullable K key, final @NonNull V value) {
    return this.putValue(key, value, false);
  }

  @SuppressWarnings("ConstantConditions")
  private V putValue(final @Nullable K key, final @NonNull V value, final boolean onlyIfPresent) {
    requireNonNull(value, "value");
    ExpungingValue<V> entry;
    V previous;
    if((previous = (entry = this.read.get(key)) != null ? entry.get() : null) == null || !entry.trySet(value)) {
      synchronized(this.lock) {
        if((entry = this.read.get(key)) != null) {
          previous = entry.get();
          // If entry can be absent and previously expunged, add the
          // entry back to the dirty map.
          if(onlyIfPresent) {
            entry.trySet(value);
          } else if(entry.tryUnexpungeAndSet(value)) {
            this.dirty.put(key, entry);
          } else {
            entry.set(value);
          }
        } else if(this.dirty != null && (entry = this.dirty.get(key)) != null) {
          previous = entry.get();
          entry.set(value);
        } else if(!onlyIfPresent) {
          if(!this.amended) {
            // Adds the first new key to the dirty map and marks it as
            // amended.
            this.dirtyLocked();
            this.amended = true;
          }
          this.dirty.put(key, new ExpungingValueImpl<>(value));
        }
      }
    }
    return previous;
  }

  @Override
  public void putAll(final @NonNull Map<? extends K, ? extends V> map) {
    for(final Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
      this.putValue(entry.getKey(), entry.getValue(), false);
    }
  }

  @Override
  @SuppressWarnings("SuspiciousMethodCalls")
  public V remove(final @Nullable Object key) {
    ExpungingValue<V> entry;
    if((entry = this.read.get(key)) == null && this.amended) {
      synchronized(this.lock) {
        if((entry = this.read.get(key)) == null && this.amended && this.dirty != null) {
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
    ExpungingValue<V> entry;
    if((entry = this.read.get(key)) == null && this.amended) {
      synchronized(this.lock) {
        if((entry = this.read.get(key)) == null && this.amended && this.dirty != null) {
          final boolean present;
          if(present = (((entry = this.dirty.get(key))) != null && entry.replace(value, null))) {
            this.dirty.remove(key);
          }
          // The slow path should be avoided, even if the value does
          // not match or is present. So we mark a miss, to eventually
          // promote and take a faster path.
          this.missLocked();
          return present;
        }
      }
    }
    return entry != null && entry.replace(value, null);
  }

  @Override
  @SuppressWarnings("ConstantConditions")
  public V putIfAbsent(final @Nullable K key, final @NonNull V value) {
    requireNonNull(value, "value");
    ExpungingValue<V> entry;
    Map.Entry<Boolean, V> result;
    if((entry = this.read.get(key)) != null) {
      if((result = entry.putIfAbsent(value)).getKey() == Boolean.TRUE) {
        return result.getValue();
      }
    }
    synchronized(this.lock) {
      if((entry = this.read.get(key)) != null) {
        // The entry was previously expunged, which implies this entry
        // is not within the dirty map.
        if(entry.tryUnexpunge()) {
          this.dirty.put(key, entry);
        }
        result = entry.putIfAbsent(value);
      } else if(this.dirty != null && (entry = this.dirty.get(key)) != null) {
        result = entry.putIfAbsent(value);
        this.missLocked();
      } else {
        if(!this.amended) {
          // Adds the first new key to the dirty map and marks it as
          // amended.
          this.dirtyLocked();
          this.amended = true;
        }
        this.dirty.put(key, new ExpungingValueImpl<>(value));
        result = new SimpleImmutableEntry<>(Boolean.TRUE, null);
      }
    }
    return result.getValue();
  }

  @Override
  public V replace(final @Nullable K key, final @NonNull V value) {
    return this.putValue(key, value, true);
  }

  @Override
  public boolean replace(final @Nullable K key, final @NonNull V currentValue, final @NonNull V newValue) {
    requireNonNull(currentValue, "currentValue");
    requireNonNull(newValue, "newValue");
    ExpungingValue<V> entry;
    if((entry = this.read.get(key)) == null && this.amended) {
      synchronized(this.lock) {
        if((entry = this.read.get(key)) == null && this.amended && this.dirty != null) {
          final boolean present = ((entry = this.dirty.get(key)) != null && entry.replace(currentValue, newValue));
          // The slow path should be avoided, even if the value does
          // not match or is present. So we mark a miss, to eventually
          // promote and take a faster path.
          this.missLocked();
          return present;
        }
      }
    }
    return entry != null && entry.replace(currentValue, newValue);
  }

  @Override
  public void clear() {
    synchronized(this.lock) {
      this.read = this.function.apply(this.read.size());
      this.amended = false;
      this.dirty = null;
      this.misses = 0;
    }
  }

  @Override
  public @NonNull Set<Entry<K, V>> entrySet() {
    if(this.entrySet != null) return this.entrySet;
    return this.entrySet = new EntrySetView();
  }

  private void promoteIfNeeded() {
    if(this.amended) {
      synchronized(this.lock) {
        if(this.amended) {
          this.promoteLocked();
        }
      }
    }
  }

  private void promoteLocked() {
    if(this.dirty != null) {
      this.read = this.dirty;
    }
    this.amended = false;
    this.dirty = null;
    this.misses = 0;
  }

  private void missLocked() {
    if(this.misses++ >= (this.dirty != null ? this.dirty.size() : 0)) {
      this.promoteLocked();
    }
  }

  private void dirtyLocked() {
    if(this.dirty != null) return;
    this.dirty = this.function.apply(this.read.size());
    for(final Map.Entry<K, ExpungingValue<V>> entry : this.read.entrySet()) {
      if(!entry.getValue().tryMarkExpunged()) {
        this.dirty.put(entry.getKey(), entry.getValue());
      }
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private final static class ExpungingValueImpl<V> implements SyncMap.ExpungingValue<V> {
    private static final AtomicReferenceFieldUpdater<ExpungingValueImpl, Object> VALUE_UPDATER = AtomicReferenceFieldUpdater
      .newUpdater(ExpungingValueImpl.class, Object.class, "value");
    private static final Object EXPUNGED = new Object();
    private volatile Object value;

    private ExpungingValueImpl(final @NonNull V value) {
      this.value = value;
    }

    @Override
    public @Nullable V get() {
      final Object value = ExpungingValueImpl.VALUE_UPDATER.get(this);
      return value == ExpungingValueImpl.EXPUNGED ? null : (V) value;
    }

    @Override
    public @NonNull Entry<Boolean, V> putIfAbsent(final @NonNull V value) {
      for(; ; ) {
        final Object previous = ExpungingValueImpl.VALUE_UPDATER.get(this);
        if(previous == ExpungingValueImpl.EXPUNGED) {
          return new AbstractMap.SimpleImmutableEntry<>(Boolean.FALSE, null);
        }
        if(previous != null) {
          return new AbstractMap.SimpleImmutableEntry<>(Boolean.TRUE, (V) previous);
        }
        if(ExpungingValueImpl.VALUE_UPDATER.compareAndSet(this, null, value)) {
          return new AbstractMap.SimpleImmutableEntry<>(Boolean.TRUE, null);
        }
      }
    }

    @Override
    public boolean expunged() {
      return ExpungingValueImpl.VALUE_UPDATER.get(this) == ExpungingValueImpl.EXPUNGED;
    }

    @Override
    public boolean exists() {
      final Object value = ExpungingValueImpl.VALUE_UPDATER.get(this);
      return value != null && value != ExpungingValueImpl.EXPUNGED;
    }

    @Override
    public void set(final @NonNull V value) {
      ExpungingValueImpl.VALUE_UPDATER.set(this, value);
    }

    @Override
    public boolean replace(final @NonNull Object compare, final @Nullable V newValue) {
      for(; ; ) {
        final Object value = ExpungingValueImpl.VALUE_UPDATER.get(this);
        if(value == ExpungingValueImpl.EXPUNGED || !Objects.equals(value, compare)) return false;
        if(ExpungingValueImpl.VALUE_UPDATER.compareAndSet(this, value, newValue)) return true;
      }
    }

    @Override
    public @Nullable V clear() {
      for(; ; ) {
        final Object value = ExpungingValueImpl.VALUE_UPDATER.get(this);
        if(value == null || value == ExpungingValueImpl.EXPUNGED) return null;
        if(ExpungingValueImpl.VALUE_UPDATER.compareAndSet(this, value, null)) return (V) value;
      }
    }

    @Override
    public boolean trySet(final @NonNull V value) {
      for(; ; ) {
        final Object present = ExpungingValueImpl.VALUE_UPDATER.get(this);
        if(present == ExpungingValueImpl.EXPUNGED) return false;
        if(ExpungingValueImpl.VALUE_UPDATER.compareAndSet(this, present, value)) return true;
      }
    }

    @Override
    public boolean tryMarkExpunged() {
      Object value = ExpungingValueImpl.VALUE_UPDATER.get(this);
      while(value == null) {
        if(ExpungingValueImpl.VALUE_UPDATER.compareAndSet(this, null, ExpungingValueImpl.EXPUNGED)) return true;
        value = ExpungingValueImpl.VALUE_UPDATER.get(this);
      }
      return false;
    }

    @Override
    public boolean tryUnexpungeAndSet(final @Nullable V value) {
      return ExpungingValueImpl.VALUE_UPDATER.compareAndSet(this, ExpungingValueImpl.EXPUNGED, value);
    }

    @Override
    public boolean tryUnexpunge() {
      return ExpungingValueImpl.VALUE_UPDATER.compareAndSet(this, ExpungingValueImpl.EXPUNGED, null);
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

  private final class EntrySetView extends AbstractSet<Map.Entry<K, V>> {
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
    private Map.Entry<K, ExpungingValue<V>> next;
    private Map.Entry<K, ExpungingValue<V>> current;

    private EntryIterator(final @NonNull Iterator<Map.Entry<K, ExpungingValue<V>>> backingIterator) {
      this.backingIterator = backingIterator;
      this.next = this.backingIterator.hasNext() ? this.backingIterator.next() : null;
    }

    @Override
    public boolean hasNext() {
      return this.next != null;
    }

    @Override
    public Map.@NonNull Entry<K, V> next() {
      if((this.current = this.next) == null) throw new NoSuchElementException();
      this.next = this.backingIterator.hasNext() ? this.backingIterator.next() : null;
      return new MapEntry(this.current);
    }

    @Override
    public void remove() {
      if(this.current == null) return;
      SyncMapImpl.this.remove(this.current.getKey());
    }

    @Override
    public void forEachRemaining(final @NonNull Consumer<? super Map.Entry<K, V>> action) {
      if(this.next != null) action.accept(new MapEntry(this.next));
      this.backingIterator.forEachRemaining(entry -> action.accept(new MapEntry(entry)));
    }
  }
}

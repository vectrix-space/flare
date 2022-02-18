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
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;

import static java.util.Objects.requireNonNull;

/* package */ final class ForwardingSyncMapImpl<K, V> extends AbstractMap<K, V> implements ForwardingSyncMap<K, V> {
  /**
   * A single implicit lock when dealing with {@code dirty} mutations.
   */
  private transient final Object lock = new Object();

  /**
   * The read only map that does not require a lock and does not allow mutations.
   */
  private transient volatile Map<K, ExpungingEntry<V>> read;

  /**
   * Represents whether the {@code dirty} map has changes the {@code read} map
   * does not have yet.
   */
  private transient volatile boolean amended;

  /**
   * The read/write map that requires a lock and allows mutations.
   */
  private transient Map<K, ExpungingEntry<V>> dirty;

  /**
   * Represents the amount of times an attempt has been made to access the
   * {@code dirty} map while {@code amended} is {@code true}.
   */
  private transient int misses;

  private transient final IntFunction<Map<K, ExpungingEntry<V>>> function;

  private transient EntrySetView entrySet;

  /* package */ ForwardingSyncMapImpl(final @NonNull IntFunction<Map<K, ExpungingEntry<V>>> function, final int initialCapacity) {
    if(initialCapacity < 0) throw new IllegalArgumentException("Initial capacity must be greater than 0");
    this.function = function;
    this.read = function.apply(initialCapacity);
  }

  // Query Operations

  @Override
  public int size() {
    this.promote();
    int size = 0;
    for(final ExpungingEntry<V> value : this.read.values()) {
      if(value.exists()) size++;
    }
    return size;
  }

  @Override
  public boolean isEmpty() {
    this.promote();
    for(final ExpungingEntry<V> value : this.read.values()) {
      if(value.exists()) return false;
    }
    return true;
  }

  @Override
  public boolean containsKey(final @Nullable Object key) {
    final ExpungingEntry<V> entry = this.getEntry(key);
    return entry != null && entry.exists();
  }

  @Override
  public @Nullable V get(final @Nullable Object key) {
    final ExpungingEntry<V> entry = this.getEntry(key);
    return entry != null ? entry.get() : null;
  }

  @Override
  public @NonNull V getOrDefault(final @Nullable Object key, final @NonNull V defaultValue) {
    requireNonNull(defaultValue, "defaultValue");
    final ExpungingEntry<V> entry = this.getEntry(key);
    return entry != null ? entry.getOr(defaultValue) : defaultValue;
  }

  @SuppressWarnings("SuspiciousMethodCalls")
  private @Nullable ExpungingEntry<V> getEntry(final @Nullable Object key) {
    ExpungingEntry<V> entry = this.read.get(key);
    if(entry == null && this.amended) {
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
    return entry;
  }

  @Override
  public @Nullable V computeIfAbsent(final @Nullable K key, final @NonNull Function<? super K, ? extends V> mappingFunction) {
    requireNonNull(mappingFunction, "mappingFunction");
    ExpungingEntry<V> entry = this.read.get(key);
    InsertionResult<V> result = entry != null ? entry.computeIfAbsent(key, mappingFunction) : null;
    if(result != null && result.operation() == InsertionResultImpl.UPDATED) return result.current();
    synchronized(this.lock) {
      if((entry = this.read.get(key)) != null) {
        // If the entry was expunged, unexpunge, add the entry
        // back to the dirty map.
        if(entry.tryUnexpungeAndCompute(key, mappingFunction)) {
          if(entry.exists()) this.dirty.put(key, entry);
          return entry.get();
        } else {
          result = entry.computeIfAbsent(key, mappingFunction);
        }
      } else if(this.dirty != null && (entry = this.dirty.get(key)) != null) {
        result = entry.computeIfAbsent(key, mappingFunction);
        if(result.current() == null) this.dirty.remove(key);
        // The slow path should be avoided, even if the value does
        // not match or is present. So we mark a miss, to eventually
        // promote and take a faster path.
        this.missLocked();
      } else {
        if(!this.amended) {
          // Adds the first new key to the dirty map and marks it as
          // amended.
          this.dirtyLocked();
          this.amended = true;
        }
        final V computed = mappingFunction.apply(key);
        if(computed != null) this.dirty.put(key, new ExpungingEntryImpl<>(computed));
        return computed;
      }
    }
    return result.current();
  }

  @Override
  public @Nullable V computeIfPresent(final @Nullable K key, final @NonNull BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
    requireNonNull(remappingFunction, "remappingFunction");
    ExpungingEntry<V> entry = this.read.get(key);
    InsertionResult<V> result = entry != null ? entry.computeIfPresent(key, remappingFunction) : null;
    if(result != null && result.operation() == InsertionResultImpl.UPDATED) return result.current();
    synchronized(this.lock) {
      if((entry = this.read.get(key)) != null) {
        result = entry.computeIfPresent(key, remappingFunction);
      } else if(this.dirty != null && (entry = this.dirty.get(key)) != null) {
        result = entry.computeIfPresent(key, remappingFunction);
        if(result.current() == null) this.dirty.remove(key);
        // The slow path should be avoided, even if the value does
        // not match or is present. So we mark a miss, to eventually
        // promote and take a faster path.
        this.missLocked();
      }
    }
    return result != null ? result.current() : null;
  }

  @Override
  public @Nullable V compute(final @Nullable K key, final @Nullable BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
    requireNonNull(remappingFunction, "remappingFunction");
    ExpungingEntry<V> entry = this.read.get(key);
    InsertionResult<V> result = entry != null ? entry.compute(key, remappingFunction) : null;
    if(result != null && result.operation() == InsertionResultImpl.UPDATED) return result.current();
    synchronized(this.lock) {
      if((entry = this.read.get(key)) != null) {
        // If the entry was expunged, unexpunge, add the entry
        // back to the dirty map if the value is not null.
        if(entry.tryUnexpungeAndCompute(key, remappingFunction)) {
          if(entry.exists()) this.dirty.put(key, entry);
          return entry.get();
        } else {
          result = entry.compute(key, remappingFunction);
        }
      } else if(this.dirty != null && (entry = this.dirty.get(key)) != null) {
        result = entry.compute(key, remappingFunction);
        if(result.current() == null) this.dirty.remove(key);
        // The slow path should be avoided, even if the value does
        // not match or is present. So we mark a miss, to eventually
        // promote and take a faster path.
        this.missLocked();
      } else {
        if(!this.amended) {
          // Adds the first new key to the dirty map and marks it as
          // amended.
          this.dirtyLocked();
          this.amended = true;
        }
        final V computed = remappingFunction.apply(key, null);
        if(computed != null) this.dirty.put(key, new ExpungingEntryImpl<>(computed));
        return computed;
      }
    }
    return result.current();
  }

  @Override
  @SuppressWarnings("ConstantConditions")
  public @Nullable V putIfAbsent(final @Nullable K key, final @NonNull V value) {
    requireNonNull(value, "value");
    ExpungingEntry<V> entry = this.read.get(key);
    InsertionResult<V> result = entry != null ? entry.setIfAbsent(value) : null;
    if(result != null && result.operation() == InsertionResultImpl.UPDATED) return result.previous();
    synchronized(this.lock) {
      if((entry = this.read.get(key)) != null) {
        // If the entry was expunged, unexpunge, add the entry
        // back to the dirty map and return null, as we know there
        // was no previous value.
        if(entry.tryUnexpungeAndSet(value)) {
          this.dirty.put(key, entry);
          return null;
        } else {
          result = entry.setIfAbsent(value);
        }
      } else if(this.dirty != null && (entry = this.dirty.get(key)) != null) {
        result = entry.setIfAbsent(value);
        // The slow path should be avoided, even if the value does
        // not match or is present. So we mark a miss, to eventually
        // promote and take a faster path.
        this.missLocked();
      } else {
        if(!this.amended) {
          // Adds the first new key to the dirty map and marks it as
          // amended.
          this.dirtyLocked();
          this.amended = true;
        }
        this.dirty.put(key, new ExpungingEntryImpl<>(value));
        return null;
      }
    }
    return result.previous();
  }

  @Override
  @SuppressWarnings("ConstantConditions")
  public @Nullable V put(final @Nullable K key, final @NonNull V value) {
    requireNonNull(value, "value");
    ExpungingEntry<V> entry = this.read.get(key);
    V previous = entry != null ? entry.get() : null;
    if(entry != null && entry.trySet(value)) return previous;
    synchronized(this.lock) {
      if((entry = this.read.get(key)) != null) {
        previous = entry.get();
        // If the entry was expunged, unexpunge and add the entry
        // back to the dirty map.
        if(entry.tryUnexpungeAndSet(value)) {
          this.dirty.put(key, entry);
        } else {
          entry.set(value);
        }
      } else if(this.dirty != null && (entry = this.dirty.get(key)) != null) {
        previous = entry.get();
        entry.set(value);
      } else {
        if(!this.amended) {
          // Adds the first new key to the dirty map and marks it as
          // amended.
          this.dirtyLocked();
          this.amended = true;
        }
        this.dirty.put(key, new ExpungingEntryImpl<>(value));
        return null;
      }
    }
    return previous;
  }

  @Override
  @SuppressWarnings("SuspiciousMethodCalls")
  public @Nullable V remove(final @Nullable Object key) {
    ExpungingEntry<V> entry = this.read.get(key);
    if(entry == null && this.amended) {
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
    ExpungingEntry<V> entry = this.read.get(key);
    if(entry == null && this.amended) {
      synchronized(this.lock) {
        if((entry = this.read.get(key)) == null && this.amended && this.dirty != null) {
          final boolean present = ((entry = this.dirty.get(key)) != null && entry.replace(value, null));
          if(present) this.dirty.remove(key);
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
  public @Nullable V replace(final @Nullable K key, final @NonNull V value) {
    requireNonNull(value, "value");
    final ExpungingEntry<V> entry = this.getEntry(key);
    return entry != null ? entry.tryReplace(value) : null;
  }

  @Override
  public boolean replace(final @Nullable K key, final @NonNull V oldValue, final @NonNull V newValue) {
    requireNonNull(oldValue, "oldValue");
    requireNonNull(newValue, "newValue");
    final ExpungingEntry<V> entry = this.getEntry(key);
    return entry != null && entry.replace(oldValue, newValue);
  }

  // Bulk Operations

  @Override
  public void forEach(final @NonNull BiConsumer<? super K, ? super V> action) {
    requireNonNull(action, "action");
    this.promote();
    V value;
    for(final Map.Entry<K, ExpungingEntry<V>> that : this.read.entrySet()) {
      if((value = that.getValue().get()) != null) {
        action.accept(that.getKey(), value);
      }
    }
  }

  @Override
  public void replaceAll(final @NonNull BiFunction<? super K, ? super V, ? extends V> function) {
    requireNonNull(function, "function");
    this.promote();
    ExpungingEntry<V> entry; V value;
    for(final Map.Entry<K, ExpungingEntry<V>> that : this.read.entrySet()) {
      if((value = (entry = that.getValue()).get()) != null) {
        entry.tryReplace(function.apply(that.getKey(), value));
      }
    }
  }

  @Override
  public void clear() {
    synchronized(this.lock) {
      this.read = this.function.apply(this.read.size());
      this.dirty = null;
      this.amended = false;
      this.misses = 0;
    }
  }

  // Views

  @Override
  public @NonNull Set<Entry<K, V>> entrySet() {
    if(this.entrySet != null) return this.entrySet;
    return this.entrySet = new EntrySetView();
  }

  private void promote() {
    if(this.amended) {
      synchronized(this.lock) {
        if(this.amended) {
          this.promoteLocked();
        }
      }
    }
  }

  private void missLocked() {
    this.misses++;
    if(this.misses < this.dirty.size()) return;
    this.promoteLocked();
  }

  private void promoteLocked() {
    this.read = this.dirty;
    this.amended = false;
    this.dirty = null;
    this.misses = 0;
  }

  private void dirtyLocked() {
    if(this.dirty != null) return;
    this.dirty = this.function.apply(this.read.size());
    for(final Map.Entry<K, ExpungingEntry<V>> entry : this.read.entrySet()) {
      if(!entry.getValue().tryExpunge()) {
        this.dirty.put(entry.getKey(), entry.getValue());
      }
    }
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  /* package */ static final class ExpungingEntryImpl<V> implements ExpungingEntry<V> {
    private static final AtomicReferenceFieldUpdater<ExpungingEntryImpl, Object> UPDATER = AtomicReferenceFieldUpdater
      .newUpdater(ExpungingEntryImpl.class, Object.class, "value");
    private static final Object EXPUNGED = new Object();
    private volatile Object value;

    /* package */ ExpungingEntryImpl(final @NonNull V value) {
      this.value = value;
    }

    @Override
    public boolean exists() {
      return this.value != null && this.value != ExpungingEntryImpl.EXPUNGED;
    }

    @Override
    public @Nullable V get() {
      return this.value == ExpungingEntryImpl.EXPUNGED ? null : (V) this.value;
    }

    @Override
    public @NonNull V getOr(final @NonNull V other) {
      final Object value = this.value;
      return value != null && value != ExpungingEntryImpl.EXPUNGED ? (V) this.value : other;
    }

    @Override
    public @NonNull InsertionResult<V> setIfAbsent(final @NonNull V value) {
      for(; ; ) {
        final Object previous = this.value;
        if(previous == ExpungingEntryImpl.EXPUNGED) return new InsertionResultImpl<>(InsertionResultImpl.EXPUNGED, null, null);
        if(previous != null) return new InsertionResultImpl<>(InsertionResultImpl.UNCHANGED, (V) previous, (V) previous);
        if(ExpungingEntryImpl.UPDATER.compareAndSet(this, null, value)) {
          return new InsertionResultImpl<>(InsertionResultImpl.UPDATED, null, value);
        }
      }
    }

    @Override
    public <K> @NonNull InsertionResult<V> computeIfAbsent(final @Nullable K key, final @NonNull Function<? super K, ? extends V> function) {
      V next = null;
      for(; ; ) {
        final Object previous = this.value;
        if(previous == ExpungingEntryImpl.EXPUNGED) return new InsertionResultImpl<>(InsertionResultImpl.EXPUNGED, null, null);
        if(previous != null) return new InsertionResultImpl<>(InsertionResultImpl.UNCHANGED, (V) previous, (V) previous);
        if(ExpungingEntryImpl.UPDATER.compareAndSet(this, null, next != null ? next : (next = function.apply(key)))) {
          return new InsertionResultImpl<>(InsertionResultImpl.UPDATED, null, next);
        }
      }
    }

    @Override
    public <K> @NonNull InsertionResult<V> computeIfPresent(final @Nullable K key, final @NonNull BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
      V next = null;
      for(; ; ) {
        final Object previous = this.value;
        if(previous == ExpungingEntryImpl.EXPUNGED) return new InsertionResultImpl<>(InsertionResultImpl.EXPUNGED, null, null);
        if(previous == null) return new InsertionResultImpl<>(InsertionResultImpl.UNCHANGED, null, null);
        if(ExpungingEntryImpl.UPDATER.compareAndSet(this, previous, next != null ? next : (next = remappingFunction.apply(key, (V) previous)))) {
          return new InsertionResultImpl<>(InsertionResultImpl.UPDATED, (V) previous, next);
        }
      }
    }

    @Override
    public <K> @NonNull InsertionResult<V> compute(final @Nullable K key, final @NonNull BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
      V next = null;
      for(; ; ) {
        final Object previous = this.value;
        if(previous == ExpungingEntryImpl.EXPUNGED) return new InsertionResultImpl<>(InsertionResultImpl.EXPUNGED, null, null);
        if(ExpungingEntryImpl.UPDATER.compareAndSet(this, previous, next != null ? next : (next = remappingFunction.apply(key, (V) previous)))) {
          return new InsertionResultImpl<>(InsertionResultImpl.UPDATED, (V) previous, next);
        }
      }
    }

    @Override
    public void set(final @NonNull V value) {
      ExpungingEntryImpl.UPDATER.set(this, value);
    }

    @Override
    public boolean replace(final @NonNull Object compare, final @Nullable V value) {
      for(; ; ) {
        final Object previous = this.value;
        if(previous == ExpungingEntryImpl.EXPUNGED || !Objects.equals(previous, compare)) return false;
        if(ExpungingEntryImpl.UPDATER.compareAndSet(this, previous, value)) return true;
      }
    }

    @Override
    public @Nullable V clear() {
      for(; ; ) {
        final Object previous = this.value;
        if(previous == null || previous == ExpungingEntryImpl.EXPUNGED) return null;
        if(ExpungingEntryImpl.UPDATER.compareAndSet(this, previous, null)) return (V) previous;
      }
    }

    @Override
    public boolean trySet(final @NonNull V value) {
      for(; ; ) {
        final Object previous = this.value;
        if(previous == ExpungingEntryImpl.EXPUNGED) return false;
        if(ExpungingEntryImpl.UPDATER.compareAndSet(this, previous, value)) return true;
      }
    }

    @Override
    public @Nullable V tryReplace(final @NonNull V value) {
      for(; ; ) {
        final Object previous = this.value;
        if(previous == null || previous == ExpungingEntryImpl.EXPUNGED) return null;
        if(ExpungingEntryImpl.UPDATER.compareAndSet(this, previous, value)) return (V) previous;
      }
    }

    @Override
    public boolean tryExpunge() {
      while(this.value == null) {
        if(ExpungingEntryImpl.UPDATER.compareAndSet(this, null, ExpungingEntryImpl.EXPUNGED)) return true;
      }
      return this.value == ExpungingEntryImpl.EXPUNGED;
    }

    @Override
    public boolean tryUnexpungeAndSet(final @NonNull V value) {
      return ExpungingEntryImpl.UPDATER.compareAndSet(this, ExpungingEntryImpl.EXPUNGED, value);
    }

    @Override
    public <K> boolean tryUnexpungeAndCompute(final @Nullable K key, final @NonNull Function<? super K, ? extends V> function) {
      if(this.value == ExpungingEntryImpl.EXPUNGED) {
        final Object value = function.apply(key);
        return ExpungingEntryImpl.UPDATER.compareAndSet(this, ExpungingEntryImpl.EXPUNGED, value);
      }
      return false;
    }

    @Override
    public <K> boolean tryUnexpungeAndCompute(final @Nullable K key, final @NonNull BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
      if(this.value == ExpungingEntryImpl.EXPUNGED) {
        final Object value = remappingFunction.apply(key, null);
        return ExpungingEntryImpl.UPDATER.compareAndSet(this, ExpungingEntryImpl.EXPUNGED, value);
      }
      return false;
    }
  }

  /* package */ static final class InsertionResultImpl<V> implements InsertionResult<V> {
    private static final byte UNCHANGED = 0x00;
    private static final byte UPDATED = 0x01;
    private static final byte EXPUNGED = 0x02;

    private final byte operation;
    private final V previous;
    private final V current;

    /* package */ InsertionResultImpl(final byte operation, final @Nullable V previous, final @Nullable V current) {
      this.operation = operation;
      this.previous = previous;
      this.current = current;
    }

    @Override
    public byte operation() {
      return this.operation;
    }

    @Override
    public @Nullable V previous() {
      return this.previous;
    }

    @Override
    public @Nullable V current() {
      return this.current;
    }
  }

  /* package */ final class MapEntry implements Map.Entry<K, V> {
    private final K key;
    private V value;

    /* package */ MapEntry(final @Nullable K key, final @NonNull V value) {
      this.key = key;
      this.value = value;
    }

    @Override
    public @Nullable K getKey() {
      return this.key;
    }

    @Override
    public @NonNull V getValue() {
      return this.value;
    }

    @Override
    public @Nullable V setValue(final @NonNull V value) {
      requireNonNull(value, "value");
      final V previous = ForwardingSyncMapImpl.this.put(this.key, value);
      this.value = value;
      return previous;
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

  /* package */ final class EntrySetView extends AbstractSet<Map.Entry<K, V>> {
    @Override
    public int size() {
      return ForwardingSyncMapImpl.this.size();
    }

    @Override
    public boolean contains(final @Nullable Object entry) {
      if(!(entry instanceof Map.Entry)) return false;
      final Map.Entry<?, ?> mapEntry = (Entry<?, ?>) entry;
      final V value = ForwardingSyncMapImpl.this.get(mapEntry.getKey());
      return value != null && Objects.equals(value, mapEntry.getValue());
    }

    @Override
    public boolean add(final @NonNull Entry<K, V> entry) {
      requireNonNull(entry, "entry");
      return ForwardingSyncMapImpl.this.put(entry.getKey(), entry.getValue()) == null;
    }

    @Override
    public boolean remove(final @Nullable Object entry) {
      if(!(entry instanceof Map.Entry)) return false;
      final Map.Entry<?, ?> mapEntry = (Entry<?, ?>) entry;
      return ForwardingSyncMapImpl.this.remove(mapEntry.getKey(), mapEntry.getValue());
    }

    @Override
    public void clear() {
      ForwardingSyncMapImpl.this.clear();
    }

    @Override
    public @NonNull Iterator<Map.Entry<K, V>> iterator() {
      ForwardingSyncMapImpl.this.promote();
      return new EntryIterator(ForwardingSyncMapImpl.this.read.entrySet().iterator());
    }
  }

  /* package */ final class EntryIterator implements Iterator<Map.Entry<K, V>> {
    private final Iterator<Map.Entry<K, ExpungingEntry<V>>> backingIterator;
    private Map.Entry<K, V> next;
    private Map.Entry<K, V> current;

    /* package */ EntryIterator(final @NonNull Iterator<Map.Entry<K, ExpungingEntry<V>>> backingIterator) {
      this.backingIterator = backingIterator;
      this.advance();
    }

    @Override
    public boolean hasNext() {
      return this.next != null;
    }

    @Override
    public Map.@NonNull Entry<K, V> next() {
      final Map.Entry<K, V> current;
      if((current = this.next) == null) throw new NoSuchElementException();
      this.current = current;
      this.advance();
      return current;
    }

    @Override
    public void remove() {
      final Map.Entry<K, V> current;
      if((current = this.current) == null) throw new IllegalStateException();
      this.current = null;
      ForwardingSyncMapImpl.this.remove(current.getKey());
    }

    private void advance() {
      this.next = null;
      while(this.backingIterator.hasNext()) {
        final Map.Entry<K, ExpungingEntry<V>> entry; final V value;
        if((value = (entry = this.backingIterator.next()).getValue().get()) != null) {
          this.next = new MapEntry(entry.getKey(), value);
          return;
        }
      }
    }
  }
}

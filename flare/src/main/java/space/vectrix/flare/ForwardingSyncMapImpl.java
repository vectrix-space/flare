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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;

import static java.util.Objects.requireNonNull;

/* package */ final class ForwardingSyncMapImpl<K, V> extends AbstractMap<K, V> implements ForwardingSyncMap<K, V> {
  private transient final Object lock = new Object();
  private transient final IntFunction<Map<K, ExpungingValue<V>>> function;
  private transient volatile Map<K, ExpungingValue<V>> immutable;
  private transient volatile boolean amended;
  private transient Map<K, ExpungingValue<V>> mutable;
  private transient int misses;
  private transient EntrySet entrySet;

  /* package */ ForwardingSyncMapImpl(final @NotNull IntFunction<Map<K, ExpungingValue<V>>> function, final int initialCapacity) {
    if(initialCapacity < 0) throw new IllegalArgumentException("Initial capacity must be greater than 0");
    this.function = function;
    this.immutable = function.apply(initialCapacity);
  }

  // Query Operations

  @Override
  public int size() {
    final Map<K, ExpungingValue<V>> immutable = this.promote();
    int size = 0;
    for(final ExpungingValue<V> value : immutable.values()) {
      if(!value.empty()) size++;
    }
    return size;
  }

  @Override
  public boolean isEmpty() {
    final Map<K, ExpungingValue<V>> immutable = this.promote();
    for(final ExpungingValue<V> value : immutable.values()) {
      if(!value.empty()) return false;
    }
    return true;
  }

  @Override
  public boolean containsKey(final @Nullable Object key) {
    final ExpungingValue<V> value = this.getValue(key);
    return value != null && !value.empty();
  }

  @Override
  public @Nullable V get(final @Nullable Object key) {
    final ExpungingValue<V> value = this.getValue(key);
    return value != null ? value.get() : null;
  }

  @Override
  public @NotNull V getOrDefault(final @Nullable Object key, final @NotNull V defaultValue) {
    requireNonNull(defaultValue, "defaultValue");
    final ExpungingValue<V> value = this.getValue(key);
    return value != null ? value.getOrDefault(defaultValue) : defaultValue;
  }

  @SuppressWarnings("SuspiciousMethodCalls")
  private @Nullable ExpungingValue<V> getValue(final @Nullable Object key) {
    ExpungingValue<V> entry = this.immutable.get(key);
    if(entry != null) return entry;
    if(this.amended) {
      // Check if the map is amended to access an entry from the mutable map.
      synchronized(this.lock) {
        if((entry = this.immutable.get(key)) == null && this.amended) {
          entry = this.mutable.get(key);
          // Count a miss to avoid taking this slow path in the future.
          this.lockedMiss();
        }
      }
    }
    return entry;
  }

  @Override
  public @Nullable V computeIfAbsent(final @Nullable K key, final @NotNull Function<? super K, ? extends V> mappingFunction) {
    requireNonNull(mappingFunction, "mappingFunction");
    // Compute the mapping function ahead of time, so we
    // can exit early if the computed value is null or
    // throws an exception.
    final V newValue = mappingFunction.apply(key);
    if(newValue == null) return null;
    ExpungingValue<V> entry = this.immutable.get(key);
    Map.Entry<V, Operation> result = entry != null ? entry.updateAbsent(null, newValue) : null;
    if(result != null && result.getValue() != Operation.EXPUNGED) return result.getKey();
    synchronized(this.lock) {
      if((entry = this.immutable.get(key)) != null) {
        // Unexpunge the entry. If modified add back
        // to the mutable map.
        if(entry.unexpunge(newValue)) {
          this.mutable.put(key, entry);
          return newValue;
        }
        // Otherwise force set the value onto the entry.
        result = entry.forceUpdateAbsent(null, newValue);
      } else if(this.amended && (entry = this.mutable.get(key)) != null) {
        result = entry.forceUpdateAbsent(null, newValue);
        // The slow path should be avoided, even if the value does
        // not match or is present. So we mark a miss, to eventually
        // promote and take a faster path.
        this.lockedMiss();
      } else {
        if(!this.amended) {
          // Adds the first new key to the mutable map and marks it as
          // amended.
          this.lockedDirty();
        }
        this.mutable.put(key, new ExpungingValueImpl<>(newValue));
        return newValue;
      }
    }
    return result.getValue() != Operation.EXPUNGED ? result.getKey() : null;
  }

  @Override
  public @Nullable V computeIfPresent(final @Nullable K key, final @NotNull BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
    requireNonNull(remappingFunction, "remappingFunction");
    ExpungingValue<V> entry = this.immutable.get(key);
    final Map.Entry<V, Operation> result = entry != null ? entry.computePresent(key, remappingFunction) : null;
    if(result != null && result.getValue() != Operation.EXPUNGED) return result.getKey();
    V previous, next = null;
    synchronized(this.lock) {
      if((entry = this.immutable.get(key)) != null) {
        // If the entry value does not exist, return null,
        // otherwise force set the value onto the entry.
        if((previous = entry.get()) == null) return null;
        entry.forceUpdate(previous, next = remappingFunction.apply(key, previous));
      } else if(this.amended && (entry = this.mutable.get(key)) != null) {
        if((previous = entry.get()) == null) return null;
        entry.forceUpdate(previous, next = remappingFunction.apply(key, previous));
        // The slow path should be avoided, even if the value does
        // not match or is present. So we mark a miss, to eventually
        // promote and take a faster path.
        this.lockedMiss();
      }
    }
    return next;
  }

  @Override
  public @Nullable V compute(final @Nullable K key, final @NotNull BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
    requireNonNull(remappingFunction, "remappingFunction");
    ExpungingValue<V> entry = this.immutable.get(key);
    final Map.Entry<V, Operation> result = entry != null ? entry.compute(key, remappingFunction) : null;
    if(result != null && result.getValue() != Operation.EXPUNGED) return result.getKey();
    final V next;
    synchronized(this.lock) {
      if((entry = this.immutable.get(key)) != null) {
        next = remappingFunction.apply(key, entry.get());
        // If the entry was expunged, unexpunge, add the entry
        // back to the mutable map, if the value exists.
        if(entry.unexpunge(next)) {
          if(next != null) this.mutable.put(key, entry);
          return next;
        }
        // Otherwise force set the value onto the entry.
        entry.forceSet(next);
      } else if(this.amended && (entry = this.mutable.get(key)) != null) {
        next = remappingFunction.apply(key, entry.get());
        // If the value does not exist, remove it from the
        // mutable map and force set the next value.
        if(next == null) this.mutable.remove(key);
        entry.forceSet(next);
        // The slow path should be avoided, even if the value does
        // not match or is present. So we mark a miss, to eventually
        // promote and take a faster path.
        this.lockedMiss();
      } else {
        if(!this.amended) {
          // Adds the first new key to the mutable map and marks it as
          // amended.
          this.lockedDirty();
        }
        next = remappingFunction.apply(key, null);
        if(next != null) this.mutable.put(key, new ExpungingValueImpl<>(next));
      }
    }
    return next;
  }

  @Override
  public @Nullable V putIfAbsent(final @Nullable K key, final @NotNull V value) {
    requireNonNull(value, "value");
    ExpungingValue<V> entry = this.immutable.get(key);
    Map.Entry<V, Operation> result = entry != null ? entry.update(null, value) : null;
    if(result != null && result.getValue() != Operation.EXPUNGED) return result.getKey();
    synchronized(this.lock) {
      if((entry = this.immutable.get(key)) != null) {
        // Unexpunge the entry. If modified add back
        // to the mutable map.
        if(entry.unexpunge(value)) {
          this.mutable.put(key, entry);
          return null;
        }
        // Otherwise force set the value onto the entry.
        result = entry.forceUpdate(null, value);
      } else if(this.amended && (entry = this.mutable.get(key)) != null) {
        result = entry.forceUpdate(null, value);
        // The slow path should be avoided, even if the value does
        // not match or is present. So we mark a miss, to eventually
        // promote and take a faster path.
        this.lockedMiss();
      } else {
        if(!this.amended) {
          // Adds the first new key to the mutable map and marks it as
          // amended.
          this.lockedDirty();
        }
        this.mutable.put(key, new ExpungingValueImpl<>(value));
        return null;
      }
    }
    return result.getKey();
  }

  @Override
  public @Nullable V put(final @Nullable K key, final @NotNull V value) {
    requireNonNull(value, "value");
    ExpungingValue<V> entry = this.immutable.get(key);
    Map.Entry<V, Operation> result = entry != null ? entry.set(value) : null;
    if(result != null && result.getValue() != Operation.EXPUNGED) return result.getKey();
    final V previousValue;
    synchronized(this.lock) {
      if((entry = this.immutable.get(key)) != null) {
        // Unexpunge the entry. If modified add back
        // to the mutable map.
        if(entry.unexpunge(value)) {
          this.mutable.put(key, entry);
          return null;
        }
        // Otherwise force set the value onto the entry.
        previousValue = entry.get();
        entry.forceSet(value);
      } else if(this.amended && (entry = this.mutable.get(key)) != null) {
        previousValue = entry.get();
        entry.forceSet(value);
        // The slow path should be avoided, even if the value does
        // not match or is present. So we mark a miss, to eventually
        // promote and take a faster path.
        this.lockedMiss();
      } else {
        if(!this.amended) {
          // Adds the first new key to the mutable map and marks it as
          // amended.
          this.lockedDirty();
        }
        this.mutable.put(key, new ExpungingValueImpl<>(value));
        return null;
      }
    }
    return previousValue;
  }

  @Override
  @SuppressWarnings("SuspiciousMethodCalls")
  public @Nullable V remove(final @Nullable Object key) {
    ExpungingValue<V> entry = this.immutable.get(key);
    if(entry == null && this.amended) {
      synchronized(this.lock) {
        if((entry = this.immutable.get(key)) == null && this.amended) {
          entry = this.mutable.remove(key);
          // The slow path should be avoided, even if the value does
          // not match or is present. So we mark a miss, to eventually
          // promote and take a faster path.
          this.lockedMiss();
        }
      }
    }
    return entry != null ? entry.clear() : null;
  }

  @Override
  @SuppressWarnings("SuspiciousMethodCalls")
  public boolean remove(final @Nullable Object key, final @NotNull Object value) {
    requireNonNull(value, "value");
    ExpungingValue<V> entry = this.immutable.get(key);
    if(entry == null && this.amended) {
      synchronized(this.lock) {
        if((entry = this.immutable.get(key)) == null && this.amended) {
          final boolean removed = ((entry = this.mutable.get(key)) != null && entry.update(value, null).getValue() == Operation.MODIFIED);
          if(removed) this.mutable.remove(key);
          // The slow path should be avoided, even if the value does
          // not match or is present. So we mark a miss, to eventually
          // promote and take a faster path.
          this.lockedMiss();
          return removed;
        }
      }
    }
    return entry != null && entry.update(value, null).getValue() == Operation.MODIFIED;
  }

  @Override
  public @Nullable V replace(final @Nullable K key, final @NotNull V value) {
    requireNonNull(value, "value");
    ExpungingValue<V> entry = this.immutable.get(key);
    Map.Entry<V, Operation> result = entry != null ? entry.set(value) : null;
    if(result != null && result.getValue() != Operation.EXPUNGED) return result.getKey();
    V previousValue = null;
    if(this.amended) {
      synchronized(this.lock) {
        if((entry = this.immutable.get(key)) != null) {
          previousValue = entry.get();
          entry.forceSet(value);
        } else if(this.amended && (entry = this.mutable.get(key)) != null) {
          previousValue = entry.get();
          entry.forceSet(value);
          // The slow path should be avoided, even if the value does
          // not match or is present. So we mark a miss, to eventually
          // promote and take a faster path.
          this.lockedMiss();
        }
      }
    }
    return previousValue;
  }

  @Override
  public boolean replace(final @Nullable K key, final @NotNull V oldValue, final @NotNull V newValue) {
    requireNonNull(oldValue, "oldValue");
    requireNonNull(newValue, "newValue");
    ExpungingValue<V> entry = this.immutable.get(key);
    Map.Entry<V, Operation> result = entry != null ? entry.update(oldValue, newValue) : null;
    if(result != null && result.getValue() == Operation.MODIFIED) return true;
    if(this.amended) {
      synchronized(this.lock) {
        if((entry = this.immutable.get(key)) != null) {
          result = entry.forceUpdate(oldValue, newValue);
        } else if(this.amended && (entry = this.mutable.get(key)) != null) {
          result = entry.forceUpdate(oldValue, newValue);
          // The slow path should be avoided, even if the value does
          // not match or is present. So we mark a miss, to eventually
          // promote and take a faster path.
          this.lockedMiss();
        }
      }
    }
    return result != null && result.getValue() == Operation.MODIFIED;
  }

  // Bulk Operations

  @Override
  public void forEach(final @NotNull BiConsumer<? super K, ? super V> action) {
    requireNonNull(action, "action");
    final Map<K, ExpungingValue<V>> immutable = this.promote();
    V value;
    for(final Map.Entry<K, ExpungingValue<V>> that : immutable.entrySet()) {
      if((value = that.getValue().get()) != null) {
        action.accept(that.getKey(), value);
      }
    }
  }

  @Override
  public void replaceAll(final @NotNull BiFunction<? super K, ? super V, ? extends V> function) {
    requireNonNull(function, "function");
    final Map<K, ExpungingValue<V>> immutable = this.promote();
    for(final Map.Entry<K, ExpungingValue<V>> that : immutable.entrySet()) {
      final K key = that.getKey();
      final V previous;
      ExpungingValue<V> entry = that.getValue();
      Map.Entry<V, Operation> result = entry != null ? entry.compute(key, function) : null;
      if(result == null && this.amended) {
        synchronized(this.lock) {
          if((entry = this.immutable.get(key)) != null) {
            if((previous = entry.get()) == null) continue;
            entry.forceUpdate(previous, function.apply(key, previous));
          } else if(this.amended && (entry = this.mutable.get(key)) != null) {
            if((previous = entry.get()) == null) continue;
            entry.forceUpdate(previous, function.apply(key, previous));
            // The slow path should be avoided, even if the value does
            // not match or is present. So we mark a miss, to eventually
            // promote and take a faster path.
            this.lockedMiss();
          }
        }
      }
    }
  }

  @Override
  public void clear() {
    synchronized(this.lock) {
      this.amended = false;
      this.immutable = this.function.apply(this.immutable.size());
      this.mutable = null;
      this.misses = 0;
    }
  }

  // Views

  @Override
  public @NotNull Set<Entry<K, V>> entrySet() {
    if(this.entrySet != null) return this.entrySet;
    return this.entrySet = new EntrySet();
  }

  private Map<K, ExpungingValue<V>> promote() {
    Map<K, ExpungingValue<V>> map = this.immutable;
    if(this.amended) {
      synchronized(this.lock) {
        if(this.amended) {
          this.lockedPromote();
          map = this.immutable;
        }
      }
    }
    return map;
  }

  private void lockedMiss() {
    if(++this.misses < this.mutable.size()) return;
    this.lockedPromote();
  }

  private void lockedPromote() {
    this.amended = false;
    this.immutable = this.mutable;
    this.mutable = null;
    this.misses = 0;
  }

  private void lockedDirty() {
    this.mutable = this.function.apply(this.immutable.size() + 1);
    this.immutable.forEach((key, value) -> {
      if(!value.expunge()) this.mutable.put(key, value);
    });
    this.amended = true;
  }

  /* package */ final class EntryImpl implements Map.Entry<K, V> {
    private final K key;

    /* package */ EntryImpl(final @Nullable K key) {
      this.key = key;
    }


    @Override
    public @Nullable K getKey() {
      return this.key;
    }

    @Override
    public @Nullable V getValue() {
      return ForwardingSyncMapImpl.this.get(this.key);
    }

    @Override
    public @Nullable V setValue(final @NotNull V value) {
      return ForwardingSyncMapImpl.this.replace(this.key, value);
    }

    @Override
    public int hashCode() {
      return Objects.hash(this.getKey(), this.getValue());
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
    public String toString() {
      return "EntryImpl{key=" + this.getKey() + ", value=" + this.getValue() + "}";
    }
  }

  /* package */ final class EntrySet extends AbstractSet<Map.Entry<K, V>> {
    @Override
    public int size() {
      return ForwardingSyncMapImpl.this.size();
    }

    @Override
    public boolean contains(final @Nullable Object other) {
      if(!(other instanceof Map.Entry)) return false;
      final Map.Entry<?, ?> mapEntry = (Entry<?, ?>) other;
      final V value = ForwardingSyncMapImpl.this.get(mapEntry.getKey());
      return value != null && Objects.equals(value, mapEntry.getValue());
    }

    @Override
    public boolean add(final @NotNull Entry<K, V> entry) {
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
    public @NotNull Iterator<Entry<K, V>> iterator() {
      return new EntryIterator(ForwardingSyncMapImpl.this.promote().entrySet().iterator());
    }
  }

  /* package */ final class EntryIterator implements Iterator<Map.Entry<K, V>> {
    private final Iterator<Map.Entry<K, ExpungingValue<V>>> backingIterator;
    private Map.Entry<K, V> next;
    private Map.Entry<K, V> current;

    /* package */ EntryIterator(final @NotNull Iterator<Map.Entry<K, ExpungingValue<V>>> backingIterator) {
      this.backingIterator = backingIterator;
      this.advance();
    }

    @Override
    public boolean hasNext() {
      return this.next != null;
    }

    @Override
    public @NotNull Entry<K, V> next() {
      if((this.current = this.next) == null) throw new NoSuchElementException();
      this.advance();
      return this.current;
    }

    @Override
    public void remove() {
      if(this.current == null) throw new IllegalStateException();
      ForwardingSyncMapImpl.this.remove(this.current.getKey());
      this.current = null;
    }

    private void advance() {
      this.next = null;
      while(this.backingIterator.hasNext()) {
        final Map.Entry<K, ExpungingValue<V>> entry;
        if(!(entry = this.backingIterator.next()).getValue().empty()) {
          this.next = new EntryImpl(entry.getKey());
          return;
        }
      }
    }
  }
}

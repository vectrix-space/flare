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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.function.IntFunction;

/**
 * A concurrent map, internally backed by a non-thread-safe map but carefully
 * managed in a matter such that any changes are thread-safe. Internally, the
 * map is split into an {@code immutable} and {@code mutable} map. The
 * immutable map only satisfies read requests, while the mutable map satisfies
 * all other requests.
 *
 * <p>The map is optimized for two common use cases:</p>
 *
 * <ul>
 *     <li>The entry for the given map is only written once but read many
 *         times, as in a cache that only grows.</li>
 *
 *     <li>Heavy concurrent modification of entries over a disjoint set of
 *         keys.</li>
 * </ul>
 *
 * <p>In both cases, this map significantly reduces lock contention compared
 * to a traditional map paired with a read and write lock, along with maps
 * with an exclusive lock (such as using
 * {@link Collections#synchronizedMap(Map)}).</p>
 *
 * <p>Null values are not accepted. Null keys are supported if the backing
 * collection supports them.</p>
 *
 * <p>Based on: <a href="https://go.dev/src/sync/map.go">sync/map.go</a></p>
 *
 * @param <K> the key type
 * @param <V> the value type
 * @since 0.1.0
 */
public interface ForwardingSyncMap<K, V> extends ConcurrentMap<K, V> {
  /**
   * Returns a new sync map, backed by a {@link HashMap}.
   *
   * @param <K> the key type
   * @param <V> the value type
   * @return a sync map
   * @since 0.1.0
   */
  @SuppressWarnings("RedundantTypeArguments")
  static <K, V> @NotNull ForwardingSyncMap<K, V> hashmap() {
    return of(HashMap<K, ForwardingSyncMap.ExpungingValue<V>>::new, 16);
  }

  /**
   * Returns a new sync map, backed by a {@link HashMap} with a provided
   * initial capacity.
   *
   * @param initialCapacity the initial capacity of the hash map
   * @param <K> the key type
   * @param <V> the value type
   * @return a sync map
   * @since 0.1.0
   */
  @SuppressWarnings("RedundantTypeArguments")
  static <K, V> @NotNull ForwardingSyncMap<K, V> hashmap(final int initialCapacity) {
    return of(HashMap<K, ForwardingSyncMap.ExpungingValue<V>>::new, initialCapacity);
  }

  /**
   * Returns a new mutable set view of a sync map, backed by a {@link HashMap}.
   *
   * @param <K> the key type
   * @return a mutable set view of a sync map
   * @since 0.1.0
   */
  @SuppressWarnings("RedundantTypeArguments")
  static <K> @NotNull Set<K> hashset() {
    return setOf(HashMap<K, ForwardingSyncMap.ExpungingValue<Boolean>>::new, 16);
  }

  /**
   * Returns a new mutable set view of a sync map, backed by a {@link HashMap}
   * with a provided initial capacity.
   *
   * @param initialCapacity the initial capacity of the hash map
   * @param <K> the key type
   * @return a mutable set view of a sync map
   * @since 0.1.0
   */
  @SuppressWarnings("RedundantTypeArguments")
  static <K> @NotNull Set<K> hashset(final int initialCapacity) {
    return setOf(HashMap<K, ForwardingSyncMap.ExpungingValue<Boolean>>::new, initialCapacity);
  }

  /**
   * Returns a new sync map, backed by the provided {@link Map} implementation
   * with a provided initial capacity.
   *
   * @param function the map creation function
   * @param initialCapacity the map initial capacity
   * @param <K> the key type
   * @param <V> the value type
   * @return a sync map
   * @since 0.2.0
   */
  static <K, V> @NotNull ForwardingSyncMap<K, V> of(final @NotNull IntFunction<Map<K, ForwardingSyncMap.ExpungingValue<V>>> function, final int initialCapacity) {
    return new ForwardingSyncMapImpl<>(function, initialCapacity);
  }

  /**
   * Returns a new mutable set view of a sync map, backed by the provided
   * {@link Map} implementation with a provided initial capacity.
   *
   * @param function the map creation function
   * @param initialCapacity the map initial capacity
   * @param <K> they key type
   * @return a mutable set view of a sync map
   * @since 0.2.0
   */
  static <K> @NotNull Set<K> setOf(final @NotNull IntFunction<Map<K, ForwardingSyncMap.ExpungingValue<Boolean>>> function, final int initialCapacity) {
    return Collections.newSetFromMap(new ForwardingSyncMapImpl<>(function, initialCapacity));
  }

  /**
   * {@inheritDoc}
   *
   * This implementation is {@code O(n)} in nature due to the need to check
   * for any expunged entries. Likewise, as with other concurrent collections,
   * the value obtained by this method may be out of date by the time this
   * method returns.
   *
   * @return the size of all the mappings contained in this map
   */
  @Override
  int size();

  /**
   * {@inheritDoc}
   *
   * The remapping function may be called more than once to avoid locking if
   * it is not necessary.
   *
   * @param key key with which the specified value is to be associated
   * @param remappingFunction the remapping function to compute a value
   * @return the new value associated with the specified key, or null if none
   */
  @Override
  @Nullable V computeIfPresent(final @Nullable K key, final @NotNull BiFunction<? super K, ? super V, ? extends V> remappingFunction);

  /**
   * {@inheritDoc}
   *
   * The remapping function may be called more than once to avoid locking if
   * it is not necessary.
   *
   * @param key key with which the specified value is to be associated
   * @param remappingFunction the remapping function to compute a value
   * @return the new value associated with the specified key, or null if none
   */
  @Override
  @Nullable V compute(final @Nullable K key, final @NotNull BiFunction<? super K, ? super V, ? extends V> remappingFunction);

  /**
   * {@inheritDoc}
   *
   * This method clears the map by resetting the internal state to a state
   * similar to as if a new map had been created. If there are concurrent
   * iterations in progress, they will reflect the state of the map prior to
   * being cleared.
   */
  @Override
  void clear();

  /**
   * {@inheritDoc}
   *
   * Iterations over a sync map are thread-safe, and the keys iterated over
   * will not change for a single iteration attempt, however they may not
   * necessarily reflect the state of the map at the time the iterator was
   * created.
   *
   * <p>Performance Note: If entries have been appended to the map, iterating
   * over the entry set will automatically promote them to the read map.</p>
   */
  @Override
  @NotNull Set<Entry<K, V>> entrySet();

  /**
   * Wrapper for a value that can be expunged.
   *
   * @param <V> the value type
   * @since 3.0.0
   */
  interface ExpungingValue<V> {
    /**
     * Returns {@code true} if this entry has been expunged or is {@code null}.
     * Otherwise, it returns {@code false}.
     *
     * @return true if the value does not exist, otherwise false
     * @since 3.0.0
     */
    boolean empty();

    /**
     * Returns the value or {@code null} if it has been expunged.
     *
     * @return the value or null if it is expunged
     * @since 3.0.0
     */
    @Nullable V get();

    /**
     * Returns the value if it exists, otherwise it returns the default value.
     *
     * @param defaultValue the default value
     * @return the value if present, otherwise the default value
     * @since 3.0.0
     */
    @NotNull V getOrDefault(final @NotNull V defaultValue);

    /**
     * Attempts to set the value, if the value is not expunged and returns the
     * previous value. Otherwise, it returns {@code null}.
     *
     * @param value the value
     * @return the previous value if it exists, otherwise null
     * @since 3.0.0
     */
    Map.@NotNull Entry<V, ForwardingSyncMap.Operation> set(final @NotNull V value);

    /**
     * Attempts to update the value, if the value is not expunged and is equal
     * to the provided {@code compare} object, returning the previous value and
     * the {@link ForwardingSyncMap.Operation}.
     *
     * @param compare the object to compare to
     * @param next the value to store
     * @return the previous value or null and the operation
     * @since 3.0.0
     */
    Map.@NotNull Entry<V, ForwardingSyncMap.Operation> update(final @Nullable Object compare, final @Nullable V next);

    /**
     * Attempts to update the value, if the value is not expunged and is equal
     * to the provided {@code compare} object, returning the next value and
     * the {@link ForwardingSyncMap.Operation}.
     *
     * @param compare the object to compare to
     * @param next the value to store
     * @return the next value or null and the operation
     * @since 3.0.0
     */
    Map.@NotNull Entry<V, ForwardingSyncMap.Operation> updateAbsent(final @Nullable Object compare, final @Nullable V next);

    /**
     * Attempts to compute the value in the provided {@link BiFunction} and
     * store it returning the next value and the
     * {@link ForwardingSyncMap.Operation}.
     *
     * @param key the key
     * @param remappingFunction the remapping function
     * @param <K> the key type
     * @return the next value or null and the operation
     * @since 3.0.0
     */
    <K> Map.@NotNull Entry<V, ForwardingSyncMap.Operation> compute(final @Nullable K key, final @NotNull BiFunction<? super K, ? super V, ? extends V> remappingFunction);

    /**
     * Attempts to compute the value in the provided {@link BiFunction} and
     * store it if the value is not expunged returning the next value and
     * the {@link ForwardingSyncMap.Operation}.
     *
     * @param key the key
     * @param remappingFunction the remapping function
     * @param <K> the key type
     * @return the next value or null and the operation
     * @since 3.0.0
     */
    <K> Map.@NotNull Entry<V, ForwardingSyncMap.Operation> computePresent(final @Nullable K key, final @NotNull BiFunction<? super K, ? super V, ? extends V> remappingFunction);

    /**
     * Updates the value, if the value is equal to the provided {@code compare}
     * object, returning the previous value and the
     * {@link ForwardingSyncMap.Operation}.
     *
     * @param compare the object to compare to
     * @param next the value to store
     * @return the previous value and the operation
     * @since 3.0.0
     */
    Map.@NotNull Entry<V, ForwardingSyncMap.Operation> forceUpdate(final @Nullable Object compare, final @Nullable V next);

    /**
     * Updates the value, if the value is equal to the provided {@code compare}
     * object, returning the next value and the
     * {@link ForwardingSyncMap.Operation}.
     *
     * @param compare the object to compare to
     * @param next the value to store
     * @return the next value and the operation
     * @since 3.0.0
     */
    Map.@NotNull Entry<V, ForwardingSyncMap.Operation> forceUpdateAbsent(final @Nullable Object compare, final @Nullable V next);

    /**
     * Clears and returns the previous value.
     *
     * @return the previous
     * @since 3.0.0
     */
    @Nullable V clear();

    /**
     * Attempts to unexpunge the value and store the provided value, returning
     * {@code true} if successful, otherwise returns {@code false}.
     *
     * @param value the value to store
     * @return true if unexpunged, otherwise false
     * @since 3.0.0
     */
    boolean unexpunge(final @Nullable V value);

    /**
     * Attempts to expunge the value and returns {@code true},
     * otherwise returns {@code false}.
     *
     * @return true if expunged, otherwise false
     * @since 3.0.0
     */
    boolean expunge();

    /**
     * Attempts to update the value with {@code update}, if the value is not
     * expunged and is equal to the provided {@code compare} object, returning
     * {@code true}. Otherwise returns {@code false}.
     *
     * @param compare the object to compare to
     * @param update the value to store
     * @return true if updated, otherwise false
     * @since 3.0.0
     */
    boolean compareAndSet(final @Nullable Object compare, final @Nullable Object update);

    /**
     * Stores the provided value.
     *
     * @param value the value to store
     * @since 3.0.0
     */
    void forceSet(final @Nullable Object value);
  }

  /**
   * The operation result.
   *
   * @since 3.0.0
   */
  enum Operation {
    MODIFIED,
    UNMODIFIED,
    EXPUNGED
  }
}

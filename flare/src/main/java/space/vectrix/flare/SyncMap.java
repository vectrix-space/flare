/*
 * This file is part of Ignite, licensed under the MIT License (MIT).
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

/**
 * A concurrent map, internally backed by a non-thread-safe map but carefully
 * managed in a matter such that any changes are thread-safe. Internally, the
 * map is split into a {@code read} and a {@code dirty} map. The read map only
 * satisfies read requests, while the dirty map satisfies all other requests.
 *
 * <p>The map is optimized for two common use cases:</p>
 *
 * <ul>
 *     <li>The entry for the given map is only written once but read many
 *         times, as in a cache that only grows.</li>
 *
 *     <li>Heavy concurrent modification of entries for a disjoint set of
 *         keys.</li>
 * </ul>
 *
 * <p>In both cases, this map significantly reduces lock contention compared
 * to a traditional map paired with a read and write lock, along with maps
 * with an exclusive lock (such as using {@link Collections#synchronizedMap(Map)}.</p>
 *
 * <p>Null values are not accepted. Null keys are supported if the backing collection
 * supports them.</p>
 *
 * <p>Based on: https://golang.org/src/sync/map.go</p>
 *
 * @param <K> the key type
 * @param <V> the value type
 * @since 0.1.0
 */
public interface SyncMap<K, V> extends ConcurrentMap<K, V> {
  /**
   * Returns a new sync map, backed by a {@link HashMap}.
   *
   * @param <K> the key type
   * @param <V> the value type
   * @return a sync map
   * @since 0.1.0
   */
  @SuppressWarnings("RedundantTypeArguments")
  static <K, V> @NonNull SyncMap<K, V> hashmap() {
    return of(HashMap<K, ExpungingValue<V>>::new, 16);
  }

  /**
   * Returns a new sync map, backed by a {@link HashMap} with a provided initial
   * capacity.
   *
   * @param initialCapacity the initial capacity of the hash map
   * @param <K> the key type
   * @param <V> the value type
   * @return a sync map
   * @since 0.1.0
   */
  @SuppressWarnings("RedundantTypeArguments")
  static <K, V> @NonNull SyncMap<K, V> hashmap(final int initialCapacity) {
    return of(HashMap<K, ExpungingValue<V>>::new, initialCapacity);
  }

  /**
   * Returns a new mutable set view of a sync map, backed by a {@link HashMap}.
   *
   * @param <K> the key type
   * @return a mutable set view of a sync map
   * @since 0.1.0
   */
  @SuppressWarnings("RedundantTypeArguments")
  static <K> @NonNull Set<K> hashset() {
    return setOf(HashMap<K, ExpungingValue<Boolean>>::new, 16);
  }

  /**
   * Returns a new mutable set view of a sync map, backed by a {@link HashMap} with
   * a provided initial capacity.
   *
   * @param initialCapacity the initial capacity of the hash map
   * @param <K> the key type
   * @return a mutable set view of a sync map
   * @since 0.1.0
   */
  @SuppressWarnings("RedundantTypeArguments")
  static <K> @NonNull Set<K> hashset(final int initialCapacity) {
    return setOf(HashMap<K, ExpungingValue<Boolean>>::new, initialCapacity);
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
   * @since 0.1.0
   */
  static <K, V> @NonNull SyncMap<K, V> of(final @NonNull Function<Integer, Map<K, ExpungingValue<V>>> function, final int initialCapacity) {
    return new SyncMapImpl<>(function, initialCapacity);
  }

  /**
   * Returns a new mutable set view of a sync map, backed by the provided
   * {@link Map} implementation with a provided initial capacity.
   *
   * @param function the map creation function
   * @param initialCapacity the map initial capacity
   * @param <K> they key type
   * @return a mutable set view of a sync map
   * @since 0.1.0
   */
  static <K> Set<K> setOf(final @NonNull Function<Integer, Map<K, ExpungingValue<Boolean>>> function, final int initialCapacity) {
    return Collections.newSetFromMap(new SyncMapImpl<>(function, initialCapacity));
  }

  /**
   * {@inheritDoc}
   *
   * Iterations over a sync map are thread-safe, and the keys iterated over will not change for a single iteration
   * attempt, however they may not necessarily reflect the state of the map at the time the iterator was created.
   *
   * <p>Performance Note: If entries have been appended to the map, iterating over the entry set will automatically
   * promote them to the read map.</p>
   */
  @Override
  @NonNull Set<Entry<K, V>> entrySet();

  /**
   * {@inheritDoc}
   *
   * This implementation is {@code O(n)} in nature due to the need to check for any expunged entries. Likewise, as
   * with other concurrent collections, the value obtained by this method may be out of date by the time this method
   * returns.
   *
   * @return the size of all the mappings contained in this map
   */
  @Override
  int size();

  /**
   * {@inheritDoc}
   *
   * This method clears the map by resetting the internal state to a state similar to as if a new map had been created.
   * If there are concurrent iterations in progress, they will reflect the state of the map prior to being cleared.
   */
  @Override
  void clear();

  /**
   * The expunging value the backing map wraps for its values.
   *
   * @param <V> the backing value type
   */
  interface ExpungingValue<V> {
    /**
     * Returns the backing value, which may be {@code null} if it has been expunged.
     *
     * @return the backing value if it has not been expunged
     * @since 0.1.0
     */
    @Nullable V get();

    /**
     * Attempts to place the value into the map if it is absent.
     *
     * @param value the value to place in the map
     * @return {@link Map.Entry} with a {@code false} key and {@code null} value if the element
     *         was expunged, otherwise a {@code true} key and value from the map
     * @since 0.1.0
     */
    Map.@NonNull Entry<Boolean, V> putIfAbsent(final @NonNull V value);

    /**
     * Returns {@code true} if this value has been expunged.
     *
     * @return {@code true} if this entry has been expunged, otherwise {@code false}
     * @since 0.1.0
     */
    boolean expunged();

    /**
     * Returns {@code true} if this element has a value (it is neither expunged nor {@code null}).
     *
     * @return {@code true} if this entry exists, otherwise {@code false}
     * @since 0.1.0
     */
    boolean exists();

    /**
     * Sets the backing value.
     *
     * @param value the value
     * @since 0.1.0
     */
    void set(final @NonNull V value);

    /**
     * Tries to replace the backing value, which can be set to {@code null}. This operation has no effect
     * if the entry was expunged.
     *
     * @param compare the value to compare against
     * @param value the new value to be stored
     * @return {@code true} if successful, otherwise {@code false}
     * @since 0.1.0
     */
    boolean replace(final @NonNull Object compare, final @Nullable V value);

    /**
     * Clears the value stored in this entry. Has no effect if {@code null} is stored in the map or
     * the entry was expunged.
     *
     * @return the previous element stored, or {@code null} if the entry had been expunged
     * @since 0.1.0
     */
    @Nullable V clear();

    /**
     * Tries to set the backing element. If the entry is expunged, this operation
     * will fail.
     *
     * @param value the new value
     * @return {@code true} if the entry was not expunged, {@code false} otherwise
     * @since 0.1.0
     */
    boolean trySet(final @NonNull V value);

    /**
     * Tries to mark the entry as expunged if its value is {@code null}.
     *
     * @return whether or not the item has been expunged
     * @since 0.1.0
     */
    boolean tryMarkExpunged();

    /**
     * Tries to set the backing value, which can be set to {@code null}, if the entry was expunged.
     *
     * @param value the new value
     * @return {@code true} if the entry was unexpunged, otherwise {@code false}
     * @since 0.1.0
     */
    boolean tryUnexpungeAndSet(final @Nullable V value);
  }
}

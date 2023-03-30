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
package space.vectrix.flare.fastutil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/* package */ interface ExpungingValue<V> {
  /**
   * Returns {@code true} if this entry has been expunged or is {@code null}.
   * Otherwise, it returns {@code false}.
   *
   * @return true if the value does not exist, otherwise false
   * @since 2.1.0
   */
  boolean empty();

  /**
   * Returns the value or {@code null} if it has been expunged.
   *
   * @return the value or null if it is expunged
   * @since 2.0.0
   */
  @Nullable V get();

  /**
   * Returns the value if it exists, otherwise it returns the default value.
   *
   * @param defaultValue the default value
   * @return the value if present, otherwise the default value
   * @since 2.1.0
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
  Map.@NotNull Entry<V, Operation> set(final @NotNull V value);

  /**
   * Attempts to update the value, if the value is not expunged and is equal
   * to the provided {@code compare} object, returning the previous value and
   * the {@link Operation}.
   *
   * @param compare the object to compare to
   * @param next the value to store
   * @return the previous value or null and the operation
   * @since 3.0.0
   */
  Map.@NotNull Entry<V, Operation> update(final @Nullable Object compare, final @Nullable V next);

  /**
   * Attempts to update the value, if the value is not expunged and is equal
   * to the provided {@code compare} object, returning the next value and
   * the {@link Operation}.
   *
   * @param compare the object to compare to
   * @param next the value to store
   * @return the next value or null and the operation
   * @since 3.0.0
   */
  Map.@NotNull Entry<V, Operation> updateAbsent(final @Nullable Object compare, final @Nullable V next);

  /**
   * Updates the value, if the value is equal to the provided {@code compare}
   * object, returning the previous value and the
   * {@link Operation}.
   *
   * @param compare the object to compare to
   * @param next the value to store
   * @return the previous value and the operation
   * @since 3.0.0
   */
  Map.@NotNull Entry<V, Operation> forceUpdate(final @Nullable Object compare, final @Nullable V next);

  /**
   * Updates the value, if the value is equal to the provided {@code compare}
   * object, returning the next value and the
   * {@link Operation}.
   *
   * @param compare the object to compare to
   * @param next the value to store
   * @return the next value and the operation
   * @since 3.0.0
   */
  Map.@NotNull Entry<V, Operation> forceUpdateAbsent(final @Nullable Object compare, final @Nullable V next);

  /**
   * Clears and returns the previous value.
   *
   * @return the previous
   * @since 2.0.0
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

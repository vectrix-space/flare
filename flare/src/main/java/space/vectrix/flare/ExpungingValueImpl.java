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

import java.util.AbstractMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.BiFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings({"rawtypes", "unchecked"})
/* package */ final class ExpungingValueImpl<V> implements ForwardingSyncMap.ExpungingValue<V> {
  private static final AtomicReferenceFieldUpdater<ExpungingValueImpl, Object> UPDATER = AtomicReferenceFieldUpdater
    .newUpdater(ExpungingValueImpl.class, Object.class, "value");
  private static final Object EXPUNGED = new Object();
  private volatile Object value;

  /* package */ ExpungingValueImpl(final @NotNull V value) {
    this.value = value;
  }

  @Override
  public boolean empty() {
    return this.value == null || this.value == ExpungingValueImpl.EXPUNGED;
  }

  @Override
  public @Nullable V value() {
    return this.value == ExpungingValueImpl.EXPUNGED ? null : (V) this.value;
  }

  @Override
  public @NotNull V valueOrDefault(final @NotNull V defaultValue) {
    return this.empty() ? defaultValue : (V) this.value;
  }

  @Override
  public Map.@NotNull Entry<V, ForwardingSyncMap.Operation> set(final @NotNull V value) {
    for(; ; ) {
      final Object previous = this.value;
      if(previous == ExpungingValueImpl.EXPUNGED) return new AbstractMap.SimpleImmutableEntry<>(null, ForwardingSyncMap.Operation.EXPUNGED);
      if(this.compareAndSet(previous, value)) return new AbstractMap.SimpleImmutableEntry<>((V) previous, ForwardingSyncMap.Operation.MODIFIED);
    }
  }

  @Override
  public Map.@NotNull Entry<V, ForwardingSyncMap.Operation> update(final @Nullable Object compare, final @Nullable V next) {
    for(; ; ) {
      final Object previous = this.value;
      if(previous == ExpungingValueImpl.EXPUNGED) return new AbstractMap.SimpleImmutableEntry<>(null, ForwardingSyncMap.Operation.EXPUNGED);
      if(!Objects.equals(previous, compare)) return new AbstractMap.SimpleImmutableEntry<>((V) previous, ForwardingSyncMap.Operation.UNMODIFIED);
      if(this.compareAndSet(previous, next)) return new AbstractMap.SimpleImmutableEntry<>((V) previous, ForwardingSyncMap.Operation.MODIFIED);
    }
  }

  @Override
  public Map.@NotNull Entry<V, ForwardingSyncMap.Operation> updateAbsent(final @Nullable Object compare, final @Nullable V next) {
    for(; ; ) {
      final Object previous = this.value;
      if(previous == ExpungingValueImpl.EXPUNGED) return new AbstractMap.SimpleImmutableEntry<>(null, ForwardingSyncMap.Operation.EXPUNGED);
      if(!Objects.equals(previous, compare)) return new AbstractMap.SimpleImmutableEntry<>((V) previous, ForwardingSyncMap.Operation.UNMODIFIED);
      if(this.compareAndSet(previous, next)) return new AbstractMap.SimpleImmutableEntry<>(next, ForwardingSyncMap.Operation.MODIFIED);
    }
  }

  @Override
  public <K> Map.@NotNull Entry<V, ForwardingSyncMap.Operation> compute(final @Nullable K key, final @NotNull BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
    V next;
    for(; ; ) {
      final Object previous = this.value;
      if(previous == ExpungingValueImpl.EXPUNGED) return new AbstractMap.SimpleImmutableEntry<>(null, ForwardingSyncMap.Operation.EXPUNGED);
      if(this.compareAndSet(previous, next = remappingFunction.apply(key, (V) previous))) return new AbstractMap.SimpleImmutableEntry<>(next, ForwardingSyncMap.Operation.MODIFIED);
    }
  }

  @Override
  public <K> Map.@NotNull Entry<V, ForwardingSyncMap.Operation> computePresent(final @Nullable K key, final @NotNull BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
    V next;
    for(; ; ) {
      final Object previous = this.value;
      if(previous == ExpungingValueImpl.EXPUNGED) return new AbstractMap.SimpleImmutableEntry<>(null, ForwardingSyncMap.Operation.EXPUNGED);
      if(previous == null) return new AbstractMap.SimpleImmutableEntry<>(null, ForwardingSyncMap.Operation.UNMODIFIED);
      if(this.compareAndSet(previous, next = remappingFunction.apply(key, (V) previous))) return new AbstractMap.SimpleImmutableEntry<>(next, ForwardingSyncMap.Operation.MODIFIED);
    }
  }

  @Override
  public Map.@NotNull Entry<V, ForwardingSyncMap.Operation> forceUpdate(final @Nullable Object compare, final @Nullable V next) {
    for(; ; ) {
      final Object previous = this.value == ExpungingValueImpl.EXPUNGED ? null : this.value;
      if(!Objects.equals(previous, compare)) return new AbstractMap.SimpleImmutableEntry<>((V) previous, ForwardingSyncMap.Operation.UNMODIFIED);
      if(this.compareAndSet(previous, next)) return new AbstractMap.SimpleImmutableEntry<>((V) previous, ForwardingSyncMap.Operation.MODIFIED);
    }
  }

  @Override
  public Map.@NotNull Entry<V, ForwardingSyncMap.Operation> forceUpdateAbsent(final @Nullable Object compare, final @Nullable V next) {
    for(; ; ) {
      final Object previous = this.value == ExpungingValueImpl.EXPUNGED ? null : this.value;
      if(!Objects.equals(previous, compare)) return new AbstractMap.SimpleImmutableEntry<>((V) previous, ForwardingSyncMap.Operation.UNMODIFIED);
      if(this.compareAndSet(previous, next)) return new AbstractMap.SimpleImmutableEntry<>(next, ForwardingSyncMap.Operation.MODIFIED);
    }
  }

  @Override
  public @Nullable V clear() {
    for(; ; ) {
      final Object previous = this.value;
      if(previous == null || previous == ExpungingValueImpl.EXPUNGED) return null;
      if(this.compareAndSet(previous, null)) return (V) previous;
    }
  }

  @Override
  public boolean unexpunge(final @Nullable V value) {
    return this.compareAndSet(ExpungingValueImpl.EXPUNGED, value);
  }

  @Override
  public boolean expunge() {
    if(this.value == ExpungingValueImpl.EXPUNGED) return true;
    return this.compareAndSet(null, ExpungingValueImpl.EXPUNGED);
  }

  @Override
  public boolean compareAndSet(final @Nullable Object compare, final @Nullable Object update) {
    return ExpungingValueImpl.UPDATER.compareAndSet(this, compare, update);
  }

  @Override
  public void forceSet(final @Nullable Object value) {
    ExpungingValueImpl.UPDATER.set(this, value);
  }
}

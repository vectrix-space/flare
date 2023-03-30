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

import java.util.AbstractMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

@SuppressWarnings({"rawtypes", "unchecked"})
/* package */ abstract class AbstractExpungingValue<V> implements ExpungingValue<V> {
  protected static final AtomicReferenceFieldUpdater<AbstractExpungingValue, Object> UPDATER = AtomicReferenceFieldUpdater
    .newUpdater(AbstractExpungingValue.class, Object.class, "value");
  protected static final Object EXPUNGED = new Object();
  protected volatile Object value;

  /* package */ AbstractExpungingValue(final @NotNull V value) {
    this.value = value;
  }

  @Override
  public boolean empty() {
    return this.value == null || this.value == AbstractExpungingValue.EXPUNGED;
  }

  @Override
  public @Nullable V get() {
    return this.value == AbstractExpungingValue.EXPUNGED ? null : (V) this.value;
  }

  @Override
  public @NotNull V getOrDefault(final @NotNull V defaultValue) {
    return this.empty() ? defaultValue : (V) this.value;
  }

  @Override
  public Map.@NotNull Entry<V, Operation> set(final @NotNull V value) {
    for(; ; ) {
      final Object previous = this.value;
      if(previous == AbstractExpungingValue.EXPUNGED) return new AbstractMap.SimpleImmutableEntry<>(null, Operation.EXPUNGED);
      if(this.compareAndSet(previous, value)) return new AbstractMap.SimpleImmutableEntry<>((V) previous, Operation.MODIFIED);
    }
  }

  @Override
  public Map.@NotNull Entry<V, Operation> update(final @Nullable Object compare, final @Nullable V next) {
    for(; ; ) {
      final Object previous = this.value;
      if(previous == AbstractExpungingValue.EXPUNGED) return new AbstractMap.SimpleImmutableEntry<>(null, Operation.EXPUNGED);
      if(!Objects.equals(previous, compare)) return new AbstractMap.SimpleImmutableEntry<>((V) previous, Operation.UNMODIFIED);
      if(this.compareAndSet(previous, next)) return new AbstractMap.SimpleImmutableEntry<>((V) previous, Operation.MODIFIED);
    }
  }

  @Override
  public Map.@NotNull Entry<V, Operation> updateAbsent(final @Nullable Object compare, final @Nullable V next) {
    for(; ; ) {
      final Object previous = this.value;
      if(previous == AbstractExpungingValue.EXPUNGED) return new AbstractMap.SimpleImmutableEntry<>(null, Operation.EXPUNGED);
      if(!Objects.equals(previous, compare)) return new AbstractMap.SimpleImmutableEntry<>((V) previous, Operation.UNMODIFIED);
      if(this.compareAndSet(previous, next)) return new AbstractMap.SimpleImmutableEntry<>(next, Operation.MODIFIED);
    }
  }

  @Override
  public Map.@NotNull Entry<V, Operation> forceUpdate(final @Nullable Object compare, final @Nullable V next) {
    for(; ; ) {
      final Object previous = this.value == AbstractExpungingValue.EXPUNGED ? null : this.value;
      if(!Objects.equals(previous, compare)) return new AbstractMap.SimpleImmutableEntry<>((V) previous, Operation.UNMODIFIED);
      if(this.compareAndSet(previous, next)) return new AbstractMap.SimpleImmutableEntry<>((V) previous, Operation.MODIFIED);
    }
  }

  @Override
  public Map.@NotNull Entry<V, Operation> forceUpdateAbsent(final @Nullable Object compare, final @Nullable V next) {
    for(; ; ) {
      final Object previous = this.value == AbstractExpungingValue.EXPUNGED ? null : this.value;
      if(!Objects.equals(previous, compare)) return new AbstractMap.SimpleImmutableEntry<>((V) previous, Operation.UNMODIFIED);
      if(this.compareAndSet(previous, next)) return new AbstractMap.SimpleImmutableEntry<>(next, Operation.MODIFIED);
    }
  }

  @Override
  public @Nullable V clear() {
    for(; ; ) {
      final Object previous = this.value;
      if(previous == null || previous == AbstractExpungingValue.EXPUNGED) return null;
      if(this.compareAndSet(previous, null)) return (V) previous;
    }
  }


  @Override
  public boolean unexpunge(final @Nullable V value) {
    return this.compareAndSet(AbstractExpungingValue.EXPUNGED, value);
  }

  @Override
  public boolean expunge() {
    if(this.value == AbstractExpungingValue.EXPUNGED) return true;
    return this.compareAndSet(null, AbstractExpungingValue.EXPUNGED);
  }

  @Override
  public boolean compareAndSet(final @Nullable Object compare, final @Nullable Object update) {
    return AbstractExpungingValue.UPDATER.compareAndSet(this, compare, update);
  }

  @Override
  public void forceSet(final @Nullable Object value) {
    AbstractExpungingValue.UPDATER.set(this, value);
  }
}

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
package space.vectrix.flare.collection.primitive;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import space.vectrix.flare.Constants;
import space.vectrix.flare.fastutil.Int2ObjectSyncMap;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@Fork(value = Constants.FORK)
@Warmup(iterations = Constants.WARM_UP_ITERATIONS, time = Constants.WARM_UP_ITERATIONS_TIME)
@Measurement(iterations = Constants.ITERATIONS, time = Constants.ITERATIONS_TIME)
@OutputTimeUnit(TimeUnit.SECONDS)
public class HighConcurrentPrimitiveMapMutationTest {
  private static final int SIZE = 1_000_000;

  private final Int2ObjectMap<Boolean> synchronizedMap = Int2ObjectMaps.synchronize(new Int2ObjectOpenHashMap<>());
  private final Int2ObjectMap<Boolean> syncMap = Int2ObjectSyncMap.hashmap();

  private final AtomicInteger counter = new AtomicInteger();

  @Benchmark
  @Threads(Constants.HIGH_THREADS)
  public void synchronizedMap(Blackhole blackhole) {
    final Random shouldPut = new Random();
    for(int i = 0; i < HighConcurrentPrimitiveMapMutationTest.SIZE; i++) {
      int value = this.counter.get();
      if(shouldPut.nextBoolean()) {
        blackhole.consume(this.synchronizedMap.put(value, Boolean.TRUE));
      } else {
        blackhole.consume(this.synchronizedMap.remove(value));
      }
    }
  }

  @Benchmark
  @Threads(Constants.HIGH_THREADS)
  public void syncMap(Blackhole blackhole) {
    final Random shouldPut = new Random();
    for(int i = 0; i < HighConcurrentPrimitiveMapMutationTest.SIZE; i++) {
      int value = this.counter.get();
      if(shouldPut.nextBoolean()) {
        blackhole.consume(this.syncMap.put(value, Boolean.TRUE));
      } else {
        blackhole.consume(this.syncMap.remove(value));
      }
    }
  }
}


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
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import space.vectrix.flare.Constants;
import space.vectrix.flare.Generator;
import space.vectrix.flare.fastutil.Int2ObjectSyncMap;

import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@Fork(value = Constants.FORK)
@Warmup(iterations = Constants.WARM_UP_ITERATIONS, time = Constants.WARM_UP_ITERATIONS_TIME)
@Measurement(iterations = Constants.ITERATIONS, time = Constants.ITERATIONS_TIME)
@OutputTimeUnit(TimeUnit.SECONDS)
public class PrimitiveMapRetrievalTest {
  private static final int SIZE = 1_000_000;

  private final Int2ObjectMap<String> synchronizedMap = Generator.generate(Int2ObjectMaps.synchronize(new Int2ObjectOpenHashMap<>()), PrimitiveMapRetrievalTest.SIZE);
  private final Int2ObjectMap<String> syncMap = Generator.generate(Int2ObjectSyncMap.hashmap(), PrimitiveMapRetrievalTest.SIZE);

  @Benchmark
  public void synchronizedMap(Blackhole blackhole) {
    for(int i = 0; i < PrimitiveMapRetrievalTest.SIZE; i++) {
      blackhole.consume(this.synchronizedMap.get(i));
    }
  }

  @Benchmark
  public void syncMap(Blackhole blackhole) {
    for(int i = 0; i < PrimitiveMapRetrievalTest.SIZE; i++) {
      blackhole.consume(this.syncMap.get(i));
    }
  }
}

package space.vectrix.flare.collection;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import space.vectrix.flare.fastutil.Int2ObjectSyncMap;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@Fork(1)
@Warmup(iterations = 2, time = 2)
@Measurement(iterations = 5, time = 4)
@OutputTimeUnit(TimeUnit.SECONDS)
public class ReadPrimitiveMapTest {
  @Param(value = { "SynchronizedMap", "SyncMap" })
  private String implementation;

  @Param(value = "100000")
  private static int size = 100000;

  private Int2ObjectMap<String> map;

  @Setup(Level.Trial)
  public void createImplementation() {
    if("SynchronizedMap".equalsIgnoreCase(this.implementation)) {
      this.map = Int2ObjectMaps.synchronize(new Int2ObjectOpenHashMap<>(ReadPrimitiveMapTest.size));
    } else if("SyncMap".equalsIgnoreCase(this.implementation)) {
      this.map = Int2ObjectSyncMap.hashmap(ReadPrimitiveMapTest.size);
    }

    for(int i = 0; i < ReadPrimitiveMapTest.size; i++) {
      this.map.put(i, String.valueOf(i));
    }

    for(int i = 0; i < ReadPrimitiveMapTest.size; i++) {
      this.map.get(i);
    }
  }

  @Benchmark
  @Threads(5)
  public void randomWrite() {
    for(int i = 0; i < ReadPrimitiveMapTest.size; i++) {
      final Random random = ThreadLocalRandom.current();
      final int randNumber = (int) Math.ceil(random.nextDouble() * ReadPrimitiveMapTest.size);
      this.map.put(randNumber, String.valueOf(randNumber));
    }
  }

  @Benchmark
  @Threads(5)
  public void randomRead() {
    for(int i = 0; i < ReadPrimitiveMapTest.size; i++) {
      final Random random = ThreadLocalRandom.current();
      final int randNumber = (int) Math.ceil(random.nextDouble() * ReadPrimitiveMapTest.size);
      this.map.get(randNumber);
    }
  }

  @Benchmark
  @Threads(5)
  public void randomReadAndWrite() {
    for(int i = 0; i < ReadPrimitiveMapTest.size; i++) {
      final Random random = ThreadLocalRandom.current();
      final int randNumber = (int) Math.ceil(random.nextDouble() * ReadPrimitiveMapTest.size);
      this.map.put(randNumber, String.valueOf(randNumber));
      this.map.get(randNumber);
    }
  }
}

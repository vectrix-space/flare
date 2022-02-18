package space.vectrix.flare.collection;

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
import space.vectrix.flare.ForwardingSyncMap;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@Fork(1)
@Warmup(iterations = 2, time = 2)
@Measurement(iterations = 5, time = 4)
@OutputTimeUnit(TimeUnit.SECONDS)
public class DirtyGenericMapTest {
  @Param(value = { "SynchronizedMap", "ConcurrentHashMap", "SyncMap" })
  private String implementation;

  @Param(value = "100000")
  private static int size = 100000;

  private Map<String, Integer> map;

  @Setup(Level.Trial)
  public void createImplementation() {
    if("SynchronizedMap".equalsIgnoreCase(this.implementation)) {
      this.map = Collections.synchronizedMap(new HashMap<>());
    } else if("ConcurrentHashMap".equalsIgnoreCase(this.implementation)) {
      this.map = new ConcurrentHashMap<>();
    } else if("SyncMap".equalsIgnoreCase(this.implementation)) {
      this.map = ForwardingSyncMap.hashmap();
    }
  }

  @Benchmark
  @Threads(5)
  public void randomWrite() {
    for(int i = 0; i < DirtyGenericMapTest.size; i++) {
      final Random random = ThreadLocalRandom.current();
      final int randNumber = (int) Math.ceil(random.nextDouble() * DirtyGenericMapTest.size);
      this.map.put(String.valueOf(randNumber), randNumber);
    }
  }

  @Benchmark
  @Threads(5)
  public void randomRead() {
    for(int i = 0; i < DirtyGenericMapTest.size; i++) {
      final Random random = ThreadLocalRandom.current();
      final int randNumber = (int) Math.ceil(random.nextDouble() * DirtyGenericMapTest.size);
      this.map.get(String.valueOf(randNumber));
    }
  }

  @Benchmark
  @Threads(5)
  public void randomReadAndWrite() {
    for(int i = 0; i < DirtyGenericMapTest.size; i++) {
      final Random random = ThreadLocalRandom.current();
      final int randNumber = (int) Math.ceil(random.nextDouble() * DirtyGenericMapTest.size);
      this.map.put(String.valueOf(randNumber), randNumber);
      this.map.get(String.valueOf(randNumber));
    }
  }
}

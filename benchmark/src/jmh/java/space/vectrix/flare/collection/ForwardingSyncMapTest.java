package space.vectrix.flare.collection;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import space.vectrix.flare.ForwardingSyncMap;

@Fork(1)
@State(Scope.Benchmark)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class ForwardingSyncMapTest {
  @Param(value = { "SynchronizedMap", "ConcurrentHashMap", "SyncMap" })
  private String implementation;

  @Param(value = "100000")
  private static int size = 100_000;

  private Map<Integer, String> populated;

  public Map<Integer, String> createEmpty() {
    if("SynchronizedMap".equalsIgnoreCase(this.implementation)) {
      return Collections.synchronizedMap(new HashMap<>(10));
    } else if("ConcurrentHashMap".equalsIgnoreCase(this.implementation)) {
      return new ConcurrentHashMap<>(10);
    } else if("SyncMap".equalsIgnoreCase(this.implementation)) {
      return ForwardingSyncMap.hashmap(10);
    }
    throw new RuntimeException("Unknown map type!");
  }

  @Setup(Level.Trial)
  public void createPopulated() {
    this.populated = this.createEmpty();

    for(int i = 0; i < ForwardingSyncMapTest.size; i++) {
      this.populated.put(i, String.valueOf(i));
    }
  }

  @Benchmark
  @Threads(5)
  public void read_sequential() {
    for(int i = 0; i < ForwardingSyncMapTest.size; i++) {
      this.populated.get(i);
    }
  }

  @Benchmark
  @Threads(5)
  public void write_sequential() {
    final Map<Integer, String> map = this.createEmpty();
    for(int i = 0; i < ForwardingSyncMapTest.size; i++) {
      map.put(i, String.valueOf(i));
    }
  }

  @Benchmark
  @Threads(5)
  public void writeAndRead_sequential() {
    final Map<Integer, String> map = this.createEmpty();
    for(int i = 0; i < ForwardingSyncMapTest.size; i++) {
      map.put(i, String.valueOf(i));
      map.get(i);
    }
  }

  @Benchmark
  @Threads(5)
  public void writeAndMoreRead_sequential() {
    final Random random = ThreadLocalRandom.current();
    for(int i = 0; i < ForwardingSyncMapTest.size; i++) {
      if(random.nextDouble() < 0.10) {
        this.populated.put(i, String.valueOf(i));
      } else {
        this.populated.get(i);
      }
    }
  }
}

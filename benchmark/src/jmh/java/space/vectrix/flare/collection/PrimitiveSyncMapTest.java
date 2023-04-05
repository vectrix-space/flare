package space.vectrix.flare.collection;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Random;
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
import space.vectrix.flare.fastutil.Int2ObjectSyncMap;

@Fork(1)
@State(Scope.Benchmark)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class PrimitiveSyncMapTest {
  @Param(value = { "SynchronizedMap", "SyncMap" })
  private String implementation;

  @Param(value = "100000")
  private static int size = 100_000;

  private Int2ObjectMap<String> populated;

  public Int2ObjectMap<String> createEmpty() {
    if("SynchronizedMap".equalsIgnoreCase(this.implementation)) {
      return Int2ObjectMaps.synchronize(new Int2ObjectOpenHashMap<>());
    } else if("SyncMap".equalsIgnoreCase(this.implementation)) {
      return Int2ObjectSyncMap.hashmap();
    }
    throw new RuntimeException("Unknown map type!");
  }

  @Setup(Level.Trial)
  public void createPopulated() {
    this.populated = this.createEmpty();

    for(int i = 0; i < PrimitiveSyncMapTest.size; i++) {
      this.populated.put(i, String.valueOf(i));
    }
  }

  @Benchmark
  @Threads(5)
  public void read_sequential() {
    for(int i = 0; i < PrimitiveSyncMapTest.size; i++) {
      this.populated.get(i);
    }
  }

  @Benchmark
  @Threads(5)
  public void write_sequential() {
    final Int2ObjectMap<String> map = this.createEmpty();
    for(int i = 0; i < PrimitiveSyncMapTest.size; i++) {
      map.put(i, String.valueOf(i));
    }
  }

  @Benchmark
  @Threads(5)
  public void writeAndRead_sequential() {
    final Int2ObjectMap<String> map = this.createEmpty();
    for(int i = 0; i < PrimitiveSyncMapTest.size; i++) {
      map.put(i, String.valueOf(i));
      map.get(i);
    }
  }

  @Benchmark
  @Threads(5)
  public void writeAndMoreRead_sequential() {
    final Random random = ThreadLocalRandom.current();
    for(int i = 0; i < PrimitiveSyncMapTest.size; i++) {
      if(random.nextDouble() < 0.10) {
        this.populated.put(i, String.valueOf(i));
      } else {
        this.populated.get(i);
      }
    }
  }
}

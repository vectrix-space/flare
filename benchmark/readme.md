# Summary

The summary of benchmarks taken for Flare.

## SyncMap

A concurrent map, internally backed by a non-thread-safe map but carefully
managed in a matter such that any changes are thread-safe. Internally, the
map is split into a `read` and a `dirty` map. The read map only satisfies read 
requests, while the dirty map satisfies all other requests.

This map is optimized for two common use cases:

- The entry for the given map is only written once but read many times, as 
  in a cache that only grows.
  
- Heavy concurrent modification of entries for a disjoint set of keys.

In both cases, this map significantly reduces lock contention compared
to a traditional map paired with a read and write lock, along with maps
with an exclusive lock (such as using `Collections#synchronizedMap(Map)`).

### Low Contention

Testing involves being read over multiple threads concurrently. These
tests involved 4 threads.

#### Read Generic

Calls `get(Object)` on the `Map` for 1 million existing entries over 5 seconds.

Local (Windows):
```
Benchmark                                                     Mode  Cnt    Score    Error  Units
LowConcurrentGenericMapRetrievalTest.concurrentHashMap       thrpt    5  192.221 ± 22.653  ops/s
LowConcurrentGenericMapRetrievalTest.syncMap                 thrpt    5  143.976 ± 14.221  ops/s
LowConcurrentGenericMapRetrievalTest.synchronizedMap         thrpt    5   21.771 ±  1.560  ops/s
```

#### Read Fastutil

Calls `get(int)` on the `Map` for 1 million existing entries over 5 seconds.

Local (Windows):
```
Benchmark                                                     Mode  Cnt    Score   Error  Units
LowConcurrentPrimitiveMapRetrievalTest.syncMap               thrpt    5   72.706 ± 3.629  ops/s
LowConcurrentPrimitiveMapRetrievalTest.synchronizedMap       thrpt    5   14.236 ± 6.368  ops/s
```

#### Write Generic

Calls `put(Object, Object)` on the `Map` for 1 million non-existent becoming existent
entries over 5 seconds.

Local (Windows):
```
Benchmark                                                     Mode  Cnt    Score   Error  Units
LowConcurrentGenericMapMutationTest.syncMap                  thrpt    5   25.310 ± 0.254  ops/s
LowConcurrentGenericMapMutationTest.synchronizedMap          thrpt    5   20.891 ± 0.223  ops/s
LowConcurrentGenericMapMutationTest.concurrentMap            thrpt    5   14.718 ± 0.999  ops/s
```

#### Write Fastutil

Calls `put(int, Object)` on the `Map` for 1 million non-existent becoming existent
entries over 5 seconds.

Local (Windows):
```
Benchmark                                                     Mode  Cnt    Score   Error  Units
LowConcurrentPrimitiveMapMutationTest.syncMap                thrpt    5   25.852 ± 0.392  ops/s
LowConcurrentPrimitiveMapMutationTest.synchronizedMap        thrpt    5   21.898 ± 0.719  ops/s
```

### High Contention

Testing involves being read over multiple threads concurrently. These
tests involved 50 threads.

#### Read Generic

Calls `get(Object)` on the `Map` for 1 million existing entries over 5 seconds.

Local (Windows):
```
Benchmark                                                     Mode  Cnt    Score   Error  Units
HighConcurrentGenericMapRetrievalTest.concurrentHashMap      thrpt    5  182.701 ± 7.399  ops/s
HighConcurrentGenericMapRetrievalTest.syncMap                thrpt    5  151.760 ± 4.356  ops/s
HighConcurrentGenericMapRetrievalTest.synchronizedMap        thrpt    5   22.462 ± 0.542  ops/s
```

#### Read Fastutil

Calls `get(int)` on the `Map` for 1 million existing entries over 5 seconds.

Local (Windows):
```
Benchmark                                                     Mode  Cnt    Score   Error  Units
HighConcurrentPrimitiveMapRetrievalTest.syncMap              thrpt    5   55.386 ± 3.189  ops/s
HighConcurrentPrimitiveMapRetrievalTest.synchronizedMap      thrpt    5   11.822 ± 0.774  ops/s
```

#### Write Generic

Calls `put(Object, Object)` on the `Map` for 1 million non-existent becoming existent
entries over 5 seconds.

Local (Windows):
```
Benchmark                                                     Mode  Cnt    Score   Error  Units
HighConcurrentGenericMapMutationTest.syncMap                 thrpt    5   25.168 ± 0.423  ops/s
HighConcurrentGenericMapMutationTest.synchronizedMap         thrpt    5   20.955 ± 0.213  ops/s
HighConcurrentGenericMapMutationTest.concurrentMap           thrpt    5   10.835 ± 1.047  ops/s
```

#### Write Fastutil

Calls `put(int, Object)` on the `Map` for 1 million non-existent becoming existent
entries over 5 seconds.

Local (Windows):
```
Benchmark                                                     Mode  Cnt    Score   Error  Units
HighConcurrentPrimitiveMapMutationTest.syncMap               thrpt    5   26.150 ± 0.697  ops/s
HighConcurrentPrimitiveMapMutationTest.synchronizedMap       thrpt    5   23.395 ± 0.094  ops/s
```

### No Contention

Testing involves no other threads, thus no locking involved.

#### Read Generic

Calls `get(Object)` on the `Map` for 1 million existing entries over 5 seconds.

Local (Windows):
```
Benchmark                                                 Mode  Cnt    Score   Error  Units
GenericMapRetrievalTest.concurrentHashMap                thrpt    5   71.008 ± 0.400  ops/s
GenericMapRetrievalTest.syncMap                          thrpt    5   62.768 ± 3.394  ops/s
GenericMapRetrievalTest.synchronizedMap                  thrpt    5   40.977 ± 0.184  ops/s
```

#### Read Fastutil

Calls `get(int)` on the `Map` for 1 million existing entries over 5 seconds.

Local (Windows):
```
Benchmark                                                 Mode  Cnt    Score   Error  Units
PrimitiveMapRetrievalTest.synchronizedMap                thrpt    5   24.490 ± 0.422  ops/s
PrimitiveMapRetrievalTest.syncMap                        thrpt    5   22.490 ± 0.079  ops/s
```

#### Write Generic

Calls `put(Object, Object)` on the `Map` for 1 million non-existent becoming existent
entries over 5 seconds.

Local (Windows):
```
Benchmark                                                 Mode  Cnt    Score   Error  Units
GenericMapMutationTest.syncMap                           thrpt    5   58.756 ± 0.412  ops/s
GenericMapMutationTest.concurrentMap                     thrpt    5   41.252 ± 2.493  ops/s
GenericMapMutationTest.synchronizedMap                   thrpt    5   39.873 ± 0.178  ops/s
```

#### Write Fastutil

Calls `put(int, Object)` on the `Map` for 1 million non-existent becoming existent
entries over 5 seconds.

Local (Windows):
```
Benchmark                                                 Mode  Cnt    Score   Error  Units
PrimitiveMapMutationTest.syncMap                         thrpt    5   68.505 ± 0.460  ops/s
PrimitiveMapMutationTest.synchronizedMap                 thrpt    5   43.895 ± 0.153  ops/s
```

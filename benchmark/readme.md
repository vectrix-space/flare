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
Benchmark                                                     Mode  Cnt    Score   Error  Units
LowConcurrentGenericMapRetrievalTest.concurrentHashMap       thrpt    5  134.140 ± 4.719  ops/s
LowConcurrentGenericMapRetrievalTest.syncMap                 thrpt    5   97.661 ± 3.171  ops/s
LowConcurrentGenericMapRetrievalTest.synchronizedMap         thrpt    5   14.866 ± 0.548  ops/s
```

#### Read Fastutil

Calls `get(int)` on the `Map` for 1 million existing entries over 5 seconds.

Local (Windows):
```
Benchmark                                                     Mode  Cnt    Score   Error  Units
LowConcurrentPrimitiveMapRetrievalTest.syncMap               thrpt    5   34.333 ± 3.637  ops/s
LowConcurrentPrimitiveMapRetrievalTest.synchronizedMap       thrpt    5   11.204 ± 0.474  ops/s
```

#### Write Generic

Calls `put(Object, Object)` on the `Map` for 1 million non-existent becoming existent
entries over 5 seconds.

Local (Windows):
```
Benchmark                                                     Mode  Cnt    Score   Error  Units
LowConcurrentGenericMapMutationTest.syncMap                  thrpt    5   27.908 ± 1.319  ops/s
LowConcurrentGenericMapMutationTest.synchronizedMap          thrpt    5   18.397 ± 1.283  ops/s
LowConcurrentGenericMapMutationTest.concurrentMap            thrpt    5   15.890 ± 0.731  ops/s
```

#### Write Fastutil

Calls `put(int, Object)` on the `Map` for 1 million non-existent becoming existent
entries over 5 seconds.

Local (Windows):
```
Benchmark                                                     Mode  Cnt    Score   Error  Units
LowConcurrentPrimitiveMapMutationTest.syncMap                thrpt    5   30.072 ± 1.662  ops/s
LowConcurrentPrimitiveMapMutationTest.synchronizedMap        thrpt    5   21.483 ± 1.007  ops/s
```

### High Contention

Testing involves being read over multiple threads concurrently. These
tests involved 50 threads.

#### Read Generic

Calls `get(Object)` on the `Map` for 1 million existing entries over 5 seconds.

Local (Windows):
```
Benchmark                                                     Mode  Cnt    Score   Error  Units
HighConcurrentGenericMapRetrievalTest.concurrentHashMap      thrpt    5  168.732 ± 4.789  ops/s
HighConcurrentGenericMapRetrievalTest.syncMap                thrpt    5  146.762 ± 4.258  ops/s
HighConcurrentGenericMapRetrievalTest.synchronizedMap        thrpt    5   13.434 ± 0.142  ops/s
```

Github Actions (Windows):
```
Benchmark                                                     Mode  Cnt    Score     Error  Units
HighConcurrentGenericMapRetrievalTest.concurrentHashMap      thrpt    5  438.827 ± 797.399  ops/s
HighConcurrentGenericMapRetrievalTest.syncMap                thrpt    5  326.290 ± 468.934  ops/s
HighConcurrentGenericMapRetrievalTest.synchronizedMap        thrpt    5    8.666 ±   0.701  ops/s
```

Github Actions (Ubuntu):
```
Benchmark                                                     Mode  Cnt    Score   Error  Units
HighConcurrentGenericMapRetrievalTest.concurrentHashMap      thrpt    5   93.518 ± 6.912  ops/s
HighConcurrentGenericMapRetrievalTest.syncMap                thrpt    5   53.486 ± 2.614  ops/s
HighConcurrentGenericMapRetrievalTest.synchronizedMap        thrpt    5   17.259 ± 1.975  ops/s
```

#### Read Fastutil

Calls `get(int)` on the `Map` for 1 million existing entries over 5 seconds.

Local (Windows):
```
Benchmark                                                     Mode  Cnt    Score   Error  Units
HighConcurrentPrimitiveMapRetrievalTest.syncMap              thrpt    5   60.768 ± 1.589  ops/s
HighConcurrentPrimitiveMapRetrievalTest.synchronizedMap      thrpt    5   12.924 ± 0.381  ops/s
```

Github Actions (Windows):
```
Benchmark                                                     Mode  Cnt    Score   Error  Units
HighConcurrentPrimitiveMapRetrievalTest.syncMap              thrpt    5   28.735 ± 1.782  ops/s
HighConcurrentPrimitiveMapRetrievalTest.synchronizedMap      thrpt    5    8.914 ± 0.755  ops/s
```

Github Actions (Ubuntu):
```
Benchmark                                                     Mode  Cnt    Score   Error  Units
HighConcurrentPrimitiveMapRetrievalTest.syncMap              thrpt    5   34.379 ± 4.455  ops/s
HighConcurrentPrimitiveMapRetrievalTest.synchronizedMap      thrpt    5   12.095 ± 4.484  ops/s
```

#### Write Generic

Calls `put(Object, Object)` on the `Map` for 1 million non-existent becoming existent
entries over 5 seconds.

Local (Windows):
```
Benchmark                                                     Mode  Cnt    Score   Error  Units
HighConcurrentGenericMapMutationTest.syncMap                 thrpt    5   25.057 ± 0.396  ops/s
HighConcurrentGenericMapMutationTest.synchronizedMap         thrpt    5   16.557 ± 0.101  ops/s
HighConcurrentGenericMapMutationTest.concurrentMap           thrpt    5   11.679 ± 0.911  ops/s
```

Github Actions (Windows):
```
Benchmark                                                     Mode  Cnt    Score   Error  Units
HighConcurrentGenericMapMutationTest.syncMap                 thrpt    5   16.171 ± 0.737  ops/s
HighConcurrentGenericMapMutationTest.synchronizedMap         thrpt    5    7.946 ± 0.619  ops/s
HighConcurrentGenericMapMutationTest.concurrentMap           thrpt    5    6.825 ± 0.580  ops/s
```

Github Actions (Ubuntu):
```
Benchmark                                                     Mode  Cnt    Score   Error  Units
HighConcurrentGenericMapMutationTest.synchronizedMap         thrpt    5   20.258 ± 2.271  ops/s
HighConcurrentGenericMapMutationTest.syncMap                 thrpt    5   18.213 ± 1.442  ops/s
HighConcurrentGenericMapMutationTest.concurrentMap           thrpt    5    8.199 ± 0.423  ops/s
```

#### Write Fastutil

Calls `put(int, Object)` on the `Map` for 1 million non-existent becoming existent
entries over 5 seconds.

Local (Windows):
```
Benchmark                                                     Mode  Cnt    Score   Error  Units
HighConcurrentPrimitiveMapMutationTest.syncMap               thrpt    5   26.921 ± 0.301  ops/s
HighConcurrentPrimitiveMapMutationTest.synchronizedMap       thrpt    5   18.763 ± 0.274  ops/s
```

Github Actions (Windows):
```
Benchmark                                                     Mode  Cnt    Score   Error  Units
HighConcurrentPrimitiveMapMutationTest.syncMap               thrpt    5   19.121 ± 1.429  ops/s
HighConcurrentPrimitiveMapMutationTest.synchronizedMap       thrpt    5    7.736 ± 1.889  ops/s
```

Github Actions (Ubuntu):
```
Benchmark                                                     Mode  Cnt    Score   Error  Units
HighConcurrentPrimitiveMapMutationTest.syncMap               thrpt    5   18.635 ± 2.093  ops/s
HighConcurrentPrimitiveMapMutationTest.synchronizedMap       thrpt    5   10.867 ± 3.541  ops/s
```

### No Contention

Testing involves no other threads, thus no locking involved.

#### Read Generic

Calls `get(Object)` on the `Map` for 1 million existing entries over 5 seconds.

Local (Windows):
```
Benchmark                                                 Mode  Cnt    Score   Error  Units
GenericMapRetrievalTest.concurrentHashMap                thrpt    5   78.350 ± 0.255  ops/s
GenericMapRetrievalTest.syncMap                          thrpt    5   70.225 ± 4.828  ops/s
GenericMapRetrievalTest.synchronizedMap                  thrpt    5   48.884 ± 0.329  ops/s
```

Github Actions (Windows):
```
Benchmark                                                 Mode  Cnt    Score   Error  Units
GenericMapRetrievalTest.concurrentHashMap                thrpt    5   40.626 ± 0.894  ops/s
GenericMapRetrievalTest.syncMap                          thrpt    5   24.101 ± 0.545  ops/s
GenericMapRetrievalTest.synchronizedMap                  thrpt    5   23.646 ± 0.703  ops/s
```

Github Actions (Ubuntu):
```
Benchmark                                                 Mode  Cnt    Score   Error  Units
GenericMapRetrievalTest.concurrentHashMap                thrpt    5   48.847 ± 1.552  ops/s
GenericMapRetrievalTest.synchronizedMap                  thrpt    5   28.675 ± 0.313  ops/s
GenericMapRetrievalTest.syncMap                          thrpt    5   28.052 ± 0.714  ops/s
```

#### Read Fastutil

Calls `get(int)` on the `Map` for 1 million existing entries over 5 seconds.

Local (Windows):
```
Benchmark                                                 Mode  Cnt    Score   Error  Units
PrimitiveMapRetrievalTest.synchronizedMap                thrpt    5   33.011 ± 0.145  ops/s
PrimitiveMapRetrievalTest.syncMap                        thrpt    5   20.566 ± 0.075  ops/s
```

Github Actions (Windows):
```
Benchmark                                                 Mode  Cnt    Score   Error  Units
PrimitiveMapRetrievalTest.synchronizedMap                thrpt    5   32.780 ± 3.096  ops/s
PrimitiveMapRetrievalTest.syncMap                        thrpt    5   13.637 ± 0.285  ops/s
```

Github Actions (Ubuntu):
```
Benchmark                                                 Mode  Cnt    Score   Error  Units
PrimitiveMapRetrievalTest.synchronizedMap                thrpt    5   41.454 ± 5.031  ops/s
PrimitiveMapRetrievalTest.syncMap                        thrpt    5   16.472 ± 1.137  ops/s
```

#### Write Generic

Calls `put(Object, Object)` on the `Map` for 1 million non-existent becoming existent
entries over 5 seconds.

Local (Windows):
```
Benchmark                                                 Mode  Cnt    Score   Error  Units
GenericMapMutationTest.synchronizedMap                   thrpt    5   54.033 ± 0.256  ops/s
GenericMapMutationTest.syncMap                           thrpt    5   48.341 ± 0.204  ops/s
GenericMapMutationTest.concurrentMap                     thrpt    5   41.216 ± 0.212  ops/s
```

Github Actions (Windows):
```
Benchmark                                                 Mode  Cnt    Score   Error  Units
GenericMapMutationTest.synchronizedMap                   thrpt    5   31.229 ± 1.913  ops/s
GenericMapMutationTest.syncMap                           thrpt    5   28.220 ± 0.838  ops/s
GenericMapMutationTest.concurrentMap                     thrpt    5   23.265 ± 1.068  ops/s
```

Github Actions (Ubuntu):
```
Benchmark                                                 Mode  Cnt    Score   Error  Units
GenericMapMutationTest.synchronizedMap                   thrpt    5   35.632 ± 1.591  ops/s
GenericMapMutationTest.syncMap                           thrpt    5   30.027 ± 1.599  ops/s
GenericMapMutationTest.concurrentMap                     thrpt    5   26.723 ± 0.667  ops/s
```

#### Write Fastutil

Calls `put(int, Object)` on the `Map` for 1 million non-existent becoming existent
entries over 5 seconds.

Local (Windows):
```
Benchmark                                                 Mode  Cnt    Score   Error  Units
PrimitiveMapMutationTest.synchronizedMap                 thrpt    5   78.835 ± 0.807  ops/s
PrimitiveMapMutationTest.syncMap                         thrpt    5   69.582 ± 0.748  ops/s
```

Github Actions (Windows):
```
Benchmark                                                 Mode  Cnt    Score   Error  Units
PrimitiveMapMutationTest.synchronizedMap                 thrpt    5   48.413 ± 1.180  ops/s
PrimitiveMapMutationTest.syncMap                         thrpt    5   43.728 ± 0.782  ops/s
```

Github Actions (Ubuntu):
```
Benchmark                                                 Mode  Cnt    Score   Error  Units
PrimitiveMapMutationTest.synchronizedMap                 thrpt    5   49.049 ± 3.541  ops/s
PrimitiveMapMutationTest.syncMap                         thrpt    5   47.010 ± 1.310  ops/s
```

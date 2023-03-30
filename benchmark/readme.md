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

### Only Dirty (5 threads)

These maps are not populated with values beforehand. `SyncMap` is at a
disadvantage by taking the slowest path (locking) the most.

```
Benchmark                                  (implementation)  (size)   Mode  Cnt     Score      Error  Units
DirtyGenericMapTest.randomRead                      SyncMap  100000  thrpt    5  2926.625 ±   23.576  ops/s
DirtyGenericMapTest.randomRead            ConcurrentHashMap  100000  thrpt    5  2787.740 ±   32.138  ops/s
DirtyGenericMapTest.randomRead              SynchronizedMap  100000  thrpt    5   403.330 ±   36.800  ops/s

DirtyGenericMapTest.randomReadAndWrite    ConcurrentHashMap  100000  thrpt    5   618.946 ±    7.933  ops/s
DirtyGenericMapTest.randomReadAndWrite              SyncMap  100000  thrpt    5   541.955 ±    5.410  ops/s
DirtyGenericMapTest.randomReadAndWrite      SynchronizedMap  100000  thrpt    5    61.987 ±    0.633  ops/s

DirtyGenericMapTest.randomWrite           ConcurrentHashMap  100000  thrpt    5   816.505 ±   23.983  ops/s
DirtyGenericMapTest.randomWrite                     SyncMap  100000  thrpt    5   740.277 ±   11.401  ops/s
DirtyGenericMapTest.randomWrite             SynchronizedMap  100000  thrpt    5    97.399 ±    1.864  ops/s

DirtyPrimitiveMapTest.randomRead                    SyncMap  100000  thrpt    5  9879.012 ± 2496.948  ops/s
DirtyPrimitiveMapTest.randomRead            SynchronizedMap  100000  thrpt    5   471.234 ±    8.487  ops/s

DirtyPrimitiveMapTest.randomReadAndWrite            SyncMap  100000  thrpt    5  1084.321 ±   26.096  ops/s
DirtyPrimitiveMapTest.randomReadAndWrite    SynchronizedMap  100000  thrpt    5   118.458 ±    4.836  ops/s

DirtyPrimitiveMapTest.randomWrite                   SyncMap  100000  thrpt    5  1232.352 ±   15.570  ops/s
DirtyPrimitiveMapTest.randomWrite           SynchronizedMap  100000  thrpt    5   171.266 ±    4.746  ops/s
```

### Only Read (5 threads)

These maps are populated with values beforehand. `SyncMap` will be taking
the fastest path (no locking) the most.

```
Benchmark                                  (implementation)  (size)   Mode  Cnt     Score      Error  Units
ReadGenericMapTest.randomRead             ConcurrentHashMap  100000  thrpt    5  1247.149 ±   21.514  ops/s
ReadGenericMapTest.randomRead                       SyncMap  100000  thrpt    5  1090.509 ±   10.417  ops/s
ReadGenericMapTest.randomRead               SynchronizedMap  100000  thrpt    5   117.445 ±    4.709  ops/s

ReadGenericMapTest.randomReadAndWrite     ConcurrentHashMap  100000  thrpt    5   627.768 ±   58.529  ops/s
ReadGenericMapTest.randomReadAndWrite               SyncMap  100000  thrpt    5   556.584 ±  227.972  ops/s
ReadGenericMapTest.randomReadAndWrite       SynchronizedMap  100000  thrpt    5    65.733 ±    4.445  ops/s

ReadGenericMapTest.randomWrite            ConcurrentHashMap  100000  thrpt    5   869.584 ±    9.220  ops/s
ReadGenericMapTest.randomWrite                      SyncMap  100000  thrpt    5   869.546 ±   10.004  ops/s
ReadGenericMapTest.randomWrite              SynchronizedMap  100000  thrpt    5    96.649 ±    1.600  ops/s

ReadPrimitiveMapTest.randomRead                     SyncMap  100000  thrpt    5  3045.319 ±   73.983  ops/s
ReadPrimitiveMapTest.randomRead             SynchronizedMap  100000  thrpt    5   323.276 ±    8.732  ops/s

ReadPrimitiveMapTest.randomReadAndWrite             SyncMap  100000  thrpt    5  1139.582 ±   16.770  ops/s
ReadPrimitiveMapTest.randomReadAndWrite     SynchronizedMap  100000  thrpt    5   119.114 ±    4.020  ops/s

ReadPrimitiveMapTest.randomWrite                    SyncMap  100000  thrpt    5  1231.071 ±   26.118  ops/s
ReadPrimitiveMapTest.randomWrite            SynchronizedMap  100000  thrpt    5   175.135 ±    3.720  ops/s
```

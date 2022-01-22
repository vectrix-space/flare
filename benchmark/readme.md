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
Benchmark                                  (implementation)  (size)   Mode  Cnt    Score   Error  Units
DirtyGenericMapTest.randomRead            ConcurrentHashMap  100000  thrpt    5  1092.846 ±   7.191  ops/s
DirtyGenericMapTest.randomRead                      SyncMap  100000  thrpt    5  1063.716 ±  19.922  ops/s
DirtyGenericMapTest.randomRead              SynchronizedMap  100000  thrpt    5   165.639 ±   8.541  ops/s

DirtyGenericMapTest.randomReadAndWrite    ConcurrentHashMap  100000  thrpt    5   205.462 ±   2.751  ops/s
DirtyGenericMapTest.randomReadAndWrite              SyncMap  100000  thrpt    5   165.743 ±  13.051  ops/s
DirtyGenericMapTest.randomReadAndWrite      SynchronizedMap  100000  thrpt    5    33.348 ±   3.187  ops/s

DirtyGenericMapTest.randomWrite           ConcurrentHashMap  100000  thrpt    5   270.430 ±   7.495  ops/s
DirtyGenericMapTest.randomWrite             SynchronizedMap  100000  thrpt    5    38.131 ±   4.955  ops/s
DirtyGenericMapTest.randomWrite                     SyncMap  100000  thrpt    5    36.057 ±   9.082  ops/s

DirtyPrimitiveMapTest.randomRead                    SyncMap  100000  thrpt    5  5136.393 ± 436.114  ops/s
DirtyPrimitiveMapTest.randomRead            SynchronizedMap  100000  thrpt    5   366.007 ±   1.716  ops/s

DirtyPrimitiveMapTest.randomReadAndWrite            SyncMap  100000  thrpt    5   466.924 ±  11.442  ops/s
DirtyPrimitiveMapTest.randomReadAndWrite    SynchronizedMap  100000  thrpt    5   106.553 ±  12.224  ops/s

DirtyPrimitiveMapTest.randomWrite           SynchronizedMap  100000  thrpt    5   137.213 ±  14.199  ops/s
DirtyPrimitiveMapTest.randomWrite                   SyncMap  100000  thrpt    5    57.604 ±  17.722  ops/s
```

### Only Read (5 threads)

These maps are populated with values beforehand. `SyncMap` will be taking
the fastest path (no locking) the most.

```
ReadGenericMapTest.randomRead             ConcurrentHashMap  100000  thrpt    5   331.616 ±  10.570  ops/s
ReadGenericMapTest.randomRead                       SyncMap  100000  thrpt    5   299.768 ±   4.700  ops/s
ReadGenericMapTest.randomRead               SynchronizedMap  100000  thrpt    5    43.201 ±   2.759  ops/s

ReadGenericMapTest.randomReadAndWrite     ConcurrentHashMap  100000  thrpt    5   212.580 ±   3.971  ops/s
ReadGenericMapTest.randomReadAndWrite               SyncMap  100000  thrpt    5   185.712 ±   4.690  ops/s
ReadGenericMapTest.randomReadAndWrite       SynchronizedMap  100000  thrpt    5    31.178 ±   2.074  ops/s

ReadGenericMapTest.randomWrite            ConcurrentHashMap  100000  thrpt    5   273.233 ±   6.057  ops/s
ReadGenericMapTest.randomWrite                      SyncMap  100000  thrpt    5   220.211 ±   7.689  ops/s
ReadGenericMapTest.randomWrite              SynchronizedMap  100000  thrpt    5    42.905 ±   3.634  ops/s

ReadPrimitiveMapTest.randomRead                     SyncMap  100000  thrpt    5   962.299 ±  15.143  ops/s
ReadPrimitiveMapTest.randomRead             SynchronizedMap  100000  thrpt    5   243.483 ±  12.702  ops/s

ReadPrimitiveMapTest.randomReadAndWrite             SyncMap  100000  thrpt    5   357.317 ±   8.721  ops/s
ReadPrimitiveMapTest.randomReadAndWrite     SynchronizedMap  100000  thrpt    5    83.976 ±  15.027  ops/s

ReadPrimitiveMapTest.randomWrite                    SyncMap  100000  thrpt    5   392.015 ±   8.591  ops/s
ReadPrimitiveMapTest.randomWrite            SynchronizedMap  100000  thrpt    5   138.869 ±  17.407  ops/s
```

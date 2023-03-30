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
Benchmark                                  (implementation)  (size)   Mode  Cnt      Score       Error  Units
DirtyGenericMapTest.randomRead                      SyncMap  100000  thrpt    5   2932.239 ±    23.499  ops/s
DirtyGenericMapTest.randomRead            ConcurrentHashMap  100000  thrpt    5   2794.828 ±    51.599  ops/s
DirtyGenericMapTest.randomRead              SynchronizedMap  100000  thrpt    5    404.373 ±    27.150  ops/s

DirtyGenericMapTest.randomReadAndWrite    ConcurrentHashMap  100000  thrpt    5    619.154 ±     2.262  ops/s
DirtyGenericMapTest.randomReadAndWrite              SyncMap  100000  thrpt    5    521.510 ±    30.843  ops/s
DirtyGenericMapTest.randomReadAndWrite      SynchronizedMap  100000  thrpt    5     60.726 ±     1.230  ops/s

DirtyGenericMapTest.randomWrite           ConcurrentHashMap  100000  thrpt    5    817.667 ±     5.822  ops/s
DirtyGenericMapTest.randomWrite                     SyncMap  100000  thrpt    5    717.097 ±     6.000  ops/s
DirtyGenericMapTest.randomWrite             SynchronizedMap  100000  thrpt    5     94.058 ±     1.768  ops/s

DirtyPrimitiveMapTest.randomRead                    SyncMap  100000  thrpt    5  10151.013 ±  2470.746  ops/s
DirtyPrimitiveMapTest.randomRead            SynchronizedMap  100000  thrpt    5    436.111 ±     7.576  ops/s

DirtyPrimitiveMapTest.randomReadAndWrite            SyncMap  100000  thrpt    5   1075.681 ±    21.117  ops/s
DirtyPrimitiveMapTest.randomReadAndWrite    SynchronizedMap  100000  thrpt    5    117.135 ±     7.941  ops/s

DirtyPrimitiveMapTest.randomWrite           SynchronizedMap  100000  thrpt    5    175.328 ±     3.977  ops/s
DirtyPrimitiveMapTest.randomWrite                   SyncMap  100000  thrpt    5    112.904 ±     1.829  ops/s
```

### Only Read (5 threads)

These maps are populated with values beforehand. `SyncMap` will be taking
the fastest path (no locking) the most.

```
ReadGenericMapTest.randomRead             ConcurrentHashMap  100000  thrpt    5   1230.149 ±   13.982  ops/s
ReadGenericMapTest.randomRead                       SyncMap  100000  thrpt    5   1020.432 ±   15.523  ops/s
ReadGenericMapTest.randomRead               SynchronizedMap  100000  thrpt    5    110.366 ±    1.975  ops/s

ReadGenericMapTest.randomReadAndWrite     ConcurrentHashMap  100000  thrpt    5    654.202 ±    7.295  ops/s
ReadGenericMapTest.randomReadAndWrite               SyncMap  100000  thrpt    5    588.168 ±    4.301  ops/s
ReadGenericMapTest.randomReadAndWrite       SynchronizedMap  100000  thrpt    5     60.217 ±    0.884  ops/s

ReadGenericMapTest.randomWrite            ConcurrentHashMap  100000  thrpt    5    864.681 ±   13.600  ops/s
ReadGenericMapTest.randomWrite                      SyncMap  100000  thrpt    5    857.699 ±   10.189  ops/s
ReadGenericMapTest.randomWrite              SynchronizedMap  100000  thrpt    5    100.739 ±    2.726  ops/s

ReadPrimitiveMapTest.randomRead                     SyncMap  100000  thrpt    5   2863.238 ±  131.310  ops/s
ReadPrimitiveMapTest.randomRead             SynchronizedMap  100000  thrpt    5    289.462 ±    8.202  ops/s

ReadPrimitiveMapTest.randomReadAndWrite             SyncMap  100000  thrpt    5   1050.175 ±   20.500  ops/s
ReadPrimitiveMapTest.randomReadAndWrite     SynchronizedMap  100000  thrpt    5    119.740 ±    1.248  ops/s

ReadPrimitiveMapTest.randomWrite                    SyncMap  100000  thrpt    5   1147.702 ±   10.950  ops/s
ReadPrimitiveMapTest.randomWrite            SynchronizedMap  100000  thrpt    5    179.187 ±    3.815  ops/s
```

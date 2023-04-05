# Summary

The summary of benchmarks taken for Flare.

## SyncMap

The scores are based on the time it takes to complete an iteration through 100,000 elements in the map. The lower
the score the faster the map is, the higher the score the slower it is.

### Forwarding

```text
Benchmark                                                             (implementation)  (size)    Mode     Cnt   Score    Error  Units

// Reads a populated map for each number in the sequence.
ForwardingSyncMapTest.read_sequential                                ConcurrentHashMap  100000  sample  438660   0.570 ±  0.001  ms/op
ForwardingSyncMapTest.read_sequential                                          SyncMap  100000  sample  297457   0.840 ±  0.001  ms/op
ForwardingSyncMapTest.read_sequential                                  SynchronizedMap  100000  sample   21291  11.737 ±  0.016  ms/op

// Writes to an empty map each number in the sequence.
ForwardingSyncMapTest.write_sequential                                 SynchronizedMap  100000  sample   42217   5.921 ±  0.024  ms/op
ForwardingSyncMapTest.write_sequential                                         SyncMap  100000  sample   34504   7.245 ±  0.030  ms/op
ForwardingSyncMapTest.write_sequential                               ConcurrentHashMap  100000  sample   34377   7.271 ±  0.028  ms/op

// Writes and reads to an empty map each number in the sequence.
ForwardingSyncMapTest.writeAndRead_sequential                          SynchronizedMap  100000  sample   34367   7.270 ±  0.028  ms/op
ForwardingSyncMapTest.writeAndRead_sequential                                  SyncMap  100000  sample   29368   8.504 ±  0.038  ms/op
ForwardingSyncMapTest.writeAndRead_sequential                        ConcurrentHashMap  100000  sample   29376   8.507 ±  0.030  ms/op

// Writes 25% of the time, reads 75% of the time, to a populated map each number in the sequence.
ForwardingSyncMapTest.writeAndMoreRead_sequential                    ConcurrentHashMap  100000  sample  258904   0.966 ±  0.001  ms/op
ForwardingSyncMapTest.writeAndMoreRead_sequential                              SyncMap  100000  sample  193326   1.293 ±  0.002  ms/op
ForwardingSyncMapTest.writeAndMoreRead_sequential                      SynchronizedMap  100000  sample   15840  15.774 ±  0.025  ms/op
```

### Primitive

```text
Benchmark                                                             (implementation)  (size)    Mode     Cnt   Score    Error  Units

// Reads a populated map for each number in the sequence.
PrimitiveSyncMapTest.read_sequential                                           SyncMap  100000  sample  183551   1.361 ±  0.002  ms/op
PrimitiveSyncMapTest.read_sequential                                   SynchronizedMap  100000  sample   17397  14.367 ±  0.019  ms/op

// Writes to an empty map each number in the sequence.
PrimitiveSyncMapTest.write_sequential                                  SynchronizedMap  100000  sample   50255   4.975 ±  0.011  ms/op
PrimitiveSyncMapTest.write_sequential                                          SyncMap  100000  sample   40535   6.166 ±  0.017  ms/op

// Writes and reads to an empty map each number in the sequence.
PrimitiveSyncMapTest.writeAndRead_sequential                           SynchronizedMap  100000  sample   45216   5.530 ±  0.013  ms/op
PrimitiveSyncMapTest.writeAndRead_sequential                                   SyncMap  100000  sample   35763   6.989 ±  0.019  ms/op

// Writes 25% of the time, reads 75% of the time, to a populated map each number in the sequence.
PrimitiveSyncMapTest.writeAndMoreRead_sequential                               SyncMap  100000  sample  133358   1.875 ±  0.003  ms/op
PrimitiveSyncMapTest.writeAndMoreRead_sequential                       SynchronizedMap  100000  sample   13833  18.064 ±  0.031  ms/op
```


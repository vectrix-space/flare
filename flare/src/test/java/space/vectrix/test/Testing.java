/*
 * This file is part of flare, licensed under the MIT License (MIT).
 *
 * Copyright (c) vectrix.space <https://vectrix.space/>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package space.vectrix.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class Testing {
  // From https://github.com/junit-team/junit4/wiki/Multithreaded-code-and-concurrency
  public static void assertConcurrent(final String message, final List<? extends Runnable> tasks, final int maximumTimeout) throws InterruptedException {
    final int numThreads = tasks.size();
    final List<Throwable> exceptions = Collections.synchronizedList(new ArrayList<>());
    final ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
    try {
      final CountDownLatch allExecutorThreadsReady = new CountDownLatch(numThreads);
      final CountDownLatch afterInitBlocker = new CountDownLatch(1);
      final CountDownLatch allDone = new CountDownLatch(numThreads);
      for(final Runnable submittedTestRunnable : tasks) {
        threadPool.submit(() -> {
          allExecutorThreadsReady.countDown();
          try {
            afterInitBlocker.await();
            submittedTestRunnable.run();
          } catch(final Throwable e) {
            exceptions.add(e);
          } finally {
            allDone.countDown();
          }
        });
      }
      assertTrue(allExecutorThreadsReady.await(tasks.size() * 10L, TimeUnit.MILLISECONDS), "Timeout initializing threads! Perform long lasting initializations before passing runnables to assertConcurrent");
      afterInitBlocker.countDown();
      assertTrue(allDone.await(maximumTimeout, TimeUnit.SECONDS), message + " timeout! More than" + maximumTimeout + "seconds");
    } finally {
      threadPool.shutdownNow();
    }
    assertTrue(exceptions.isEmpty(), message + "failed with exception(s)" + exceptions);
  }
}

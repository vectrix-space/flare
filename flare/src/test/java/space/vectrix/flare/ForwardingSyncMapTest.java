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
package space.vectrix.flare;

import com.google.common.collect.Lists;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import net.jodah.concurrentunit.Waiter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.junit.jupiter.api.Test;
import space.vectrix.test.TestHelper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ForwardingSyncMapTest extends AbstractMapTest<String, String> {
  @Override
  protected @NonNull Map<String, String> createMap() {
    return ForwardingSyncMap.hashmap();
  }

  @Override
  protected @NonNull Map<String, String> populate(final @NonNull Map<String, String> map, final int entries) {
    for(int i = 0; i < entries; i++) {
      map.put(this.key(i), this.value(i));
    }
    return map;
  }

  @Override
  protected Map.@NonNull Entry<String, String> entry(final int index) {
    return new AbstractMap.SimpleImmutableEntry<>(String.valueOf(index), String.valueOf(index));
  }

  @Override
  protected String key(final int index) {
    return String.valueOf(index);
  }

  @Override
  protected String value(final int index) {
    return String.valueOf(index);
  }

  // Put

  @Test
  public void testPutNullKey() {
    final Map<String, String> map = this.createMap();
    assertNull(map.put(null, this.value(0)));
  }

  @Test
  public void testPutNullValue() {
    final Map<String, String> map = this.createMap();
    assertThrows(NullPointerException.class, () -> map.put(this.key(0), null));
  }

  @Test
  public void testPutRead() {
    final Map<String, String> map = this.populate(this.createMap(), 3);
    assertEquals(this.value(0), map.put(this.key(0), this.value(3)), "Map should return the value at index 0, when putting a value for the key at index 0.");
    for(int i = 0; i < 10; i++) { // Read multiple times in order to promote dirty to read map.
      assertEquals(this.value(3), map.get(this.key(0)), "Map should return the value at index 3, when retrieving the key at index 0.");
    }
    assertEquals(this.value(3), map.put(this.key(0), this.value(2)), "Map should return the value at index 3, when putting a value for the key at index 0.");
    assertEquals(this.value(2), map.get(this.key(0)), "Map should return the value at index 2, when retrieving the key at index 0.");
    assertNull(map.put(this.key(3), this.value(3)), "Map should return null when putting a new entry.");
    assertEquals(this.value(3), map.get(this.key(3)), "Map should return the value at index 3, when retrieving the key at index 3.");
  }

  // Put If Absent

  @Test
  public void testPutIfAbsentNullValue() {
    final Map<String, String> map = this.createMap();
    assertThrows(NullPointerException.class, () -> map.putIfAbsent(this.key(0), null));
  }

  @Test
  public void testPutIfAbsentRead() {
    final Map<String, String> map = this.createMap();
    assertNull(map.putIfAbsent(this.key(0), this.value(0)), "Map should return null when no previous value is present for the key at index 0.");
    for(int i = 0; i < 10; i++) { // Read multiple times in order to promote dirty to read map.
      assertEquals(this.value(0), map.get(this.key(0)), "Map should return the value at index 0 when no mutation of the key at index 0 should have occurred.");
    }
    assertEquals(this.value(0), map.putIfAbsent(this.key(0), this.value(2)), "Map should return the value at index 0, when attempting to put the key at index 0.");
    assertEquals(this.value(0), map.get(this.key(0)), "Map should return the value at index 0 when no mutation of the key at index 0 should have occurred.");
  }

  // Compute If Absent

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testComputeIfAbsentNullFunction() {
    final Map<String, String> map = this.createMap();
    assertThrows(NullPointerException.class, () -> map.computeIfAbsent(this.key(0), null));
  }

  @Test
  public void testComputeIfAbsent() {
    final Map<String, String> map = this.createMap();
    assertEquals(this.value(0), map.computeIfAbsent(this.key(0), ignored -> this.value(0)), "Map should return the computed value at index 0.");
    for(int i = 0; i < 10; i++) {
      assertEquals(this.value(0), map.get(this.key(0)), "Map should return the value at index 0 when no mutation of the key at index 0 should have occurred.");
    }
    assertEquals(this.value(0), map.computeIfAbsent(this.key(0), ignored -> this.value(2)), "Map should return the value at index 0, when attempting to computeIfAbsent the key at index 0.");
    assertEquals(this.value(0), map.get(this.key(0)), "Map should return the value at index 0 when no mutation of the key at index 0 should have occurred.");
  }

  // Compute If Present

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testComputeIfPresentNullFunction() {
    final Map<String, String> map = this.createMap();
    assertThrows(NullPointerException.class, () -> map.computeIfPresent(this.key(0), null));
  }

  @Test
  public void testComputeIfPresent() {
    final Map<String, String> map = this.createMap();
    assertNull(map.computeIfPresent(this.key(0), (ignoredKey, ignoredValue) -> this.value(0)), "Map should return the null value at index 0.");
    map.put(this.key(0), this.value(0));
    for(int i = 0; i < 10; i++) {
      assertEquals(this.value(0), map.get(this.key(0)), "Map should return the value at index 0 when no mutation of the key at index 0 should have occurred.");
    }
    assertEquals(this.value(2), map.computeIfPresent(this.key(0), (ignoredKey, ignoredValue) -> this.value(2)), "Map should return the value at index 2, when attempting to computeIfPresent the key at index 0.");
    assertEquals(this.value(2), map.get(this.key(0)), "Map should return the value at index 2 when mutation of the key at index 0 should have occurred.");
    assertNull(map.computeIfPresent(this.key(0), (ignoredKey, ignoredValue) -> null), "Map should return null when mutation  of the key at index 0 should have occurred.");
    assertNull(map.get(this.key(0)), "Map should return null for key at index 0.");
  }

  // Compute

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testComputeNullFunction() {
    final Map<String, String> map = this.createMap();
    assertThrows(NullPointerException.class, () -> map.compute(this.key(0), null));
  }

  @Test
  public void testCompute() {
    final Map<String, String> map = this.createMap();
    assertEquals(this.value(0), map.compute(this.key(0), (ignoredKey, ignoredValue) -> this.value(0)), "Map should return the computed value at index 0.");
    for(int i = 0; i < 10; i++) {
      assertEquals(this.value(0), map.get(this.key(0)), "Map should return the value at index 0 when no mutation of the key at index 0 should have occurred.");
    }
    assertEquals(this.value(2), map.compute(this.key(0), (ignoredKey, ignoredValue) -> this.value(2)), "Map should return the value at index 0, when attempting to computeIfAbsent the key at index 0.");
    assertEquals(this.value(2), map.get(this.key(0)), "Map should return the value at index 0 when mutation of the key at index 0 should have occurred.");
    assertNull(map.compute(this.key(0), (ignoredKey, ignoredValue) -> null), "Map should return null when mutation  of the key at index 0 should have occurred.");
    assertNull(map.get(this.key(0)), "Map should return null for key at index 0.");
  }

  // Replace

  @Test
  public void testReplaceNullValue() {
    final Map<String, String> map = this.createMap();
    assertThrows(NullPointerException.class, () -> map.replace(this.key(0), null));
  }

  @Test
  public void testReplaceRead() {
    final Map<String, String> map = this.populate(this.createMap(), 3);
    for(int i = 0; i < 10; i++) { // Read multiple times in order to promote dirty to read map.
      assertEquals(this.value(0), map.get(this.key(0)), "Map should return the value at index 0 when no mutation of the key at index 0 should have occurred.");
    }
    assertEquals(this.value(0), map.replace(this.key(0), this.value(3)), "Map should return the previous value associated with the key at index 0.");
    assertEquals(this.value(3), map.get(this.key(0)), "Map should return the new value associated with the key at index 0.");
  }

  // Replace Entry

  @Test
  public void testReplaceEntryNullValue1() {
    final Map<String, String> map = this.createMap();
    assertThrows(NullPointerException.class, () -> map.replace(this.key(0), null, this.value(0)));
  }

  @Test
  public void testReplaceEntryNullValue2() {
    final Map<String, String> map = this.createMap();
    assertThrows(NullPointerException.class, () -> map.replace(this.key(0), this.value(0), null));
  }

  @Test
  public void testReplaceEntryRead() {
    final Map<String, String> map = this.populate(this.createMap(), 3);
    for(int i = 0; i < 10; i++) { // Read multiple times in order to promote dirty to read map.
      assertEquals(this.value(0), map.get(this.key(0)), "Map should return the value at index 0 when no mutation of the key at index 0 should have occurred.");
    }
    assertFalse(map.replace(this.key(0), this.value(1), this.value(2)), "Map should not be able to replace the key at index 0, with the old value not matching the actual stored value.");
    assertTrue(map.replace(this.key(0), this.value(0), this.value(2)), "Map should be able to replace the key at index 0, with the old value matching the actual stored value.");
    assertEquals(this.value(2), map.get(this.key(0)), "Map should return the new value associated with the key at index 0.");
  }

  // For Each

  @Test
  public void testForEach() {
    final Map<String, String> map = this.populate(this.createMap(), 5);
    final AtomicInteger counter = new AtomicInteger();
    map.forEach((key, value) -> {
      final String original = String.valueOf(counter.getAndIncrement());
      assertEquals(original, key, "Map should return " + original + ".");
    });
  }

  // Put All

  @Test
  public void testPutAll() {
    final Map<String, String> first = this.populate(this.createMap(), 0);
    final Map<String, String> other = this.populate(this.createMap(), 5);
    first.putAll(other);
    for(int i = 0; i < 5; i++) {
      final String original = String.valueOf(i);
      assertEquals(original, first.get(original), "Map should return " + original + ".");
    }
  }

  // Replace All

  @Test
  public void testReplaceAll() {
    final Map<String, String> map = this.populate(this.createMap(), 5);
    final AtomicInteger offset = new AtomicInteger(10);
    map.replaceAll((key, value) -> String.valueOf(offset.getAndIncrement()));
    for(int i = 0; i < 5; i++) {
      final String originalKey = String.valueOf(i);
      final String offsetKey = String.valueOf(i + 10);
      assertEquals(offsetKey, map.get(originalKey), "Map should return " + offsetKey + ".");
    }
  }

  // Entry Set

  @Test
  public void testEntriesAdd() {
    final Map<String, String> map = this.populate(this.createMap(), 3);
    final Set<Map.Entry<String, String>> keys = map.entrySet();
    assertFalse(keys.add(this.entry(0)), "Map should return false for attempting to store the entry at index 0.");
    assertTrue(keys.add(this.entry(3)), "Map should return true for storing the entry at index 3.");
    assertTrue(keys.contains(this.entry(3)), "Map should return true for containing the entry at index 3.");
  }

  @Test
  public void testEntriesAddAll() {
    final Map<String, String> first = this.populate(this.createMap(), 0);
    final Map<String, String> other = this.populate(this.createMap(), 5);
    final Set<Map.Entry<String, String>> keys = first.entrySet();
    keys.addAll(other.entrySet());
    for(int i = 0; i < 5; i++) {
      final String original = String.valueOf(i);
      assertEquals(original, first.get(original), "Map should return " + original + ".");
    }
  }

  // Key Set

  @Test
  public void testKeysAddUnsupported() {
    final Map<String, String> map = this.populate(this.createMap(), 3);
    final Set<String> keys = map.keySet();
    assertThrows(UnsupportedOperationException.class, () -> keys.add(this.key(0)));
  }

  @Test
  public void testKeysAddAllUnsupported() {
    final Map<String, String> map = this.populate(this.createMap(), 3);
    final Set<String> keys = map.keySet();
    assertThrows(UnsupportedOperationException.class, () -> keys.addAll(Lists.newArrayList(this.key(0), this.key(1))));
  }

  // Value Collection

  @Test
  public void testValuesAddUnsupported() {
    final Map<String, String> map = this.populate(this.createMap(), 3);
    final Collection<String> values = map.values();
    assertThrows(UnsupportedOperationException.class, () -> values.add(this.value(0)));
  }

  @Test
  public void testValuesAddAllUnsupported() {
    final Map<String, String> map = this.populate(this.createMap(), 3);
    final Collection<String> values = map.values();
    assertThrows(UnsupportedOperationException.class, () -> values.addAll(Lists.newArrayList(this.value(0), this.value(1))));
  }

  // Expunging

  @Test
  public void testExpungeRead() {
    final Map<String, String> map = this.populate(this.createMap(), 3);
    final int initialSize = map.size();
    for(int i = 0; i < 10; i++) { // Read multiple times in order to promote dirty to read map.
      assertEquals(this.value(0), map.get(this.key(0)), "Map should return the value at index 0, when retrieving the key at index 0.");
    }
    assertEquals(this.value(0), map.remove(this.key(0)), "Map should return the value at index 0, when removing the entry for the key at index 0.");
    assertEquals(initialSize - 1, map.size(), "Map should return a size with 1 less when removing an entry.");
    assertNull(map.get(this.key(0)), "Map should return null when removing the entry.");
    assertNull(map.remove(this.key(0)), "Map should return null when removing the entry at index 0 with no value present.");
    for(int i = 0; i < 10; i++) { // Read multiple times in order to promote dirty to read map.
      assertEquals(this.value(2), map.get(this.key(2)), "Map should return the value at index 2, when retrieving the key at index 2.");
    }
    assertNull(map.get(this.key(0)), "Map should return null after the entry is removed.");
  }

  @Test
  public void testUnexpungeRead() {
    final Map<String, String> map = this.populate(this.createMap(), 3);
    final int initialSize = map.size();
    for(int i = 0; i < 10; i++) { // Read multiple times in order to promote dirty to read map.
      assertEquals(this.value(0), map.get(this.key(0)), "Map should return the value at index 0, when retrieving the key at index 0.");
    }
    assertEquals(this.value(0), map.remove(this.key(0)), "Map should return the value at index 0, when removing the entry for the key at index 0.");
    assertEquals(initialSize - 1, map.size(), "Map should return a size with 1 less when removing an entry.");
    assertNull(map.get(this.key(0)), "Map should return null when removing the entry.");
    assertNull(map.remove(this.key(0)), "Map should return null when removing the entry at index 0 with no value present.");
    assertNull(map.get(this.key(0)), "Map should return null when getting the entry at index 0 with no value present.");
    assertNull(map.put(this.key(0), this.value(0)), "Map should return null when putting the entry into the map.");
    assertEquals(this.value(0), map.get(this.key(0)), "Map should return the value at index 0 for the key at index 0.");
    for(int i = 0; i < 10; i++) { // Read multiple times in order to promote dirty to read map.
      assertEquals(this.value(2), map.get(this.key(2)), "Map should return the value at index 2, when retrieving the key at index 2.");
    }
    assertEquals(this.value(0), map.get(this.key(0)), "Map should return the value at index 0 for the key at index 0.");
  }

  // Concurrent

  @Test
  public void testConcurrentPutRemove() throws Throwable {
    final Map<Integer, Boolean> map = ForwardingSyncMap.hashmap();
    final Waiter waiter = new Waiter();
    final AtomicInteger counter = new AtomicInteger();

    final int threadCount = 50;
    TestHelper.threadedRun(threadCount, () -> {
      try {
        final Random shouldPut = new Random();
        for(int i = 0; i < 1_000_000; i++) {
          final int value = counter.get();
          if(shouldPut.nextBoolean()) {
            map.put(value, Boolean.TRUE);
          } else {
            map.remove(value);
          }
        }
      } catch (final Exception exception) {
        waiter.fail(exception);
      }

      waiter.resume();
    });

    waiter.await(100_000, threadCount);
  }

  @Test
  public void testConcurrentIterateRemove() throws Throwable {
    final Map<Integer, Boolean> map = ForwardingSyncMap.hashmap();
    for(int i = 0; i < 1_000_000; i++) {
      map.put(i, Boolean.TRUE);
    }

    final Waiter waiter = new Waiter();

    final int threadCount = 50;
    TestHelper.threadedRun(threadCount, () -> {
      try {
        final Iterator<Boolean> iterator = map.values().iterator();
        while(iterator.hasNext()) {
          iterator.next();
          iterator.remove();
        }
      } catch (final Exception exception) {
        waiter.fail(exception);
      }

      waiter.resume();
    });

    waiter.await(100_000, threadCount);
  }
}

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

import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public abstract class AbstractMapTest<K, V> {
  /**
   * Creates a basic {@link Map} to use in a test.
   *
   * @return the basic map
   * @since 0.2.0
   */
  protected abstract @NonNull Map<K, V> createMap();

  /**
   * Populates the specified {@link Map} with the specified amount
   * of {@code int} entries.
   *
   * @param map the map
   * @param entries the amount of entries
   * @return the map
   * @since 0.2.0
   */
  protected abstract @NonNull Map<K, V> populate(final @NonNull Map<K, V> map, final int entries);

  /**
   * Returns the {@link Map.Entry} at the specified {@code index}.
   *
   * @param index the entry index
   * @return the map entry
   * @since 0.2.0
   */
  protected abstract Map.@NonNull Entry<K, V> entry(final int index);

  /**
   * Returns the {@code K} key at the specified {@code index}.
   *
   * @param index the index
   * @return the key
   * @since 0.2.0
   */
  protected abstract K key(final int index);

  /**
   * Returns the {@code V} value at the specified {@code index}.
   *
   * @param index the index
   * @return the value
   * @since 0.2.0
   */
  protected abstract V value(final int index);

  // Size

  @Test
  public void testSize() {
    final Map<K, V> map = this.populate(this.createMap(), 3);
    assertEquals(3, map.size(), "Map should be of size 3.");
  }

  // Empty

  @Test
  public void testEmpty1() {
    final Map<K, V> full = this.populate(this.createMap(), 3);
    assertFalse(full.isEmpty(), "Map should not be empty.");
  }

  @Test
  public void testEmpty2() {
    final Map<K, V> empty = this.populate(this.createMap(), 0);
    assertTrue(empty.isEmpty(), "Map should be empty.");
  }

  // Contains Value

  @Test
  public void testContainsValue() {
    final Map<K, V> map = this.populate(this.createMap(), 3);
    assertTrue(map.containsValue(this.value(0)), "Map should contain the value at index 0.");
    assertFalse(map.containsValue(this.value(3)), "Map should not contain the value at index 3.");
  }

  // Contains Key

  @Test
  public void testContainsKey() {
    final Map<K, V> map = this.populate(this.createMap(), 3);
    assertTrue(map.containsKey(this.key(0)), "Map should contain the key at index 0.");
    assertFalse(map.containsKey(this.key(3)), "Map should not contain the key at index 3.");
  }

  // Get

  @Test
  public void testGet() {
    final Map<K, V> map = this.populate(this.createMap(), 3);
    assertEquals(this.value(0), map.get(this.key(0)), "Map should return the value at index 0, when retrieving the key at index 0.");
    assertNull(map.get(this.key(3)), "Map should contain a null value for key at index 3.");
  }

  // Compute Absent

  @Test
  public void testComputeAbsent() {
    final Map<K, V> map = this.populate(this.createMap(), 3);
    assertEquals(this.value(0), map.computeIfAbsent(this.key(0), key -> this.value(3)), "Map should return the value at index 0, when computing if absent the key at index 0.");
    assertEquals(this.value(0), map.get(this.key(0)), "Map should return the value at index 0, when retrieving the key at index 0.");
    assertEquals(this.value(3), map.computeIfAbsent(this.key(3), key -> {
      assertEquals(this.key(3), key, "Map should input the key at index 3.");
      return this.value(3);
    }), "Map should return the computed value at index 3.");
    assertEquals(this.value(3), map.get(this.key(3)), "Map should return the value at index 3, when retrieving the key at index 3.");
    assertNull(map.computeIfAbsent(this.key(4), key -> {
      assertEquals(this.key(4), key, "Map should input the key at index 4.");
      return null;
    }), "Map should return the computed null value at index 4.");
  }

  // Compute Present

  @Test
  public void testComputePresent() {
    final Map<K, V> map = this.populate(this.createMap(), 3);
    assertEquals(this.value(3), map.computeIfPresent(this.key(0), (key, value) -> {
      assertEquals(this.key(0), key, "Map should input the key at index 0.");
      assertEquals(this.value(0), value, "Map should input the value at index 0.");
      return this.value(3);
    }), "Map should return the value at index 3, when computing if present the key at index 0.");
    assertEquals(this.value(3), map.get(this.key(0)), "Map should return the value at index 3, when retrieving the key at index 0.");
    assertNull(map.computeIfPresent(this.key(1), (key, value) -> {
      assertEquals(this.key(1), key, "Map should input the key at index 1.");
      assertEquals(this.value(1), value, "Map should input the value at index 1.");
      return null;
    }), "Map should return the computed null value at index 1.");
    assertFalse(map.containsKey(this.key(1)), "Map should no longer contain the value at index 1.");
    assertNull(map.computeIfPresent(this.key(3), (key, value) -> this.value(3)), "Map should return the computed null value at index 3.");
  }

  // Compute

  @Test
  public void testCompute() {
    final Map<K, V> map = this.populate(this.createMap(), 3);
    assertEquals(this.value(3), map.compute(this.key(0), (key, value) -> {
      assertEquals(this.key(0), key, "Map should input the key at index 0.");
      assertEquals(this.value(0), value, "Map should input the value at index 0.");
      return this.value(3);
    }), "Map should return the value at index 3, when computing if present the key at index 0.");
    assertEquals(this.value(3), map.get(this.key(0)), "Map should return the value at index 3, when retrieving the key at index 0.");
    assertNull(map.compute(this.key(1), (key, value) -> {
      assertEquals(this.key(1), key, "Map should input the key at index 1.");
      assertEquals(this.value(1), value, "Map should input the value at index 1.");
      return null;
    }), "Map should return the computed null value at index 1.");
    assertFalse(map.containsKey(this.key(1)), "Map should no longer contain the value at index 1.");
    assertEquals(this.value(3), map.compute(this.key(3), (key, value) -> this.value(3)), "Map should return the value at index 3, when computing the key at index 3.");
  }

  // Put If Absent

  @Test
  public void testPutAbsent() {
    final Map<K, V> map = this.populate(this.createMap(), 3);
    assertNull(map.putIfAbsent(this.key(3), this.value(3)), "Map should return null when no previous value is present for the key at index 3.");
    assertEquals(this.value(2), map.putIfAbsent(this.key(2), this.value(3)), "Map should return the value at index 2, when attempting to put the key at index 2.");
    assertEquals(this.value(2), map.get(this.key(2)), "Map should return the value at index 2 when no mutation of the key at index 2 should have occurred.");
  }

  // Put

  @Test
  public void testPut() {
    final Map<K, V> map = this.populate(this.createMap(), 3);
    assertEquals(this.value(0), map.put(this.key(0), this.value(3)), "Map should return the value at index 0, when putting a value for the key at index 0.");
    assertEquals(this.value(3), map.get(this.key(0)), "Map should return the value at index 3, when retrieving the key at index 0.");
    assertNull(map.put(this.key(3), this.value(3)), "Map should return null when putting a new entry.");
    assertEquals(this.value(3), map.get(this.key(3)), "Map should return the value at index 3, when retrieving the key at index 3.");
  }

  @Test
  public void testPutConcurrentEntryIteration() {
    final Map<K, V> map = this.populate(this.createMap(), 5);
    try {
      final Iterator<Map.Entry<K, V>> iterator = map.entrySet().iterator();
      map.put(this.key(8), this.value(8));
      iterator.next();
    } catch(final ConcurrentModificationException exception) {
      fail("Map should be able to put without mutating the iterator.");
    }
  }

  @Test
  public void testPutConcurrentKeyIteration() {
    final Map<K, V> map = this.populate(this.createMap(), 5);
    try {
      final Iterator<K> iterator = map.keySet().iterator();
      map.put(this.key(8), this.value(8));
      iterator.next();
    } catch(final ConcurrentModificationException exception) {
      fail("Map should be able to put without mutating the iterator.");
    }
  }

  @Test
  public void testPutConcurrentValueIteration() {
    final Map<K, V> map = this.populate(this.createMap(), 5);
    try {
      final Iterator<V> iterator = map.values().iterator();
      map.put(this.key(8), this.value(8));
      iterator.next();
    } catch(final ConcurrentModificationException exception) {
      fail("Map should be able to put without mutating the iterator.");
    }
  }

  // Remove

  @Test
  public void testRemove() {
    final Map<K, V> map = this.populate(this.createMap(), 3);
    final int initialSize = map.size();
    assertEquals(this.value(0), map.remove(this.key(0)), "Map should return the value at index 0, when removing the entry for the key at index 0.");
    assertEquals(initialSize - 1, map.size(), "Map should return a size with 1 less when removing an entry.");
    assertNull(map.get(this.key(0)), "Map should return null when removing the entry.");
    assertNull(map.remove(this.key(0)), "Map should return null when removing the entry at index 0 with no value present.");
  }

  @Test
  public void testRemoveConcurrentEntryIteration() {
    final Map<K, V> map = this.populate(this.createMap(), 5);
    try {
      final Iterator<Map.Entry<K, V>> iterator = map.entrySet().iterator();
      map.remove(this.key(0));
      iterator.next();
    } catch(final ConcurrentModificationException exception) {
      fail("Map should be able to clear without mutating the iterator.");
    }
  }

  @Test
  public void testRemoveConcurrentKeyIteration() {
    final Map<K, V> map = this.populate(this.createMap(), 5);
    try {
      final Iterator<K> iterator = map.keySet().iterator();
      map.remove(this.key(0));
      iterator.next();
    } catch(final ConcurrentModificationException exception) {
      fail("Map should be able to clear without mutating the iterator.");
    }
  }

  @Test
  public void testRemoveConcurrentValueIteration() {
    final Map<K, V> map = this.populate(this.createMap(), 5);
    try {
      final Iterator<V> iterator = map.values().iterator();
      map.remove(this.key(0));
      iterator.next();
    } catch(final ConcurrentModificationException exception) {
      fail("Map should be able to clear without mutating the iterator.");
    }
  }

  // Remove Entry

  @Test
  public void testRemoveEntry() {
    final Map<K, V> map = this.populate(this.createMap(), 3);
    final int initialSize = map.size();
    assertFalse(map.remove(this.key(0), this.value(1)), "Map should return false at index 0, when removing the entry for the key at index 0.");
    assertTrue(map.remove(this.key(0), this.value(0)), "Map should return true at index 0, when removing the entry for the key at index 0.");
    assertEquals(initialSize - 1, map.size(), "Map should return a size with 1 less when removing an entry.");
    assertNull(map.get(this.key(0)), "Map should return null when removing the entry.");
    assertFalse(map.remove(this.key(0), this.value(0)), "Map should return null when removing the entry at index 0 with no value present.");
  }

  @Test
  public void testRemoveEntryConcurrentEntryIteration() {
    final Map<K, V> map = this.populate(this.createMap(), 5);
    try {
      final Iterator<Map.Entry<K, V>> iterator = map.entrySet().iterator();
      map.remove(this.key(0), this.value(0));
      iterator.next();
    } catch(final ConcurrentModificationException exception) {
      fail("Map should be able to clear without mutating the iterator.");
    }
  }

  @Test
  public void testRemoveEntryConcurrentKeyIteration() {
    final Map<K, V> map = this.populate(this.createMap(), 5);
    try {
      final Iterator<K> iterator = map.keySet().iterator();
      map.remove(this.key(0), this.value(0));
      iterator.next();
    } catch(final ConcurrentModificationException exception) {
      fail("Map should be able to clear without mutating the iterator.");
    }
  }

  @Test
  public void testRemoveEntryConcurrentValueIteration() {
    final Map<K, V> map = this.populate(this.createMap(), 5);
    try {
      final Iterator<V> iterator = map.values().iterator();
      map.remove(this.key(0), this.value(0));
      iterator.next();
    } catch(final ConcurrentModificationException exception) {
      fail("Map should be able to clear without mutating the iterator.");
    }
  }

  // Replace

  @Test
  public void testReplace() {
    final Map<K, V> map = this.populate(this.createMap(), 3);
    assertEquals(this.value(0), map.replace(this.key(0), this.value(3)), "Map should return the previous value associated with the key at index 0.");
    assertEquals(this.value(3), map.get(this.key(0)), "Map should return the new value associated with the key at index 0.");
  }

  // Replace Entry

  @Test
  public void testReplaceEntry() {
    final Map<K, V> map = this.populate(this.createMap(), 3);
    assertFalse(map.replace(this.key(0), this.value(1), this.value(2)), "Map should not be able to replace the key at index 0, with the old value not matching the actual stored value.");
    assertTrue(map.replace(this.key(0), this.value(0), this.value(2)), "Map should be able to replace the key at index 0, with the old value matching the actual stored value.");
    assertEquals(this.value(2), map.get(this.key(0)), "Map should return the new value associated with the key at index 0.");
  }

  // Clear

  @Test
  public void testClear() {
    final Map<K, V> map = this.populate(this.createMap(), 5);
    map.clear();
    assertTrue(map.isEmpty(), "Map should be empty once cleared.");
    assertEquals(map.size(), 0);
    assertFalse(map.entrySet().iterator().hasNext());
  }

  @Test
  public void testClearConcurrentEntryIteration() {
    final Map<K, V> map = this.populate(this.createMap(), 5);
    try {
      final Iterator<Map.Entry<K, V>> iterator = map.entrySet().iterator();
      map.clear();
      iterator.next();
    } catch(final ConcurrentModificationException exception) {
      fail("Map should be able to clear without mutating the iterator.");
    }
  }

  @Test
  public void testClearConcurrentKeyIteration() {
    final Map<K, V> map = this.populate(this.createMap(), 5);
    try {
      final Iterator<K> iterator = map.keySet().iterator();
      map.clear();
      iterator.next();
    } catch(final ConcurrentModificationException exception) {
      fail("Map should be able to clear without mutating the iterator.");
    }
  }

  @Test
  public void testClearConcurrentValueIteration() {
    final Map<K, V> map = this.populate(this.createMap(), 5);
    try {
      final Iterator<V> iterator = map.values().iterator();
      map.clear();
      iterator.next();
    } catch(final ConcurrentModificationException exception) {
      fail("Map should be able to clear without mutating the iterator.");
    }
  }

  // Entry Set

  @Test
  public void testEntries() {
    final Map<K, V> map = this.populate(this.createMap(), 5);
    final Set<Map.Entry<K, V>> entries = map.entrySet();
    assertEquals(5, entries.size(), "Set size should be 5.");
    assertFalse(entries.isEmpty());
    assertTrue(entries.contains(this.entry(0)));
    assertFalse(entries.contains(this.entry(5)));
    assertTrue(entries.remove(this.entry(0)));
    assertFalse(entries.remove(this.entry(5)));
    assertFalse(entries.contains(this.entry(0)));
    assertEquals(4, entries.size());
  }

  // Key Set

  @Test
  public void testKeys() {
    final Map<K, V> map = this.populate(this.createMap(), 5);
    final Set<K> keys = map.keySet();
    assertEquals(5, keys.size(), "Set size should be 5.");
    assertFalse(keys.isEmpty());
    assertTrue(keys.contains(this.key(0)));
    assertFalse(keys.contains(this.key(5)));
    assertTrue(keys.remove(this.key(0)));
    assertFalse(keys.remove(this.key(5)));
    assertFalse(keys.contains(this.key(0)));
    assertEquals(4, keys.size());
  }

  // Value Collection

  @Test
  public void testValues() {
    final Map<K, V> map = this.populate(this.createMap(), 5);
    final Collection<V> values = map.values();
    assertEquals(5, values.size(), "Collection size should be 5.");
    assertFalse(values.isEmpty());
    assertTrue(values.contains(this.value(0)));
    assertFalse(values.contains(this.value(5)));
    assertTrue(values.remove(this.value(0)));
    assertFalse(values.remove(this.value(5)));
    assertFalse(values.contains(this.value(0)));
    assertEquals(4, values.size());
  }
}

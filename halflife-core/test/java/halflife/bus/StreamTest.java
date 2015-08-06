package halflife.bus;

import halflife.bus.concurrent.AVar;
import halflife.bus.concurrent.Atom;
import halflife.bus.key.Key;
import org.junit.Test;
import org.pcollections.TreePVector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class StreamTest extends AbstractStreamTest {

  @Test
  public void testMap() throws InterruptedException {
    AVar<Integer> res = new AVar<>();
    Stream<Integer> intStream = new Stream<>();

    intStream.map(Key.wrap("key1"), Key.wrap("key2"), (i) -> i + 1);
    intStream.consume(Key.wrap("key2"), res::set);

    intStream.notify(Key.wrap("key1"), 1);

    assertThat(res.get(1, TimeUnit.SECONDS), is(2));
  }

  @Test
  public void debounceTest() throws InterruptedException {
    AVar<Integer> res = new AVar<>();
    Stream<Integer> intStream = new Stream<>();


    intStream.map(Key.wrap("key1"), Key.wrap("key2"), (i) -> i + 1);
    intStream.debounce(Key.wrap("key2"), Key.wrap("key3"), 100, TimeUnit.MILLISECONDS);

    {
      CountDownLatch latch = new CountDownLatch(1);
      AtomicInteger counter = new AtomicInteger(0);
      intStream.consume(Key.wrap("key3"), (Integer i) -> {
        res.set(i);
        counter.incrementAndGet();
        latch.countDown();
      });

      intStream.notify(Key.wrap("key1"), 1);
      intStream.notify(Key.wrap("key1"), 2);
      intStream.notify(Key.wrap("key1"), 3);
      latch.await(1, TimeUnit.SECONDS);
      assertThat(counter.get(), is(1));
    }
    assertThat(res.get(1, TimeUnit.SECONDS), is(4));
  }

  @Test
  public void streamStateSupplierTest() throws InterruptedException {
    AVar<Integer> res = new AVar<>(3);
    Stream<Integer> intStream = new Stream<>();

    intStream.map(Key.wrap("key1"), Key.wrap("key2"), (i) -> i + 1);
    intStream.map(Key.wrap("key2"), Key.wrap("key3"), (Atom<Integer> state) -> {
                    return (i) -> {
                      return state.swap(old -> old + i);
                    };
                  },
                  0);
    intStream.consume(Key.wrap("key3"), res::set);

    intStream.notify(Key.wrap("key1"), 1);
    intStream.notify(Key.wrap("key1"), 2);
    intStream.notify(Key.wrap("key1"), 3);

    assertThat(res.get(1, TimeUnit.SECONDS), is(9));
  }

  @Test
  public void streamStateFnTest() throws InterruptedException {
    AVar<Integer> res = new AVar<>(3);
    Stream<Integer> intStream = new Stream<>();

    intStream.map(Key.wrap("key1"), Key.wrap("key2"), (i) -> i + 1);
    intStream.map(Key.wrap("key2"), Key.wrap("key3"), (Atom<Integer> state, Integer i) -> {
                    return state.swap(old -> old + i);
                  },
                  0);
    intStream.consume(Key.wrap("key3"), res::set);

    intStream.notify(Key.wrap("key1"), 1);
    intStream.notify(Key.wrap("key1"), 2);
    intStream.notify(Key.wrap("key1"), 3);

    assertThat(res.get(1, TimeUnit.SECONDS), is(9));
  }

  @Test
  public void streamFilterTest() throws InterruptedException {
    AVar<Integer> res = new AVar<>(2);
    Stream<Integer> intStream = new Stream<>();

    intStream.filter(Key.wrap("key1"), Key.wrap("key2"), (i) -> i % 2 == 0);
    intStream.consume(Key.wrap("key2"), res::set);

    intStream.notify(Key.wrap("key1"), 1);
    intStream.notify(Key.wrap("key1"), 2);
    intStream.notify(Key.wrap("key1"), 3);
    intStream.notify(Key.wrap("key1"), 4);
    assertThat(res.get(1, TimeUnit.SECONDS), is(4));
  }

  @Test
  public void divideTest() {
    List<Integer> even = new ArrayList<>();
    List<Integer> odd = new ArrayList<>();

    Key evenKey = Key.wrap("even");
    Key oddKey = Key.wrap("odd");
    Key numbersKey = Key.wrap("numbers");

    Stream<Integer> intStream = new Stream<>();
    intStream.divide(numbersKey,
                     (k, v) -> {
                       return v % 2 == 0 ? evenKey : oddKey;
                     });

    intStream.consume(evenKey, (Integer i) -> {
      even.add(i);
    });

    intStream.consume(oddKey, (Integer i) -> {
      odd.add(i);
    });

    for (int i = 0; i < 10; i++) {
      intStream.notify(numbersKey, i);
    }

    assertThat(even, is(Arrays.asList(0, 2, 4, 6, 8)));
    assertThat(odd, is(Arrays.asList(1, 3, 5, 7, 9)));
  }

  @Test
  public void partitionTest() throws InterruptedException {
    AVar<List<Integer>> res = new AVar<>();
    Stream<Integer> intStream = new Stream<>();

    intStream.partition(Key.wrap("key1"), Key.wrap("key2"), (i) -> {
      return i.size() == 5;
    });
    intStream.consume(Key.wrap("key2"), (List<Integer> a) -> {
      res.set(a);
    });

    intStream.notify(Key.wrap("key1"), 1);
    intStream.notify(Key.wrap("key1"), 2);
    intStream.notify(Key.wrap("key1"), 3);
    intStream.notify(Key.wrap("key1"), 4);
    intStream.notify(Key.wrap("key1"), 5);
    intStream.notify(Key.wrap("key1"), 6);
    intStream.notify(Key.wrap("key1"), 7);

    assertThat(res.get(1, TimeUnit.SECONDS), is(TreePVector.from(Arrays.asList(1, 2, 3, 4, 5))));
  }

  @Test
  public void slideTest() throws InterruptedException {
    AVar<List<Integer>> res = new AVar<>(6);
    Stream<Integer> intStream = new Stream<>();

    intStream.slide(Key.wrap("key1"), Key.wrap("key2"), (i) -> {
      return i.subList(i.size() > 5 ? i.size() - 5 : 0,
                       i.size());
    });
    intStream.consume(Key.wrap("key2"), (List<Integer> a) -> {
      res.set(a);
    });

    intStream.notify(Key.wrap("key1"), 1);
    intStream.notify(Key.wrap("key1"), 2);
    intStream.notify(Key.wrap("key1"), 3);
    intStream.notify(Key.wrap("key1"), 4);
    intStream.notify(Key.wrap("key1"), 5);
    intStream.notify(Key.wrap("key1"), 6);
    assertThat(res.get(1, TimeUnit.SECONDS), is(TreePVector.from(Arrays.asList(2, 3, 4, 5, 6))));
  }

  @Test
  public void testUnregister() throws InterruptedException {
    Key k1 = Key.wrap("key1");
    Key k2 = Key.wrap("key2");

    CountDownLatch latch = new CountDownLatch(1);
    Stream<Integer> intStream = new Stream<>();

    intStream.map(k1, k2, (i) -> i + 1);
    intStream.consume(k2, (i) -> latch.countDown());

    intStream.unregister(k2);
    intStream.notify(k1, 1);

    assertThat(latch.getCount(), is(1L));
  }

  @Test
  public void testUnregisterAll() throws InterruptedException {
    Key k1 = Key.wrap("key1");
    Key k2 = Key.wrap("key2");

    CountDownLatch latch = new CountDownLatch(2);
    Stream<Integer> intStream = new Stream<>();

    intStream.map(k1, k2, (i) -> {
      latch.countDown();
      return i + 1;
    });

    intStream.consume(k2, (i) -> latch.countDown());

    intStream.unregister((Predicate<Key>) key -> true);
    intStream.notify(k1, 1);

    assertThat(latch.getCount(), is(2L));
  }

  @Test
  public void testDispatchers() throws InterruptedException {
    Key k1 = Key.wrap("key1");
    Key k2 = Key.wrap("key2");

    CountDownLatch latch = new CountDownLatch(1);
    Stream<Integer> intStream = new Stream<>();

    intStream.fork(Executors.newFixedThreadPool(2),
                   2048)
             .map(k1, k2, (i) -> {
               try {
                 Thread.sleep(100);
               } catch (InterruptedException e) {
                 e.printStackTrace();
               }
               return i;
             });

    intStream.consume(k2, (i) -> latch.countDown());

    intStream.unregister((Predicate<Key>) key -> true);
    intStream.notify(k1, 1);

    assertThat(latch.getCount(), is(1L));
  }


}


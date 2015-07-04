package com.instana.processor;

import halflife.bus.Stream;
import halflife.bus.concurrent.AVar;
import halflife.bus.key.Key;
import halflife.bus.registry.KeyMissMatcher;
import org.junit.Test;

public class MatchedStreamText extends AbstractFirehoseTest {

  @Test
  public void mapTest() throws InterruptedException {
    Stream<Integer> stream = new Stream<>(firehose);
    AVar<Integer> res = new AVar<>();

    stream.map((KeyMissMatcher<Key>) key -> {
      return key.equals(Key.wrap("source"));
    }, (i) -> {
      System.out.printf("aaa: %d\n", i);
      return i + 1;
    }).map((i -> {
      System.out.printf("bbb: %d\n", i);
      return i * 2;
    }))
          .consume((i) -> {
            System.out.println(i);
          });


    firehose.notify(Key.wrap("source"), 1);
    //firehose.notify(Key.wrap("source"), 1);

    //assertThat(res.get(1, TimeUnit.SECONDS), is(4));
  }
}

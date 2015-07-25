package halflife.bus;

import halflife.bus.key.Key;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public class AnonymousStream<V> {

  private final Key    rootKey;
  private final Key    upstream;
  private final Stream stream;

  public AnonymousStream(Key upstream, Stream stream) {
    this.upstream = upstream;
    this.rootKey = upstream;
    this.stream = stream;
  }

  public AnonymousStream(Key rootKey, Key upstream, Stream stream) {
    this.upstream = upstream;
    this.stream = stream;
    this.rootKey = rootKey;
  }

  @SuppressWarnings(value = {"unchecked"})
  public <V1> AnonymousStream<V1> map(Function<V, V1> mapper) {
    Key downstream = upstream.derive();

    stream.map(upstream, downstream, mapper);

    return new AnonymousStream<>(rootKey, downstream, stream);
  }

  @SuppressWarnings(value = {"unchecked"})
  public AnonymousStream<V> filter(Predicate<V> predicate) {
    Key downstream = upstream.derive();

    stream.filter(upstream, downstream, predicate);

    return new AnonymousStream<>(rootKey, downstream, stream);
  }

  @SuppressWarnings(value = {"unchecked"})
  public AnonymousStream<List<V>> slide(UnaryOperator<List<V>> drop) {
    Key downstream = upstream.derive();

    stream.slide(upstream, downstream, drop);

    return new AnonymousStream<>(rootKey, downstream, stream);
  }

  @SuppressWarnings(value = {"unchecked"})
  public AnonymousStream<List<V>> partition(Predicate<List<V>> emit) {
    Key downstream = upstream.derive();

    stream.partition(upstream, downstream, emit);

    return new AnonymousStream<>(rootKey, downstream, stream);
  }

  @SuppressWarnings(value = {"unchecked"})
  public AnonymousStream<V> consume(Consumer<V> consumer) {
    stream.consume(upstream, consumer);
    return this;
  }

  @SuppressWarnings(value = {"unchecked"})
  public AnonymousStream<V> redirect(Key destination) {
    stream.consume(upstream, new KeyedConsumer<Key, V>() {
      @Override
      public void accept(Key key, V value) {
        stream.notify(destination, value);
      }
    });

    return this;
  }

  @SuppressWarnings(value = {"unchecked"})
  public Runnable cancellableConsumer(Consumer<V> consumer) {
    stream.consume(upstream, consumer);
    return () -> {
      stream.unregister(upstream);
    };
  }

  @SuppressWarnings(value = {"unchecked"})
  public void notify(V v) {
    this.stream.notify(upstream, v);
  }

  public void unregister() {
    this.stream.unregister(new Predicate<Key>() {
      @Override
      public boolean test(Key k) {
        return k.isDerivedFrom(rootKey) || k.equals(rootKey);
      }
    });
  }
}

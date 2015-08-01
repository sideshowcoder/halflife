package halflife.bus;

import halflife.bus.key.Key;
import halflife.bus.registry.ConcurrentRegistry;
import halflife.bus.registry.DefaultingRegistry;
import org.junit.After;
import org.junit.Before;
import reactor.Environment;
import reactor.fn.Consumer;

public class AbstractFirehoseTest {

  protected Firehose<Key>           firehose;
  protected Environment             environment;
  protected DefaultingRegistry<Key> consumerRegistry;
  protected Consumer<Throwable>     dispatchErrorHandler;

  @Before
  public void setup() {
    this.environment = new Environment();
    this.consumerRegistry = new ConcurrentRegistry<>();
    this.dispatchErrorHandler = throwable -> {
      System.out.println(throwable.getMessage());
      throwable.printStackTrace();
    };
    this.firehose = new Firehose<>(environment.getDispatcher("sync"),
                                   consumerRegistry,
                                   dispatchErrorHandler,
                                   null);
  }

  @After
  public void teardown() {
    this.environment.shutdown();
  }

}



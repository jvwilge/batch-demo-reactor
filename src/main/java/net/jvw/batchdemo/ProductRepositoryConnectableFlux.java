package net.jvw.batchdemo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class ProductRepositoryConnectableFlux {

  private static final Logger LOG = LoggerFactory.getLogger(ProductRepositoryConnectableFlux.class);

  private ConnectableFlux<Product> connectableFlux;
  private Disposable connectionToConnectableFlux;
  private AtomicReference<FluxSink<Long>> fluxSink = new AtomicReference<>();

  private ProductRepository repository;

  public ProductRepositoryConnectableFlux(ProductRepository repository) {
    this.repository = repository;
  }

  @PostConstruct
  public void init() {

    connectableFlux = Flux.<Long>create(fluxSink::set, FluxSink.OverflowStrategy.ERROR)
        .bufferTimeout(5, Duration.ofMillis(50))
        .doOnNext(ids -> LOG.debug("processing buffer of size {}", ids.size()))
        .concatMap(ids ->
            Mono.<List<Product>>create(sink -> {
              repository.getBatchAsync(ids, sink);
            })
        )
        .flatMap(Flux::fromIterable)
        .publish();

    // https://www.slideshare.net/Pivotal/reactive-programming-with-pivotals-reactor slide 20
    connectionToConnectableFlux = connectableFlux.connect(); // start pumping, with or without subscribers
    LOG.info("Connectable flux is connected");
    //    connectableFlux.subscribe(); // don't need a subscribe here since there will come a lot of short lived subsribers
  }

  @PreDestroy
  public void shutdown() {
    connectionToConnectableFlux.dispose();
  }

  public Mono<Product> get(Long id) {

    return connectableFlux
        .filter(product -> id.equals(product.getId())) // connectableFlux contains all results from batch, filter on id
        .next() // basically a take(1) that converts to mono : https://stackoverflow.com/questions/42021559/convert-from-flux-to-mono
        .doOnSubscribe(ignore -> {
          // send id to connectableFlux so it will be included in a batch
          // send id in onSubscribe or otherwise the result might arrive before the actual subscribe()
          fluxSink.get().next(id);
        });
  }

}




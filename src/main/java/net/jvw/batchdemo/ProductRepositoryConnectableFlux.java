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
    connectableFlux = Flux.<Long>create(fluxSink::set)
        .bufferTimeout(3, Duration.ofMillis(5000))
        .doOnNext(ids -> LOG.debug("processing buffer of size {}", ids.size()))
        .map(ids->repository.getBatch(ids))
        .concatMap(Flux::fromIterable)
        .doOnCancel(() -> LOG.debug("Ccancelling"))
        .doOnComplete(() -> LOG.debug("Ccomplete"))
        .doOnTerminate(() -> LOG.debug("Cterminate"))
        .doOnError(throwable -> LOG.error("Boem: ", throwable))
        .publish();

    // https://www.slideshare.net/Pivotal/reactive-programming-with-pivotals-reactor slide 20
    connectionToConnectableFlux = connectableFlux.connect(); // start pumping, with or without subscribers
    LOG.info("Connectable flux is connected");
    //    connectableFlux.subscribe(); //TODO probably not needed
  }

  @PreDestroy
  public void shutdown() {
    connectionToConnectableFlux.dispose();
  }

  public Mono<Product> get(Long id) {
    LOG.debug("sending {} to fluxSink", id);
    //subscribe first, subscription might arrive after id is sent
    final Mono<Product> result = Mono.<Product>create(productMonoSink -> {
      connectableFlux.filter(product -> id.equals(product.getId()))
          .take(1)
          .subscribe();
    });

    fluxSink.get().next(id);
    return result;
  }

}




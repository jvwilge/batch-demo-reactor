package net.jvw.batchdemo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class ProductRepositoryConnectableFlux {

  private static final Logger LOG = LoggerFactory.getLogger(ProductRepositoryConnectableFlux.class);

  private static final Scheduler PRCF_SCHEDULER = Schedulers.newBoundedElastic(1, 10_000, "prcf"); //only need 1 thread here

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
        .concatMap(ids -> { // don't need flatMap so don't use it to prevent exponential explosion of tasks

          return Mono.<List<Product>>create(
              sink -> repository.getBatchAsync(ids, sink)
          )
          ;

        })
//        .publishOn(PRCF_SCHEDULER) // 15-dec-19 @ 16:21 This one is needed to jump out of `I/O dispatcher` thread
        .flatMap(Flux::fromIterable, 1)
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


    final Mono<Product> productMono = connectableFlux
        .filter(product -> id.equals(product.getId())) // connectableFlux contains all results from batch, filter on id
        .next() // basically a take(1) that converts to mono : https://stackoverflow.com/questions/42021559/convert-from-flux-to-mono
        //        .doOnSubscribe(subscription -> LOG.debug("subscribing: {}", subscription))
        //        .doOnTerminate(() -> LOG.debug("terminate"))
        .doOnCancel(() -> LOG.debug("cancel"))
//                .subscribeOn(PRCF_SCHEDULER) // enabling this one creates timeout
        //                .doOnComplete(() -> LOG.debug("complete"))
        //        .doOnNext(product -> LOG.debug("taking shit: {}", product));
        //        .subscribe();// subscribe is nodig omdat dit een nested mono is
        ;

    //    LOG.debug("sending {} to fluxSink", id);

    fluxSink.get().next(id); // send id to connectableFlux so it will be included in a batch
    return productMono;
  }

}




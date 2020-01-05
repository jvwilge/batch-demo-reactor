package net.jvw.batchdemo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.Duration;

@RestController()
@RequestMapping("/product")
public class ProductHandler {

  private static final Logger LOG = LoggerFactory.getLogger(ProductHandler.class);

  private ProductRepository repository;

  private ProductRepositoryConnectableFlux repositoryConnectableFlux;

  public ProductHandler(ProductRepository repository, ProductRepositoryConnectableFlux repositoryConnectableFlux) {
    this.repository = repository;
    this.repositoryConnectableFlux = repositoryConnectableFlux;
  }

  @GetMapping
  public Mono<ServerResponse> hello(@RequestBody String requestBody) {
    return ServerResponse.ok().contentType(MediaType.TEXT_PLAIN)
        .body(BodyInserters.fromObject("Hello, Spring!"));
  }

  public Mono<Product> upsertClassic(@RequestBody ProductUpdate update) {
    //    return Mono.just(update.getId())
    //        .map(repository::get)
    //        .subscribeOn(NBE_SCHEDULER).doOnNext(product -> {
    //          LOG.trace("transforming product: {}", product);
    //          product.setPrice(Math.max(0, product.getPrice() + update.getPriceChange()));
    //          product.setStock(Math.max(0, product.getStock() + update.getStockChange()));
    //        })
    //        .publishOn(NBE_SCHEDULER) // als je deze weglaat blaast de boel direct op
    //        .doOnNext(product ->
    //            Mono.<Product>create(sink -> repository.updateAsync(product, sink)).subscribe())
    //        ;
    return Mono.empty();
  }

  @PutMapping
  public Mono<Product> upsert(@RequestBody ProductUpdate update) {

    LOG.debug("upsert");

    return repositoryConnectableFlux.get(update.getId())
//        .publishOn(NBE_SCHEDULER) // 15-dec-19 @ 16:58 -> otherwise prcf
        .timeout(Duration.ofMillis(5000))
        .doOnNext(product -> {
          LOG.trace("transforming product: {}", product);
          product.setPrice(Math.max(0, product.getPrice() + update.getPriceChange()));
          product.setStock(Math.max(0, product.getStock() + update.getStockChange()));
        })
        //        .publishOn(NBE_SCHEDULER) // als je deze weglaat blaast de boel direct op
        .flatMap(product ->
             Mono
                .<Product>create(sink -> {
                  repository.updateAsync(product, sink);
                })
//                .publishOn(NBE_SCHEDULER) // 15-dec-19 @ 16:52 jump back after update
        )
        .doOnNext(product -> {})
        ;
  }

}

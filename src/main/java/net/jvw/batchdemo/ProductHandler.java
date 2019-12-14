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

  @PutMapping
  public Mono<Product> upsert(@RequestBody ProductUpdate update) {

    boolean withConnectableFlux = true;

    Mono<Product> mono = null;

    if (withConnectableFlux) {
      mono = repositoryConnectableFlux.get(update.getId()).timeout(Duration.ofMillis(5000));
    } else {
      mono = Mono.just(update.getId())
          .map(repository::get)
          .subscribeOn(ProductRepositoryConnectableFlux.SCHEDULER)
      ;
    }

    return mono.doOnNext(product -> {
      LOG.trace("transforming product: {}", product);
      product.setPrice(Math.max(0, product.getPrice() + update.getPriceChange()));
      product.setStock(Math.max(0, product.getStock() + update.getStockChange()));
    })
        .publishOn(ProductRepositoryConnectableFlux.SCHEDULER) // als je deze weglaat blaast de boel direct op
        .doOnNext(product ->
            Mono.<Product>create(sink -> repository.updateAsync(product, sink)))
        ;
  }

}

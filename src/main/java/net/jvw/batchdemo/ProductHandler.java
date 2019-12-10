package net.jvw.batchdemo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@RestController()
@RequestMapping("/product")
public class ProductHandler {

  private ProductRepository repository;

  private ProductRepositoryConnectableFlux repositoryConnectableFlux;

  private ObjectMapper mapper;

  public ProductHandler(ProductRepository repository, ProductRepositoryConnectableFlux repositoryConnectableFlux, ObjectMapper mapper) {
    this.repository = repository;
    this.repositoryConnectableFlux = repositoryConnectableFlux;
    this.mapper = mapper;
  }

  @GetMapping
  public Mono<ServerResponse> hello(@RequestBody String requestBody) {
    return ServerResponse.ok().contentType(MediaType.TEXT_PLAIN)
        .body(BodyInserters.fromObject("Hello, Spring!"));
  }

  @PutMapping
  public Mono<Product> upsert(@RequestBody ProductUpdate update) {
    return
        //        Mono.just(update.getId())
        //        .map(repository::get)
        repositoryConnectableFlux.get(update.getId())
            .doOnNext(product -> {
              product.setPrice(Math.max(0, product.getPrice() + update.getPriceChange()));
              product.setStock(Math.max(0, product.getStock() + update.getStockChange()));
            })
            .doOnNext(repository::insert);
  }


}

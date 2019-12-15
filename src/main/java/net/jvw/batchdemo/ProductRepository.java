package net.jvw.batchdemo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpHost;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetRequest;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Requests;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.MonoSink;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Repository
public class ProductRepository {

  private static final Logger LOG = LoggerFactory.getLogger(ProductRepository.class);

  private static final String INDEX_NAME = "product";
  private static final String DOC_TYPE = "_doc";
  private RestHighLevelClient client;

  private ObjectMapper mapper;

  public ProductRepository(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @PostConstruct
  public void init() {
    final RestClientBuilder builder = RestClient.builder(new HttpHost("localhost", 9200, "http"));
    client = new RestHighLevelClient(builder);
  }

  @PreDestroy
  public void shutdown() throws IOException {
    client.close();
  }

  //TODO get with optimistic locking
  public Product get(long id) throws RuntimeException {
    try {
      LOG.debug("getting id {}", id);
      GetRequest request = Requests.getRequest(INDEX_NAME).id("" + id);
      final GetResponse response = client.get(request, RequestOptions.DEFAULT);
      return mapper.readValue(response.getSourceAsBytes(), Product.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void getBatchAsync(List<Long> ids, MonoSink<List<Product>> monoSink) throws RuntimeException {
    LOG.debug("getting async batch: {}", ids);
    try {
      MultiGetRequest multiGetRequest = new MultiGetRequest();
      ids.forEach(id -> multiGetRequest.add(INDEX_NAME, DOC_TYPE, "" + id));

      ActionListener<MultiGetResponse> actionListener = new ActionListener<MultiGetResponse>() {
        @Override
        public void onResponse(MultiGetResponse multiGetItemResponses) {
          final List<Product> result = Arrays.stream(multiGetItemResponses.getResponses()).map(multiGetItemResponse -> {

            try {
              final byte[] sourceAsBytes = multiGetItemResponse.getResponse().getSourceAsBytes();
              if (sourceAsBytes == null) {
                //monoSink.error(new RuntimeException("source is null (ids: " + ids + ")"));
                return null;
              }
              return mapper.readValue(sourceAsBytes, Product.class);
            } catch (IOException e) {
              LOG.error("boom", e);
            }
            return null;

          })
              .filter(Objects::nonNull)
              .collect(Collectors.toList());
          monoSink.success(result);
        }

        @Override
        public void onFailure(Exception e) {
          LOG.error("failure: ", e);
          monoSink.error(e);
        }
      };
      client.mgetAsync(multiGetRequest, RequestOptions.DEFAULT, actionListener);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  public void insert(Product product) throws RuntimeException {
    LOG.debug("inserting {}", product);
    try {
      final IndexRequest indexRequest = Requests.indexRequest(INDEX_NAME).type(DOC_TYPE);
      indexRequest.source(mapper.writeValueAsString(product), XContentType.JSON);
      client.index(indexRequest, RequestOptions.DEFAULT);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }


  public void updateAsync(Product product, MonoSink<Product> sink) throws RuntimeException {
    try {
      LOG.debug("updating {}", product);
      final IndexRequest indexRequest = new IndexRequest(INDEX_NAME, DOC_TYPE, product.getId() + "");
      indexRequest.source(mapper.writeValueAsString(product), XContentType.JSON);
      client.indexAsync(indexRequest, RequestOptions.DEFAULT, new ActionListener<IndexResponse>() {
        @Override
        public void onResponse(IndexResponse response) {
          LOG.debug("response: {}", response );
          sink.success(product);
        }

        @Override
        public void onFailure(Exception e) {
          sink.error(e);
        }
      });
    } catch (JsonProcessingException e) {
      sink.error(e);
    }
  }


}

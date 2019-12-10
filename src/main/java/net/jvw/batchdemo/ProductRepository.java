package net.jvw.batchdemo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpHost;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetRequest;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Requests;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class ProductRepository {

  private static final Logger LOG = LoggerFactory.getLogger(ProductRepository.class);

  private static final String INDEX_NAME = "product";
  public static final String DOC_TYPE = "_doc";
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


  public List<Product> getBatch(List<Long> ids) throws RuntimeException {
    LOG.debug("getting batch: {}", ids);
    try {
      MultiGetRequest multiGetRequest = new MultiGetRequest();

      ids.forEach(id -> multiGetRequest.add(INDEX_NAME, DOC_TYPE, "" + id));

      final MultiGetResponse response = client.mget(multiGetRequest, RequestOptions.DEFAULT);

      return Arrays.stream(response.getResponses()).map(multiGetItemResponse -> {

        try {
          return mapper.readValue(multiGetItemResponse.getResponse().getSourceAsBytes(), Product.class);
        } catch (IOException e) {
          LOG.error("boom", e);
        }
        return new Product();

      }).collect(Collectors.toList());


    } catch (IOException e) {
      throw new RuntimeException(e);
    }

  }


  public void insert(Product product) throws RuntimeException {
    try {
      final IndexRequest indexRequest = Requests.indexRequest(INDEX_NAME).type(DOC_TYPE);
      indexRequest.source(mapper.writeValueAsString(product), XContentType.JSON);
      client.index(indexRequest, RequestOptions.DEFAULT);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }


}

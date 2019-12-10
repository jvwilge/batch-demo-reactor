package net.jvw.batchdemo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

class GenerateTestDataTest {

  private static final ObjectMapper mapper = new ObjectMapper();

  private static EasyRandom random = new EasyRandom();


  @BeforeAll
  static void setUp() {
    Locale.setDefault(new Locale("en", "UK"));

    EasyRandomParameters parameters = new EasyRandomParameters()
        .seed(123L)
        .stringLengthRange(10, 20);

    random = new EasyRandom(parameters);
  }

  @Test
  void test() throws Exception {

    List<Long> isList = new ArrayList<>();

    Files.createFile(Paths.get("/tmp/products.jsonl"));
    Files.createFile(Paths.get("/tmp/updates.jsonl"));

    while (isList.size() < 8000) {
      final double price = Math.abs(random.nextDouble() * 100);
      final long id = Math.abs(random.nextLong());
      isList.add(id);
      final Product product = new Product(id, price, random.nextInt(1000), random.nextObject(String.class));
      final String s =
          "{ \"index\" : { \"_index\" : \"product\", \"_id\" : \"" + product.getId() + "\" } }\n" +
              mapper.writeValueAsString(product) + "\n";

      Files.writeString(Paths.get("/tmp/products.jsonl"), s, StandardOpenOption.APPEND);
      System.out.println(s);
    }
    Files.writeString(Paths.get("/tmp/products.jsonl"), "\n", StandardOpenOption.APPEND);

    for (int i = 0; i < 500_000; i++) {
      final int index = random.nextInt(isList.size());
      final ProductUpdate update = new ProductUpdate(isList.get(index), random.nextDouble() * 10 - 5, random.nextInt(10) - 5);
      final String u = mapper.writeValueAsString(update) + "\n";
      Files.writeString(Paths.get("/tmp/updates.jsonl"), u, StandardOpenOption.APPEND);
      System.out.println(u);
    }

    Files.writeString(Paths.get("/tmp/updates.jsonl"), "\n", StandardOpenOption.APPEND);


    //TODO jvw create testdata en schrijf naar txt-file
  }

}

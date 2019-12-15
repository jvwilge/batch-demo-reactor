package net.jvw.batchdemo;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BatchDemoApplication {

	public static void main(String[] args) {
		try {
			Class.forName(XContentBuilder.class.getName(), true, XContentBuilder.class.getClassLoader());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		SpringApplication.run(BatchDemoApplication.class, args);
	}

}

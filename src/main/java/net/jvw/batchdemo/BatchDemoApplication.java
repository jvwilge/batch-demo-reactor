package net.jvw.batchdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BatchDemoApplication {

	public static void main(String[] args) {
//		Schedulers.enableMetrics();
		SpringApplication.run(BatchDemoApplication.class, args);
	}

}

package com.hinetics.caresync;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class CareSyncApplication {

	public static void main(String[] args) {
		SpringApplication.run(CareSyncApplication.class, args);
	}

}

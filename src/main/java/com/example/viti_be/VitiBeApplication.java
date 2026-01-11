package com.example.viti_be;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class VitiBeApplication {

	public static void main(String[] args) {
		SpringApplication.run(VitiBeApplication.class, args);
	}

}

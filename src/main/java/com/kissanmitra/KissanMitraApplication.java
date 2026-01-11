package com.kissanmitra;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;

@SpringBootApplication(exclude = {RedisAutoConfiguration.class})
public class KissanMitraApplication {

	public static void main(String[] args) {
		SpringApplication.run(KissanMitraApplication.class, args);
	}

}

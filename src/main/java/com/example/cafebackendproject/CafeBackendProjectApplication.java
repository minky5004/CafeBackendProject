package com.example.cafebackendproject;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class CafeBackendProjectApplication {

    public static void main(String[] args) {
        SpringApplication.run(CafeBackendProjectApplication.class, args);
    }

}

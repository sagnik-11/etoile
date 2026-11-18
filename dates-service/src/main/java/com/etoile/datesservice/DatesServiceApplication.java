package com.etoile.datesservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class DatesServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DatesServiceApplication.class, args);
    }
}

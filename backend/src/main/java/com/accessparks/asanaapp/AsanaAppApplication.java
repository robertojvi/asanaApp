package com.accessparks.asanaapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AsanaAppApplication {
    public static void main(String[] args) {
        SpringApplication.run(AsanaAppApplication.class, args);
    }
}

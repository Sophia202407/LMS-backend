package com.example.restarter_backend;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RestarterBackendApplication {

    public static void main(String[] args) {
        // Load the .env file
        Dotenv dotenv = Dotenv.configure().load();
        dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(),
        entry.getValue()));

        SpringApplication.run(RestarterBackendApplication.class, args);
    }

}

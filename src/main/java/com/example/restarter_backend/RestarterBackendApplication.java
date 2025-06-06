package com.example.restarter_backend;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableScheduling
public class RestarterBackendApplication {

    public static void main(String[] args) {
        // Load the .env file
        Dotenv dotenv = Dotenv.configure().load();
        System.out.println("DB_USERNAME=" + dotenv.get("DB_USERNAME")); // Debug
        
        dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(),
        entry.getValue()));

        SpringApplication.run(RestarterBackendApplication.class, args);
    }

}

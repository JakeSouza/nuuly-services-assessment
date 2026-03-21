package com.nuuly;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

/** 
 * This is the main class for the Spring Boot application.
 */
@SpringBootApplication
@EnableKafka
public class Main {
    /**
     * This is the main method that starts the Spring Boot application.
     * @param args Command line argument (not used in this application)
    */
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }
}
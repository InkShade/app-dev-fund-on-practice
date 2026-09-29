package com.fridgechef;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class FridgeChefApplication {

    public static void main(String[] args) {
        SpringApplication.run(FridgeChefApplication.class, args);
    }
}

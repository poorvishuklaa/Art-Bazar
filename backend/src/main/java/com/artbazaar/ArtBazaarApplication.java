package com.artbazaar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ArtBazaarApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArtBazaarApplication.class, args);
    }
}

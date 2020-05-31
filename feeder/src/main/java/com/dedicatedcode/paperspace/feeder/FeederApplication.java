package com.dedicatedcode.paperspace.feeder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FeederApplication {

    public static void main(String[] args) {
        SpringApplication.run(FeederApplication.class, args);
    }

}

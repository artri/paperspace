package com.dedicatedcode.paperspace.feeder;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class FeederConfiguration {
    @Bean("fileHandlerThreadPool")
    public ExecutorService cachedThreadPool() {
        return Executors.newFixedThreadPool(10);
    }
}

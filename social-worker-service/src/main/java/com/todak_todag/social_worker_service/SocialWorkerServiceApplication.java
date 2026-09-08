package com.todak_todag.social_worker_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableFeignClients
@EnableDiscoveryClient
@SpringBootApplication
public class SocialWorkerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(
                SocialWorkerServiceApplication.class,
                args
        );
    }
}

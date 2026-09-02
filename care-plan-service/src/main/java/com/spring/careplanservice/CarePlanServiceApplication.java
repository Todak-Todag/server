package com.spring.careplanservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class CarePlanServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CarePlanServiceApplication.class, args);
    }

}

package com.bedirhan.cityeventmonitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CityEventMonitorApplication {

    public static void main(String[] args) {
        SpringApplication.run(CityEventMonitorApplication.class, args);
    }

}

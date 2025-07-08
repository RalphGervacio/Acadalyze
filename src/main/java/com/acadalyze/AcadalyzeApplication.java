package com.acadalyze;

import nz.net.ultraq.thymeleaf.layoutdialect.LayoutDialect;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class AcadalyzeApplication {
    public static void main(String[] args) {
        System.out.println("Starting Acadalyze...");
        SpringApplication.run(AcadalyzeApplication.class, args);
    }
    
    @Bean
    public LayoutDialect layoutDialect(){
        return new LayoutDialect();
    }
}


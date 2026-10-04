package com.lin.elm;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages =  "com.lin.elm")
@MapperScan ("com.lin.elm.mapper")
public class ElmApplication {

    public static void main(String[] args) {
        SpringApplication.run(ElmApplication.class, args);
    }

    
}

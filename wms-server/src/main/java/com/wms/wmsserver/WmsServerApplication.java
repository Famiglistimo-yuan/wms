package com.wms.wmsserver;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.wms.wmsserver.mapper")
public class WmsServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(WmsServerApplication.class, args);
    }

}

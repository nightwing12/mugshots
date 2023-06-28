package com.uncharted.mugshots;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan(basePackages = {"com.uncharted"})
@EntityScan({"com.uncharted"})
@ConfigurationPropertiesScan("com.uncharted")
@EnableCaching
@EnableScheduling
@EnableAsync
@Slf4j
public class MugshotsApplication {

    public static void main(String... args) {
        SpringApplication.run(MugshotsApplication.class, args);
    }

}

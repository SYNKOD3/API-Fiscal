package br.com.antigravity.fiscalapi;

import br.com.antigravity.fiscalapi.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class FiscalApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(FiscalApiApplication.class, args);
    }
}

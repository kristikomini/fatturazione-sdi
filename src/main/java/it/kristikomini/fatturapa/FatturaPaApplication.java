package it.kristikomini.fatturapa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** FatturaPA / SDI e-invoicing service — Spring Boot 3 / Java 21. */
@SpringBootApplication
@EnableScheduling // drives the outbox relay
public class FatturaPaApplication {

    public static void main(String[] args) {
        SpringApplication.run(FatturaPaApplication.class, args);
    }
}

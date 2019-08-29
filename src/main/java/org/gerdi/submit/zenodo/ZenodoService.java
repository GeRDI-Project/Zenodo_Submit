package org.gerdi.submit.zenodo;

import org.gerdi.submit.security.SecurityConfiguration;
import org.gerdi.submit.security.TokenProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication(scanBasePackages = {"org.gerdi.submit.*"})
public class ZenodoService {

    public static void main(String... args) {
        SpringApplication.run(ZenodoService.class, args);
    }

}

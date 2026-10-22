package io.github.stuartvision.riskfeed;

import java.time.Clock;
import java.util.Arrays;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@ConfigurationPropertiesScan
public class RiskFeedApplication {

    public static void main(String[] args) {
        var context = SpringApplication.run(RiskFeedApplication.class, args);
        // Command-line import (used by schedulers): exit with the import's exit code.
        if (Arrays.stream(args).anyMatch(a -> a.startsWith("--import="))) {
            System.exit(SpringApplication.exit(context));
        }
    }

    /** Injected wherever "today" matters, so tests can fix the date. */
    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }
}

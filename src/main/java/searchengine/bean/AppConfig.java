package searchengine.bean;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ForkJoinPool;

@Configuration
public class AppConfig {
    @Bean
    public ForkJoinPool initializeForkJoinPool() {
        return new ForkJoinPool();
    }
}


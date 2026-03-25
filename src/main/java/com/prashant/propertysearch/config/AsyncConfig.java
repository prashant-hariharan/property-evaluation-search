package com.prashant.propertysearch.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration class for enabling asynchronous execution and defining a custom thread pool task executor.
   <p>
 * The configuration ensures that the application can execute tasks asynchronously with a controlled
 * thread pool, improving scalability and responsiveness.
 *
 * @author prashant
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "propertySearchIndexTaskExecutor")
    public Executor propertySearchIndexTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("property-index-");
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(200);
        executor.initialize();
        return executor;
    }
}

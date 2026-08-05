package com.shopflow.notification.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Dedicated executor for @Async("notificationExecutor") methods.
 *
 * ThreadPoolTaskExecutor mechanics (interview staple):
 *   new task -> core threads busy? -> QUEUE it -> queue full? -> grow to max
 *   -> at max AND queue full? -> RejectedExecutionHandler decides.
 * CallerRunsPolicy = the submitting (Kafka listener) thread runs the task
 * itself - natural BACKPRESSURE: intake slows instead of dropping
 * notifications or dying with OOM from an unbounded queue.
 */
@Configuration
@EnableAsync   // without this, @Async is a silent no-op
public class AsyncConfig {

    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("notif-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}

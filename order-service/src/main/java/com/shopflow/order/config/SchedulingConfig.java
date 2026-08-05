package com.shopflow.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * THREAD POOL FOR SCHEDULED TASKS.
 *
 * DEFAULT PITFALL: out of the box, Spring runs ALL @Scheduled methods on a
 * SINGLE thread. One slow job silently delays every other job - a classic
 * production incident ("why did the 2am report also stop the 30s sweep?").
 *
 * FIX: provide a ThreadPoolTaskScheduler via SchedulingConfigurer. Now up to
 * 4 jobs run concurrently, and the thread-name prefix makes them instantly
 * identifiable in logs and thread dumps ("sched-1", "sched-2", ...).
 */
@Configuration
public class SchedulingConfig implements SchedulingConfigurer {

    @Bean(destroyMethod = "shutdown")
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("sched-");
        // If shutdown happens mid-job, wait briefly instead of killing it.
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(10);
        scheduler.initialize();
        return scheduler;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.setTaskScheduler(taskScheduler());
    }
}

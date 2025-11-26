package com.iscm.iam.config;

// import lombok.extern.slf4j.Slf4j;
// import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.scheduling.annotation.AsyncConfigurer;
// import org.springframework.scheduling.annotation.EnableAsync;
// import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
// import org.springframework.core.task.TaskExecutor;

// import java.lang.reflect.Method;
// import java.util.concurrent.Executor;
// import java.util.concurrent.ThreadPoolExecutor;

// @Slf4j
// @Configuration
// @EnableAsync
public class AsyncConfig /*implements AsyncConfigurer*/ {

    // @Value("${app.async.core-pool-size:5}")
    // private int corePoolSize;

    // @Value("${app.async.max-pool-size:20}")
    // private int maxPoolSize;

    // @Value("${app.async.queue-capacity:100}")
    // private int queueCapacity;

    // @Value("${app.async.thread-name-prefix:iscm-async-}")
    // private String threadNamePrefix;

    // @Bean(name = "taskExecutor")
    // @Override
    // public Executor getAsyncExecutor() {
    //     ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

    //     // Core configuration
    //     executor.setCorePoolSize(corePoolSize);
    //     executor.setMaxPoolSize(maxPoolSize);
    //     executor.setQueueCapacity(queueCapacity);
    //     executor.setThreadNamePrefix(threadNamePrefix);

    //     // Rejection policy
    //     executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

    //     // Wait for tasks to complete on shutdown
    //     executor.setWaitForTasksToCompleteOnShutdown(true);
    //     executor.setAwaitTerminationSeconds(30);

    //     // Thread configuration
    //     executor.setThreadGroupName("iscm-async-group");
    //     executor.setKeepAliveSeconds(60);

    //     executor.initialize();
    //     log.info("Async task executor initialized with coreSize={}, maxSize={}, queueCapacity={}",
    //             corePoolSize, maxPoolSize, queueCapacity);

    //     return executor;
    // }

    // @Bean(name = "securityEventExecutor")
    // public TaskExecutor securityEventExecutor() {
    //     ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    //     executor.setCorePoolSize(3);
    //     executor.setMaxPoolSize(10);
    //     executor.setQueueCapacity(50);
    //     executor.setThreadNamePrefix("security-event-");
    //     executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    //     executor.setWaitForTasksToCompleteOnShutdown(true);
    //     executor.setAwaitTerminationSeconds(20);
    //     executor.initialize();
    //     return executor;
    // }

    // @Bean(name = "emailExecutor")
    // public TaskExecutor emailExecutor() {
    //     ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    //     executor.setCorePoolSize(2);
    //     executor.setMaxPoolSize(8);
    //     executor.setQueueCapacity(100);
    //     executor.setThreadNamePrefix("email-");
    //     executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    //     executor.setWaitForTasksToCompleteOnShutdown(true);
    //     executor.setAwaitTerminationSeconds(60);
    //     executor.initialize();
    //     return executor;
    // }

    // @Bean(name = "auditExecutor")
    // public TaskExecutor auditExecutor() {
    //     ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    //     executor.setCorePoolSize(2);
    //     executor.setMaxPoolSize(5);
    //     executor.setQueueCapacity(200);
    //     executor.setThreadNamePrefix("audit-");
    //     executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    //     executor.setWaitForTasksToCompleteOnShutdown(true);
    //     executor.setAwaitTerminationSeconds(10);
    //     executor.initialize();
    //     return executor;
    // }

    // @Bean(name = "cleanupExecutor")
    // public TaskExecutor cleanupExecutor() {
    //     ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    //     executor.setCorePoolSize(1);
    //     executor.setMaxPoolSize(3);
    //     executor.setQueueCapacity(20);
    //     executor.setThreadNamePrefix("cleanup-");
    //     executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    //     executor.setWaitForTasksToCompleteOnShutdown(true);
    //     executor.setAwaitTerminationSeconds(120);
    //     executor.initialize();
    //     return executor;
    // }

    // @Override
    // public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
    //     return new CustomAsyncExceptionHandler();
    // }

    // private static class CustomAsyncExceptionHandler implements AsyncUncaughtExceptionHandler {
    //     @Override
    //     public void handleUncaughtException(Throwable ex, Method method, Object... params) {
    //         log.error("Async method {} threw exception with parameters: {}", method.getName(), params, ex);
    //     }
    // }
}
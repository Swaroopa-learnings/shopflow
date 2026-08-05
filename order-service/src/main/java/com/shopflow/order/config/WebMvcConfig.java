package com.shopflow.order.config;

import com.shopflow.order.web.interceptor.TimingInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Interceptors are NOT auto-registered by @Component alone (unlike Filters) -
 * they must be added to the MVC registry, with optional path patterns.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final TimingInterceptor timingInterceptor;

    public WebMvcConfig(TimingInterceptor timingInterceptor) {
        this.timingInterceptor = timingInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(timingInterceptor)
                .addPathPatterns("/api/**");   // scope it to the API, skip actuator
    }
}

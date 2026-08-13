package com.shopflow.order.config;

import com.shopflow.order.web.interceptor.TimingInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers MVC interceptors. Unlike filters, interceptors are not picked up
 * by @Component alone - they must be added here.
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
                .addPathPatterns("/api/**");   // API only, skip actuator
    }
}

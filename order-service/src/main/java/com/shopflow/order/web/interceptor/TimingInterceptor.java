package com.shopflow.order.web.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Times each controller method and logs how long it took.
 *
 * Runs inside Spring MVC after handler mapping, so unlike a servlet filter it
 * knows which controller method will handle the request. Registered in
 * WebMvcConfig. afterCompletion always runs, including on exceptions.
 */
@Component
public class TimingInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(TimingInterceptor.class);
    private static final String START_ATTR = "timing.start";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        log.info("In preHandle of TimingInterceptor of order-service");
        request.setAttribute(START_ATTR, System.nanoTime());
        return true;   // false would stop the request here
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        log.info("In afterCompletion of TimingInterceptor of order-service");
        Object start = request.getAttribute(START_ATTR);
        if (start instanceof Long s && handler instanceof HandlerMethod method) {
            long ms = (System.nanoTime() - s) / 1_000_000;
            log.info("Handler {}.{} took {} ms{}",
                    method.getBeanType().getSimpleName(), method.getMethod().getName(), ms,
                    ex != null ? " (exception: " + ex.getClass().getSimpleName() + ")" : "");
        }
    }
}

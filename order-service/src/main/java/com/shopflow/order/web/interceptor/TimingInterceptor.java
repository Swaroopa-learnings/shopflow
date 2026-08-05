package com.shopflow.order.web.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * SPRING MVC INTERCEPTOR (HandlerInterceptor) - registered in WebMvcConfig.
 *
 * Runs INSIDE Spring MVC, after handler mapping - so unlike a Servlet Filter
 * it KNOWS which controller method is about to execute (the `handler` param).
 * That makes interceptors the right tool for handler-aware concerns:
 * per-endpoint timing, checking custom annotations on controller methods
 * (e.g. a home-made @RequiresRole), tenant resolution, etc.
 *
 * Lifecycle:
 *  - preHandle        before the controller (return false = abort request)
 *  - postHandle       after the controller, before view rendering
 *  - afterCompletion  after the response is done, ALWAYS called (even on
 *                     exception) - the safe place to stop timers / clean up.
 */
@Component
public class TimingInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(TimingInterceptor.class);
    private static final String START_ATTR = "timing.start";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_ATTR, System.nanoTime());
        return true;   // false would short-circuit the request here
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        Object start = request.getAttribute(START_ATTR);
        if (start instanceof Long s && handler instanceof HandlerMethod method) {
            long ms = (System.nanoTime() - s) / 1_000_000;
            // We can name the exact controller method - a Filter cannot do this.
            log.info("Handler {}.{} took {} ms{}",
                    method.getBeanType().getSimpleName(), method.getMethod().getName(), ms,
                    ex != null ? " (exception: " + ex.getClass().getSimpleName() + ")" : "");
        }
    }
}

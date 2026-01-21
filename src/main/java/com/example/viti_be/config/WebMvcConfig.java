package com.example.viti_be.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC Configuration for interceptors and other web-related settings
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final CartCookieInterceptor cartCookieInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Register cart cookie interceptor for cart endpoints
        registry.addInterceptor(cartCookieInterceptor)
                .addPathPatterns("/api/cart/**");
    }
}

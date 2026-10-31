package com.studyloop.config;

import com.studyloop.common.web.CurrentUserArgumentResolver;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final CurrentUserArgumentResolver currentUserResolver;

    public WebConfig(CurrentUserArgumentResolver currentUserResolver) {
        this.currentUserResolver = currentUserResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserResolver);
    }

    /**
     * Single-service deployment: when the React build is bundled into {@code classpath:/static} (root Dockerfile),
     * serve it from the same origin as the API. Hashed assets are cached for a year; every other non-API path without
     * a file extension falls back to {@code index.html} so client-side routes survive a reload. API paths are never
     * rewritten, so unknown {@code /api/**} URLs still return the JSON 404. Without a bundled build nothing is
     * registered and the service is API-only.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        if (!new ClassPathResource("static/index.html").exists()) {
            return;
        }
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("classpath:/static/assets/")
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable());
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.noCache())
                .resourceChain(true)
                .addResolver(new SpaFallbackResolver());
    }

    static final class SpaFallbackResolver extends PathResourceResolver {
        @Override
        protected Resource getResource(String resourcePath, Resource location) throws IOException {
            if (resourcePath.startsWith("api/") || resourcePath.equals("api")) {
                return null;
            }
            if (!resourcePath.isEmpty() && !resourcePath.endsWith("/")) {
                Resource requested = location.createRelative(resourcePath);
                if (requested.exists() && requested.isReadable()) {
                    return requested;
                }
                String last = resourcePath.substring(resourcePath.lastIndexOf('/') + 1);
                if (last.contains(".")) {
                    return null;
                }
            }
            Resource index = location.createRelative("index.html");
            return index.exists() && index.isReadable() ? index : null;
        }
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(AppProperties props) {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(props.cors().allowedOrigins().stream().map(String::trim).toList());
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
        cfg.setExposedHeaders(List.of("X-Trace-Id", "Retry-After"));
        cfg.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cfg);
        return source;
    }
}

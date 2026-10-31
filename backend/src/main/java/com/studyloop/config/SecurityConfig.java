package com.studyloop.config;

import com.studyloop.common.error.ErrorCode;
import com.studyloop.common.error.ErrorResponseWriter;
import com.studyloop.common.web.LastActiveFilter;
import com.studyloop.common.web.UserActivityTracker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ErrorResponseWriter errors,
                                                   UserActivityTracker tracker) throws Exception {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("role");
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(h -> h
                        .contentTypeOptions(Customizer.withDefaults())
                        .frameOptions(f -> f.deny())
                        .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/signup", "/api/auth/login", "/api/auth/refresh",
                                "/api/auth/logout", "/api/auth/demo").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/admin/demo/reset").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/public/**", "/api/health").permitAll()
                        .requestMatchers("/api/docs", "/api/docs/**", "/api/swagger-ui/**", "/api/swagger-ui.html",
                                "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        // The bundled React app (single-service deploy): static files and client-side routes.
                        .requestMatchers(HttpMethod.GET, "/**").permitAll()
                        .requestMatchers(HttpMethod.HEAD, "/**").permitAll()
                        .anyRequest().denyAll())
                .oauth2ResourceServer(o -> o
                        .jwt(j -> j.jwtAuthenticationConverter(converter))
                        .authenticationEntryPoint((req, res, ex) -> errors.write(req, res, ErrorCode.UNAUTHENTICATED))
                        .accessDeniedHandler((req, res, ex) -> errors.write(req, res, ErrorCode.FORBIDDEN)))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> errors.write(req, res, ErrorCode.UNAUTHENTICATED))
                        .accessDeniedHandler((req, res, ex) -> errors.write(req, res, ErrorCode.FORBIDDEN)))
                .addFilterAfter(new LastActiveFilter(tracker), BearerTokenAuthenticationFilter.class);
        return http.build();
    }
}

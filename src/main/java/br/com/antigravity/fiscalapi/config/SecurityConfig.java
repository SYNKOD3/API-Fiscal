package br.com.antigravity.fiscalapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            BasicAuthFilter basicAuthFilter,
                                            ApiKeyFilter apiKeyFilter,
                                            JwtAuthenticationFilter jwtAuthenticationFilter,
                                            AppProperties properties) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> {
                auth.requestMatchers("/actuator/health").permitAll();
                auth.requestMatchers("/api/v1/auth/token").permitAll();
                if (properties.getDevConsole().isEnabled()) {
                    auth.requestMatchers("/dev/**").permitAll();
                }
                if (properties.getOpenApi().isPublicAccess()) {
                    auth.requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll();
                }
                auth.anyRequest().authenticated();
            })
            .addFilterBefore(basicAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(apiKeyFilter, BasicAuthFilter.class)
            .addFilterAfter(jwtAuthenticationFilter, ApiKeyFilter.class)
            .build();
    }
}

package com.example.batchmonitor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfiguration {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                             @Value("${app.security.enabled:false}") boolean enabled) throws Exception {
        if (enabled) {
            http.authorizeHttpRequests(auth -> auth.requestMatchers("/app.css", "/app.js").permitAll().anyRequest().authenticated())
                    .httpBasic(httpBasic -> {})
                    .csrf(csrf -> csrf.disable());
        } else {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll()).csrf(csrf -> csrf.disable());
        }
        return http.build();
    }

    @Bean
    UserDetailsService users(@Value("${app.security.username:admin}") String username,
                             @Value("${app.security.password:change-me}") String password) {
        var encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        return new InMemoryUserDetailsManager(User.withUsername(username).password(encoder.encode(password)).roles("BATCH_ADMIN").build());
    }
}

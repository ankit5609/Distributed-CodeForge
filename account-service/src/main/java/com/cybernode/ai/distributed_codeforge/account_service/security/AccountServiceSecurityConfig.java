package com.cybernode.ai.distributed_codeforge.account_service.security;

import com.cybernode.ai.distributed_codeforge.common_lib.security.JwtAuthFilter;
import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity
public class AccountServiceSecurityConfig {
    private final JwtAuthFilter jwtAuthFilter;
    private final HandlerExceptionResolver handlerExceptionResolver;


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity){

        try {
            httpSecurity
                    .csrf(csrfConfig-> csrfConfig.disable())
                    .cors(Customizer.withDefaults())
                    .sessionManagement(sessionConfig -> sessionConfig.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/api/auth/**","/webhooks/**"
                            ).permitAll()
                            .anyRequest().authenticated()
                    )
                    .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                    .exceptionHandling(exceptionHandlingConfigurer ->
                            exceptionHandlingConfigurer.accessDeniedHandler((request, response, accessDeniedException) -> {
                                handlerExceptionResolver.resolveException(request, response, null, accessDeniedException);
                            }));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        try {
            return httpSecurity.build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}

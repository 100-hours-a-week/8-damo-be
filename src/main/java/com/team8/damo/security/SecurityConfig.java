package com.team8.damo.security;

import com.team8.damo.security.handler.CustomLogoutHandler;
import com.team8.damo.security.jwt.JwtAuthenticationFilter;
import com.team8.damo.security.jwt.JwtExceptionFilter;
import com.team8.damo.security.jwt.JwtProvider;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] AUTH_WHITELIST = {
        "/api/v1/auth/oauth",
        "/api/v1/auth/reissue",
        "/api/v1/auth/test",
        "/api/v1/s3",
        "/login",
        "/api/healthy",
        "/swagger-ui/**",
        "/v3/api-docs/**",
        "/api-test",
        "/api/ws-stomp",
        "/sub/**",
        "/pub/**",
        "/actuator/health",
        "/actuator/prometheus"
    };

    private final JwtProvider jwtProvider;
    private final JwtExceptionFilter jwtExceptionFilter;
    private final CustomLogoutHandler customLogoutHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors
                .configurationSource(corsConfigurationSource())
            )
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .sessionManagement(sessionManagement ->
                sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .logout(logout -> logout
                .logoutUrl("/api/logout")
                .addLogoutHandler(customLogoutHandler)
                .logoutSuccessHandler((request, response, authentication) -> {
                    response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                })
                .deleteCookies("access_token", "refresh_token")
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(AUTH_WHITELIST).permitAll()
                .anyRequest().authenticated()
            )
            .addFilterAt(new JwtAuthenticationFilter(jwtProvider), UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtExceptionFilter, JwtAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("https://localhost:3000", "http://localhost:5173", "https://damo.today"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Set-Cookie"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

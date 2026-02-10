package com.example.ForDay.global.config.security;

import com.example.ForDay.global.filter.JwtTokenFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenFilter jwtTokenFilter;


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(configurationSource()))
                .csrf((auth) -> auth.disable())
                .formLogin((auth) -> auth.disable())
                .httpBasic((auth) -> auth.disable())
                .authorizeHttpRequests((auth) -> auth
                        .requestMatchers(
                                "/app/metadata", "/health_check", "/error_check", "/swagger-ui/**", "/v3/api-docs/**", "/v3/api-docs.yaml", "/log-test", "/terms/**")
                        .permitAll()
                        .requestMatchers(
                                "/auth/kakao", "/auth/apple", "/auth/guest", "/auth/refresh")
                        .permitAll()
                        /*.requestMatchers(
                                "/auth/switch-account"
                        ).hasRole("GUEST")
                        .requestMatchers("/users/info",
                                "/users/profile-image",
                                "/hobbies/cover-image",
                                "/users/hobbies/in-progress",
                                "/users/feeds",
                                "/users/hobby-cards",
                                "/users/scraps",
                                "/friends/**",
                                "/hobbies/stories/tabs",
                                "/records/stories",
                                "/recent/**",
                                "/hobbies/{hobbyId}/activities/{activityId}/collect",
                                "/records/{recordId}/scrap",
                                "/records/{recordId}/report"
                        ).hasRole("USER")*/
                        .anyRequest().authenticated())
                .sessionManagement((session) -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public CorsConfigurationSource configurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of(
                "http://localhost:5173",
                "http://localhost:5173/",
                "https://forday.kr"
        ));
        configuration.setAllowedMethods(List.of(
                "GET","POST","PUT","PATCH","DELETE","OPTIONS"
        ));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}

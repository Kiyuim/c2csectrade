package lut.cn.c2cplatform.config;

import lut.cn.c2cplatform.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
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
@EnableMethodSecurity
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // 使用 BCrypt 加密，匹配数据库中的密码格式
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
           .cors(cors -> cors.configurationSource(corsConfigurationSource()))
           .csrf(csrf -> csrf.disable())
           .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
           .authorizeHttpRequests(auth -> auth
               .requestMatchers("/api/auth/**").permitAll()
               .requestMatchers("/api/captcha/**").permitAll()
               .requestMatchers("/ws/**").permitAll() // allow WebSocket handshake and SockJS endpoints
               .requestMatchers("/api/products").permitAll() // 允许所有用户查看商品列表
               .requestMatchers("/api/products/*").permitAll() // 允许所有用户查看商品详情
               .requestMatchers("/api/products/search").permitAll() // 允许所有用户搜索商品
               .requestMatchers("/api/chat/history/**").authenticated()
               .requestMatchers("/api/chat/read/**").authenticated()
               .requestMatchers("/api/admin/**").hasRole("ADMIN")
               .requestMatchers("/actuator/health", "/actuator/info").permitAll()
               .anyRequest().authenticated()
            )
           .exceptionHandling(ex -> ex
               .authenticationEntryPoint((request, response, authException) -> {
                   response.setContentType("application/json;charset=UTF-8");
                   response.setStatus(401);
                   response.getWriter().write("{\"error\":\"未授权访问\"}");
               })
               .accessDeniedHandler((request, response, accessDeniedException) -> {
                   response.setContentType("application/json;charset=UTF-8");
                   response.setStatus(403);
                   response.getWriter().write("{\"error\":\"访问被拒绝\"}");
               })
           );
        // use the injected component instead of creating a new instance
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Apply CORS to all endpoints including /ws/**
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

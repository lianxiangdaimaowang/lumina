package com.lianxiangdaimaowang.lumina.config;

import com.lianxiangdaimaowang.lumina.filter.JwtRequestFilter;
import com.lianxiangdaimaowang.lumina.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.http.HttpMethod;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final CorsConfigurationSource corsConfigurationSource;

    @Autowired
    public SecurityConfig(UserDetailsService userDetailsService, JwtUtil jwtUtil, 
                         PasswordEncoder passwordEncoder, JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
                         CustomAccessDeniedHandler accessDeniedHandler,
                         CorsConfigurationSource corsConfigurationSource) {
        this.userDetailsService = userDetailsService;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public JwtRequestFilter jwtRequestFilter() {
        return new JwtRequestFilter(jwtUtil, userDetailsService);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors().configurationSource(corsConfigurationSource)
                .and()
            .csrf()
                .disable()
            .authorizeRequests()
                // 允许所有公共端点访问
                .antMatchers(
                    "/",
                    "/index.html", 
                    "/static/**", 
                    "/api/server/info", 
                    "/api/health/**", 
                    "/swagger-ui.html", 
                    "/swagger-ui/**", 
                    "/v2/api-docs", 
                    "/v3/api-docs", 
                    "/swagger-resources/**", 
                    "/webjars/**",
                    "/actuator/health/**", 
                    "/actuator/info/**",
                    "/error"
                ).permitAll()
                // 特别允许身份验证端点
                .antMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/register").permitAll()
                .antMatchers(HttpMethod.GET, "/api/auth/refresh").permitAll()
                // 管理员专用端点
                .antMatchers("/actuator/**").hasRole("ADMIN")
                // 其他所有请求需要认证
                .anyRequest().authenticated()
                .and()
            .exceptionHandling()
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
                .and()
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS);

        // 添加JWT过滤器并确保它在UsernamePasswordAuthenticationFilter之前执行
        http.addFilterBefore(jwtRequestFilter(), UsernamePasswordAuthenticationFilter.class);
                
        // 设置认证提供者
        http.authenticationProvider(authenticationProvider());
                
        return http.build();
    }
} 
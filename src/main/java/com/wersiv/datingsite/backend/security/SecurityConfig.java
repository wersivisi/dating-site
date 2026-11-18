package com.wersiv.datingsite.backend.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/avatars/**").permitAll()
                        .requestMatchers("/", "/index", "/index.html", "/css/**", "/js/**", "/error",
                                "/login", "/login.html", "/register", "/register.html",
                                "/help", "/help.html", "/safety", "/safety.html", "/privacy", "/privacy.html",
                                "/terms", "/terms.html", "/forgot-password", "/forgot-password.html",
                                "/verify-email", "/verify-email.html", "/reset-password", "/reset-password.html",
                                "/reset-success", "/reset-success.html").permitAll()
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .formLogin(form -> form.loginPage("/login").loginProcessingUrl("/login")
                        .usernameParameter("email").defaultSuccessUrl("/my-profile", true).failureUrl("/login?error").permitAll())
                .logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/login?logout"))
                .exceptionHandling(errors -> errors.authenticationEntryPoint((request, response, exception) -> {
                    if (request.getRequestURI().substring(request.getContextPath().length()).startsWith("/api/")) {
                        response.setStatus(401);
                    } else {
                        response.sendRedirect(request.getContextPath() + "/login");
                    }
                }))
                .httpBasic(AbstractHttpConfigurer::disable)
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"));

        return http.build();
    }
}

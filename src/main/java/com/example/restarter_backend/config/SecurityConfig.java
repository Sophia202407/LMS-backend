package com.example.restarter_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod; 
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.core.userdetails.UserDetailsService; 
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Enable method-level security for @PreAuthorize
public class SecurityConfig {

    // Removed: private final UserService userService;
    // Removed the constructor that injected UserService, as it's no longer needed here.
    // public SecurityConfig(UserService userService) {
    //     this.userService = userService;
    // }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Disable CSRF for stateless API
            .cors(cors -> cors.configurationSource(corsConfigurationSource())) // Configure CORS
            .authorizeHttpRequests(authorize -> authorize
                // Allow registration and login without authentication
                .requestMatchers(
                    "/api/auth/**",
                    "/api/auth/register", 
                    "/api/auth/login", 
                    "/api/auth/logout", 
                    "/api/books",
                    "/api/books/status",
                    "/api/books/search"
                ).permitAll()
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            // Configure form login for session-based authentication
            .formLogin(form -> form.disable())

            // Configure logout for session invalidation
            .logout(logout -> logout
                .logoutUrl("/api/logout") // The URL to trigger logout from frontend
                .invalidateHttpSession(true) // Invalidate the server-side session
                .deleteCookies("JSESSIONID") // Delete the session cookie from the client
                .permitAll() // Allow anyone to perform logout
            )

            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED) // <-- IMPORTANT CHANGE: For session-based auth
            );
        return http.build();
    }

    // Expose AuthenticationManager as a bean
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Use BCryptPasswordEncoder for strong password hashing
        return new BCryptPasswordEncoder();
    }

    // Configure the AuthenticationProvider to use your UserDetailsService and PasswordEncoder
    // Spring will automatically inject the UserDetailsService bean (your UserService) here.
    @Bean
    public DaoAuthenticationProvider authenticationProvider(UserDetailsService userDetailsService) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService); // Use the injected userDetailsService
        authProvider.setPasswordEncoder(passwordEncoder()); // Call the passwordEncoder bean method
        return authProvider;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173")); // Allow your frontend origin
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept", "X-Requested-With", "remember-me"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L); // How long the pre-flight request can be cached
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // Apply CORS to all paths
        return source;
    }
}


//Jun.6 confirmed all your CORS requirements are met in SecurityConfig
// you can safely remove the separate CORS configuration file.

//Jun.7 key changes:
// update the authorization set on books, loans, logout and loginform
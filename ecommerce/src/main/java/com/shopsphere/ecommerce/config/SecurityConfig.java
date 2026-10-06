package com.shopsphere.ecommerce.config;

import com.shopsphere.ecommerce.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // NOTE: rules are matched top to bottom - order matters.
                .authorizeHttpRequests(auth -> auth

                        // CORS preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Public: auth + error page
                        .requestMatchers("/api/auth/**", "/error").permitAll()

                        // Public: API docs (turn off in production with SWAGGER_ENABLED=false)
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**",
                                "/v3/api-docs/**").permitAll()


                        // Razorpay -> our server (authenticated by HMAC signature)
                        .requestMatchers(HttpMethod.POST, "/api/payments/webhook").permitAll()

                        // Public: browsing the catalogue (read-only)
                        .requestMatchers(HttpMethod.GET,
                                "/api/products/**",
                                "/api/categories/**",
                                "/api/product-images/**",
                                "/api/product-variants/**",
                                "/api/reviews/product/**").permitAll()

                        // Admin only
                        .requestMatchers("/api/admin/**",
                                "/api/coupons/**").hasRole("ADMIN")

                        // Categories: writes are admin only
                        .requestMatchers("/api/categories/**").hasRole("ADMIN")

                        // Product catalogue writes: seller or admin
                        // (sellers may only touch their own - checked in ProductService)
                        .requestMatchers(
                                "/api/products/**",
                                "/api/product-images/**",
                                "/api/product-variants/**")
                        .hasAnyRole("SELLER", "ADMIN")

                        // Seller dashboard
                        .requestMatchers("/api/seller/**").hasRole("SELLER")

                        // Everything else needs a valid access token
                        .anyRequest().authenticated()
                )

                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, ex) -> {
                            response.setStatus(401);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write("{\"error\":\"Unauthorized\"}");
                        })
                        .accessDeniedHandler((request, response, ex) -> {
                            response.setStatus(403);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write("{\"error\":\"Access denied\"}");
                        })
                )

                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

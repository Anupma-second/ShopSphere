package com.shopsphere.ecommerce.security;

import com.shopsphere.ecommerce.entity.User;
import com.shopsphere.ecommerce.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    public JwtAuthenticationFilter(UserRepository userRepository,
                                   JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = jwtService.parseClaims(token);

            // FIX: a refresh token must never work as an access token
            if (jwtService.isAccessToken(claims)) {

                User user = userRepository
                        .findByEmail(claims.getSubject())
                        .orElse(null);

                // FIX: disabled accounts are rejected even with a valid token
                if (user != null && user.isEnabled()) {

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    user,
                                    null,
                                    List.of(new SimpleGrantedAuthority(
                                            "ROLE_" + user.getRole().name()))
                            );

                    SecurityContextHolder.getContext()
                            .setAuthentication(authentication);
                }
            }

        } catch (Exception e) {
            // Invalid or expired token: continue unauthenticated -> 401 later
        }

        filterChain.doFilter(request, response);
    }
}

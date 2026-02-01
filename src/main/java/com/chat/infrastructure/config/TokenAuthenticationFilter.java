package com.chat.infrastructure.config;

import com.chat.application.port.out.SessionTokenRepositoryPort;
import com.chat.application.port.out.UserRepositoryPort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

@Component
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private final SessionTokenRepositoryPort sessionRepository;
    private final UserRepositoryPort userRepositoryPort;

    public TokenAuthenticationFilter(SessionTokenRepositoryPort sessionRepository,
                                     UserRepositoryPort userRepositoryPort) {
        this.sessionRepository = sessionRepository;
        this.userRepositoryPort = userRepositoryPort;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {

            String token = header.substring(7);

            String userId = sessionRepository.findUserIdByToken(token)
                    .orElseThrow(() -> new IllegalArgumentException("Unauthorized"));

            Optional<String> usernameOpt = userRepositoryPort.findById(userId)
                    .map(u -> u.getUsername().value());

            if (usernameOpt.isPresent()) {
                String username = usernameOpt.get();

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        Collections.emptyList()
                );

                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        chain.doFilter(request, response);
    }
}
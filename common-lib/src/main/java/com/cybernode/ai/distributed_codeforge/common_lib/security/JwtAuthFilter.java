package com.cybernode.ai.distributed_codeforge.common_lib.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;


@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final AuthUtil authUtil;
    private final HandlerExceptionResolver handlerExceptionResolver;
    private final RequestAttributeSecurityContextRepository securityContextRepository =
            new RequestAttributeSecurityContextRepository();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        log.info("incoming requests: {}", request.getRequestURI());
        final String requestHeaderToken = request.getHeader("Authorization");
        if (requestHeaderToken != null && requestHeaderToken.startsWith("Bearer ")) {
            try {
                String jwtToken = requestHeaderToken.split(("Bearer "))[1];
                JwtUserPrincipal user = authUtil.verifyAccessToken(jwtToken);
                if (user != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                            user, jwtToken, user.authorities()
                    );
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                    securityContextRepository.saveContext(SecurityContextHolder.getContext(), request, response);
                }
            } catch (Exception e) {
                log.error("JWT authentication failed: {}", e.getMessage());
                handlerExceptionResolver.resolveException(request, response, null, e);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}

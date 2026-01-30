package io.reflectoring.carshippingbackend.configaration;

import io.reflectoring.carshippingbackend.Util.JwtUtil;
import io.reflectoring.carshippingbackend.services.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtTokenFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getServletPath();
        String method = request.getMethod();

        // Log the request for debugging
        logger.info("Processing " + method + " " + path +
                " Content-Type: " + request.getContentType());

        // Skip JWT validation for public GET paths
        if (method.equalsIgnoreCase("GET") && isPublicGetPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = null;

        // ALWAYS check cookies first (works for both regular and multipart requests)
        token = extractTokenFromCookies(request);

        // If not found in cookies, check Authorization header (for non-multipart)
        if (token == null) {
            String header = request.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                token = header.substring(7);
            }
        }

        // Validate token
        if (token != null && jwtUtil.validateToken(token)) {
            String email = jwtUtil.extractEmail(token);
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);

            logger.info("✅ Authenticated user: " + email);
        } else if (!isPublicEndpoint(path, method)) {
            logger.error("❌ Blocking " + method + " " + path + " - No valid token");
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Authentication required");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String extractTokenFromCookies(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            logger.warn("No cookies found in request");
            return null;
        }

        for (Cookie cookie : cookies) {
            logger.debug("Cookie: " + cookie.getName() + " (value length: " +
                    (cookie.getValue() != null ? cookie.getValue().length() : 0) + ")");

            // Check common JWT cookie names
            if ("auth-token".equals(cookie.getName())) {
                logger.info("Found auth-token cookie");
                return cookie.getValue();
            }
            if ("token".equals(cookie.getName())) {
                logger.info("Found token cookie");
                return cookie.getValue();
            }
        }

        return null;
    }

    private boolean isPublicGetPath(String path) {
        if (path.startsWith("/api/motorcycles/dashboard")) {
            return false; // Dashboard requires authentication
        }

        return path.startsWith("/api/cars") ||
                path.startsWith("/api/motorcycles") ||
                path.startsWith("/api/commercial") ||
                path.startsWith("/api/admin/users") ||
                path.startsWith("/api/images");
    }

    private boolean isPublicEndpoint(String path, String method) {
        if (path.startsWith("/api/auth/")) return true;
        if (method.equals("OPTIONS")) return true;
        if (method.equalsIgnoreCase("GET") && isPublicGetPath(path)) return true;
        return false;
    }
}
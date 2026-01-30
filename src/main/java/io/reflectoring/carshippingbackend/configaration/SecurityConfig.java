package io.reflectoring.carshippingbackend.configaration;

import io.reflectoring.carshippingbackend.Util.JwtUtil;
import io.reflectoring.carshippingbackend.services.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
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
@RequiredArgsConstructor
public class SecurityConfig {
@Autowired
    private final CustomUserDetailsService customUserDetailsService;
@Autowired
    private final JwtUtil jwtUtil;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(customUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public JwtTokenFilter jwtTokenFilter() {
        return new JwtTokenFilter(jwtUtil, customUserDetailsService);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("https://www.f-carshipping.com","https://f-carshipping.com"));
        configuration.setAllowedMethods(List.of("GET", "POST","PATCH", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Allow OPTIONS preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Public routes (like login/register)
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(" /api/auxiliary/**").permitAll()



                        // Public GETs but protected modifications
                        .requestMatchers(HttpMethod.GET, "/api/cars/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/motorcycles/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/vehicles/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/images/**").permitAll()


                        // Protected write operations for authenticated users
                        .requestMatchers(HttpMethod.POST, "/api/cars/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/cars/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/cars/**").authenticated()

                        .requestMatchers(HttpMethod.POST, "/api/motorcycles/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/motorcycles/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/motorcycles/**").authenticated()

                        .requestMatchers(HttpMethod.POST, "/api/vehicles/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/vehicles/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/vehicle/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/images/**").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/images/**").authenticated()





                        // Admin endpoints (extra restricted)
                        // Allow GET requests to users endpoint
                        .requestMatchers(HttpMethod.GET, "/api/admin/users", "/api/admin/users/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/motorcycles/dashboard/**").authenticated()


                        // Everything else still requires authentication
                        .anyRequest().authenticated()
                )

                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

package com.investment.tracker.config;

import com.investment.tracker.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Spring Security configuration for basic authentication.
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private final UserDetailsServiceImpl userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService)
                .passwordEncoder(passwordEncoder());
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                // CORS configuration (using existing CorsConfig bean)
                .cors()
                .and()
                // CSRF disabled for REST API
                .csrf().disable()
                // Session management - stateful sessions for basic auth
                .sessionManagement()
                    .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                    .maximumSessions(1) // Allow one session per user
                .and()
                .and()
                // Authorization rules
                .authorizeRequests()
                    // Public endpoints
                    .antMatchers("/h2-console/**").permitAll() // H2 console access
                    .antMatchers(HttpMethod.POST, "/api/auth/login").permitAll() // Login endpoint
                    .antMatchers(HttpMethod.POST, "/api/auth/register").permitAll() // Register endpoint
                    .antMatchers("/api/auth/logout").permitAll() // Logout endpoint
                    // Protected endpoints
                    .antMatchers("/api/**").authenticated() // All other API endpoints require auth
                    .anyRequest().permitAll()
                .and()
                // HTTP Basic authentication for REST API
                .httpBasic()
                .and()
                // Logout configuration
                .logout()
                    .logoutUrl("/api/auth/logout")
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID")
                    .permitAll();

        // H2 console configuration (allow frame embedding)
        http.headers().frameOptions().sameOrigin();
    }
}

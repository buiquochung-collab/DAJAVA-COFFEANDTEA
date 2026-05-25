package com.example.demo.config;

import com.example.demo.service.CustomOAuth2UserService;
import com.example.demo.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private CustomOAuth2UserService oauth2UserService;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // Phân quyền chi tiết cho Admin
                .requestMatchers("/admin/orders/**", "/admin/orders").hasAnyRole("ADMIN", "CASHIER")
                .requestMatchers("/admin/products/**", "/admin/categories/**", "/admin/inventory/**", "/admin/recipes/**").hasAnyRole("ADMIN", "STOCK_MANAGER")
                .requestMatchers("/admin/users/**", "/admin/promotions/**", "/admin/vouchers/**", "/admin/notifications/**", "/admin/settings/**").hasRole("ADMIN")
                .requestMatchers("/admin", "/admin/").hasAnyRole("ADMIN", "CASHIER", "STOCK_MANAGER")
                
                .requestMatchers("/user/**", "/cart/checkout/**", "/cart/checkout", "/cart/apply-voucher", "/cart/payment/**", "/cart/order-success/**").authenticated()
                .requestMatchers("/", "/menu", "/menu/**", "/product/**", "/category/**", "/search", "/search/**", 
                                 "/register", "/register/**", "/login", "/login/**", "/logout", "/logout/**", "/news", "/news/**", "/promotions", "/promotions/**",
                                 "/auth/**", "/privacy-policy", "/terms-of-service", "/ordering-policy", "/membership",
                                 "/css/**", "/js/**", "/images/**", "/webjars/**", "/uploads/**", "/api/**", "/error").permitAll()
                .requestMatchers("/cart", "/cart/add", "/cart/remove/**", "/cart/update").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/", false)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .defaultSuccessUrl("/", false)
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(oauth2UserService)
                )
            )
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                .logoutSuccessUrl("/")
                .deleteCookies("JSESSIONID")
                .invalidateHttpSession(true)
                .permitAll()
            )
            .rememberMe(remember -> remember
                .key("uniqueAndSecret")
                .tokenValiditySeconds(86400 * 30) // 30 days
                .userDetailsService(userDetailsService)
            )
            .exceptionHandling(ex -> ex
                .accessDeniedPage("/?error=no_permission")
            );

        return http.build();
    }
}

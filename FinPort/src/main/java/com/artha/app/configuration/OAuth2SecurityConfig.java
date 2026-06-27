package com.artha.app.configuration;

import com.artha.app.security.OAuth2SuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * OAuth2 (Google) additions to the security chain. Only loaded when a
 * {@link ClientRegistrationRepository} is on the context (i.e. when Google
 * client credentials are configured). In dev/tests without OAuth2 properties,
 * the base {@link SecurityConfig} chain handles everything.
 */
@Configuration
@org.springframework.boot.autoconfigure.condition.ConditionalOnBean(ClientRegistrationRepository.class)
@EnableWebSecurity
public class OAuth2SecurityConfig {

    @Bean
    public SecurityFilterChain oauth2SecurityFilterChain(HttpSecurity http,
                                                         OAuth2SuccessHandler oAuth2SuccessHandler) throws Exception {
        http
                .securityMatcher("/oauth2/**", "/login/oauth2/**")
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll())
                .oauth2Login(oauth -> oauth.successHandler(oAuth2SuccessHandler));
        return http.build();
    }
}
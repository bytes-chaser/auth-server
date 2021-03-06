package com.goldenebrg.authserver.security;

import com.goldenebrg.authserver.index.ServiceName;
import com.goldenebrg.authserver.services.ServerConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.sql.DataSource;
import java.util.List;


@Configuration
@EnableWebSecurity
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {


    private final UserDetailsService userDetailsService;
    private final DataSource dataSource;
    private final ServerConfigurationService serverConfigurationService;

    @Autowired
    SecurityConfiguration(@Qualifier(ServiceName.USER_DETAIL_SERVICE) UserDetailsService userDetailsService,
                          DataSource dataSource, ServerConfigurationService serverConfigurationService) {
        this.userDetailsService = userDetailsService;
        this.dataSource = dataSource;
        this.serverConfigurationService = serverConfigurationService;
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Override
    protected void configure(final AuthenticationManagerBuilder auth) throws Exception{
        auth.userDetailsService(userDetailsService)
                .passwordEncoder(passwordEncoder())
                .and()
                .authenticationProvider(authenticationProvider())
                .jdbcAuthentication()
                .dataSource(dataSource);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    @Override
    protected void configure(final HttpSecurity http) throws Exception {
        http
                .csrf().ignoringAntMatchers("/auth/**")
                .and()
                .cors()
                .and()
                .authorizeRequests()
                .antMatchers("/admin/**").hasRole("ADMIN")
                .antMatchers("/user", "/services").hasAnyRole("ADMIN", "USER")
                .antMatchers("/**").permitAll()
                .and()
                .formLogin()
                .loginPage("/")
                .defaultSuccessUrl("/", true)
                .loginProcessingUrl("/perform_login")
                .usernameParameter("login")
                .passwordParameter("password")
                .failureHandler(authenticationFailureHandler())
                .and()
                .logout().
                logoutUrl("/appLogout").
                logoutSuccessUrl("/")
                .and()
                .headers()
                .xssProtection()
                .and()
                .contentSecurityPolicy("script-src 'self'")
        ;
    }

    @Bean
    public AuthenticationFailureHandler authenticationFailureHandler() {
        return new AuthFailedHandler();
    }


    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        final CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowCredentials(true);

        List<String> corsOrigins = serverConfigurationService.getCorsOrigins();
        if (!corsOrigins.isEmpty())
            configuration.setAllowedOrigins(corsOrigins);

        List<String> corsMethods = serverConfigurationService.getCorsMethods();
        if (!corsMethods.isEmpty())
            configuration.setAllowedMethods(corsMethods);


        List<String> corsHeaders = serverConfigurationService.getCorsHeaders();
        if (!corsHeaders.isEmpty())
            configuration.setAllowedHeaders(corsHeaders);

        final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }


    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

}

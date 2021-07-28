package uk.gov.hmcts.reform.wacaseeventhandler.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import uk.gov.hmcts.reform.authorisation.filters.ServiceAuthFilter;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;


@Configuration
@ConfigurationProperties(prefix = "security")
@EnableWebSecurity
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    private final List<String> anonymousPaths = new ArrayList<>();
    @Autowired
    private ServiceAuthFilter serviceAuthFilter;

    public List<String> getAnonymousPaths() {
        return anonymousPaths;
    }

    @Override
    public void configure(WebSecurity web) {
        web.ignoring().mvcMatchers(
            anonymousPaths.toArray(String[]::new)
        );
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .addFilterBefore(serviceAuthFilter, AbstractPreAuthenticatedProcessingFilter.class)
            .sessionManagement().sessionCreationPolicy(STATELESS)
            .and()
            .httpBasic().disable()
            .formLogin().disable()
            .logout().disable()
            .csrf().disable();
    }
}

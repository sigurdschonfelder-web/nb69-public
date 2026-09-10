package sigurdws.NB69.house;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean Clock clock() { return Clock.system(ZoneId.of("Europe/Oslo")); }
    @Bean PasswordEncoder passwords() { return new BCryptPasswordEncoder(12); }
    @Bean UserDetailsService users(JdbcTemplate jdbc) {
        return username -> jdbc.query("select * from nb69_users where username = ?", (rs, row) -> {
            String hash = rs.getString("password_hash");
            return User.withUsername(rs.getString("username"))
                .password(hash == null ? "!disabled" : hash)
                .disabled(hash == null)
                .roles(rs.getBoolean("admin") ? "ADMIN" : "RESIDENT").build();
        }, username).stream().findFirst().orElseThrow(() -> new UsernameNotFoundException("Unknown user"));
    }
    @Bean org.springframework.security.core.session.SessionRegistry sessions() { return new org.springframework.security.core.session.SessionRegistryImpl(); }
    @Bean org.springframework.security.web.session.HttpSessionEventPublisher sessionEvents() { return new org.springframework.security.web.session.HttpSessionEventPublisher(); }
    @Bean SecurityFilterChain security(HttpSecurity http, org.springframework.security.core.session.SessionRegistry sessions) throws Exception {
        return http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/prikker", "/api/prikker/**").denyAll()
                .requestMatchers("/api/csrf", "/api/login", "/api/activate", "/error").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .sessionManagement(session -> session.maximumSessions(-1).sessionRegistry(sessions)
                .expiredSessionStrategy(event -> event.getResponse().setStatus(401)))
            .addFilterBefore(new AuthenticationThrottle(), org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
            .formLogin(form -> form.loginProcessingUrl("/api/login")
                .successHandler((req, res, auth) -> res.setStatus(204))
                .failureHandler((req, res, err) -> res.setStatus(401)))
            .logout(logout -> logout.logoutUrl("/api/logout").deleteCookies("JSESSIONID")
                .logoutSuccessHandler((req, res, auth) -> res.setStatus(204)))
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((req, res, err) -> res.setStatus(401))
                .accessDeniedHandler((req, res, err) -> res.setStatus(403)))
            .requestCache(cache -> cache.disable())
            .build();
    }
}

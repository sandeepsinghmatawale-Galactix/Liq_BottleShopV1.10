package com.barinventory.admin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import com.barinventory.admin.service.UserDetailsServiceImpl;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

	private final UserDetailsServiceImpl userDetailsService;

	// =================================================================================
	// PASSWORD ENCODER
	// =================================================================================
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(10);
	}

	// =================================================================================
	// AUTH MANAGER
	// =================================================================================
	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
		return config.getAuthenticationManager();
	}

	// =================================================================================
	// AUTH PROVIDER
	// =================================================================================
	@Bean
	public DaoAuthenticationProvider authenticationProvider() {
		DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
		authProvider.setUserDetailsService(userDetailsService);
		authProvider.setPasswordEncoder(passwordEncoder());
		return authProvider;
	}

	// =================================================================================
	// CUSTOMER SECURITY CHAIN (Order 1 - Higher Priority)
	// =================================================================================
	@Bean
	@Order(1)
	public SecurityFilterChain customerSecurityFilterChain(HttpSecurity http) throws Exception {
		http.securityMatcher("/customer/**")

				.csrf(csrf -> csrf.ignoringRequestMatchers("/customer/api/**"))

				.authenticationProvider(authenticationProvider())

				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/customer/login", "/customer/register").permitAll()
						.requestMatchers("/select-bar").authenticated()
						.requestMatchers("/customer/api/**").hasRole("CUSTOMER")
						.requestMatchers("/customer/**").hasRole("CUSTOMER")
						.anyRequest().authenticated())

				.formLogin(form -> form.loginPage("/customer/login")
						.loginProcessingUrl("/customer/login")
						.usernameParameter("email")
						.passwordParameter("password")
						.successHandler((request, response, authentication) -> {
							response.sendRedirect("/customer/dashboard");
						})
						.failureUrl("/customer/login?error=true")
						.permitAll())

				.logout(logout -> logout.logoutRequestMatcher(new AntPathRequestMatcher("/customer/logout"))
						.logoutSuccessUrl("/customer/login?logout=true")
						.invalidateHttpSession(true)
						.deleteCookies("JSESSIONID")
						.permitAll())

				.sessionManagement(session -> session.maximumSessions(3)
						.maxSessionsPreventsLogin(false))

				.exceptionHandling(ex -> ex.accessDeniedHandler((request, response, e) -> {
					response.sendRedirect("/customer/login?error=403");
				}));

		return http.build();
	}


	// =================================================================================
	// ADMIN/STAFF SECURITY CHAIN (Order 2 - Lower Priority)
	// =================================================================================
	@Bean
	@Order(2)
	public SecurityFilterChain adminSecurityFilterChain(HttpSecurity http) throws Exception {
		http
				// =================================================================================
				// CSRF CONFIG
				// =================================================================================
				.csrf(csrf -> csrf.ignoringRequestMatchers("/api/**", "/sessions/wells/*/save"))

				// =================================================================================
				// AUTH PROVIDER
				// =================================================================================
				.authenticationProvider(authenticationProvider())

				// =================================================================================
				// AUTHORIZATION RULES
				// =================================================================================
				.authorizeHttpRequests(auth -> auth

					    // ✅ Static
					    .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()

					    // ✅ Public
					    .requestMatchers("/login", "/error").permitAll()

					    // ✅ Bar selection (required after login)
					    .requestMatchers("/select-bar", "/select-bar/**").authenticated()

					    // ❌ Block old dashboard completely
					    .requestMatchers("/dashboard").authenticated()

					    // ✅ Segregated dashboards
					    .requestMatchers("/admin/**").hasRole("ADMIN")     // GLOBAL ADMIN
					    .requestMatchers("/owner/**").hasAnyRole("BAR_OWNER")
					    .requestMatchers("/staff/**").hasAnyRole("BAR_STAFF")

					    // ✅ Business modules (require login + bar context)
					    .requestMatchers("/bars/**", "/sessions/**", "/stockroom/**", "/inventory/**", "/api/**")
					    .authenticated()

					    .anyRequest().authenticated()
					)

				// =================================================================================
				// LOGIN CONFIG
				// =================================================================================
				.formLogin(form -> form.loginPage("/login").loginProcessingUrl("/login").usernameParameter("email")
						.passwordParameter("password")

						// ✅ FINAL SUCCESS HANDLER (MULTI-BAR FLOW)
						.successHandler((request, response, authentication) -> {
							com.barinventory.admin.entity.User user = (com.barinventory.admin.entity.User) authentication
									.getPrincipal();

							// 👉 If system ADMIN → go directly
							if (user.getRole().name().equals("ADMIN")) {
							    response.sendRedirect("/admin/dashboard");
							    return;
							}

							// 👉 Otherwise → select bar
							response.sendRedirect("/select-bar");
						})

						.failureUrl("/login?error=true").permitAll())

				// =================================================================================
				// LOGOUT CONFIG
				// =================================================================================
				.logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/login?logout=true")
						.invalidateHttpSession(true).deleteCookies("JSESSIONID").permitAll())

				// =================================================================================
				// SESSION MANAGEMENT
				// =================================================================================
				.sessionManagement(session -> session.maximumSessions(1) // Strict for admin/staff
						.maxSessionsPreventsLogin(false))

				// =================================================================================
				// EXCEPTION HANDLING
				// =================================================================================
				.exceptionHandling(ex -> ex.accessDeniedHandler((request, response, e) -> {
				    var auth = org.springframework.security.core.context.SecurityContextHolder
				            .getContext().getAuthentication();

				    System.out.println("403 URI = " + request.getRequestURI());
				    System.out.println("User = " + (auth != null ? auth.getName() : "anonymous"));
				    System.out.println("Authorities = " + (auth != null ? auth.getAuthorities() : "none"));

				    response.sendRedirect("/login?error=403");
				}));


		return http.build();
	}
}

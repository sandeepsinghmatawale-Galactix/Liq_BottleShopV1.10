package com.barinventory.admin.service;

import com.barinventory.admin.entity.User;
import com.barinventory.admin.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        log.info("Attempting to load user by email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("User not found with email: {}", email);
                    return new UsernameNotFoundException("User not found");
                });

        if (!user.getActive()) {
            log.warn("Disabled account login attempt: {}", email);
            throw new UsernameNotFoundException("Account is disabled");
        }

        // ✅ SAFE LOGGING (NO LAZY CALLS)
        log.info("User loaded: email={}, role={}, active={}", 
                user.getEmail(), 
                user.getRole(), 
                user.getActive());

        return user; // ✅ DO NOT TOUCH RELATIONS HERE
    }
}
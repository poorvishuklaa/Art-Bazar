package com.artbazaar.config;

import com.artbazaar.entity.Role;
import com.artbazaar.entity.User;
import com.artbazaar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * Admins cannot register through the API. Setting ADMIN_EMAIL and ADMIN_PASSWORD creates the
 * first admin on startup (only if no account with that email exists yet).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAccountInitializer implements ApplicationRunner {

    private final AppProperties props;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        AppProperties.Admin admin = props.admin();
        if (admin == null || !StringUtils.hasText(admin.email()) || !StringUtils.hasText(admin.password())) {
            return;
        }
        String email = admin.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            return;
        }
        if (admin.password().length() < 8) {
            log.warn("ADMIN_PASSWORD is shorter than 8 characters; admin account not created");
            return;
        }
        User user = new User();
        user.setName(StringUtils.hasText(admin.name()) ? admin.name().trim() : "Admin");
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(admin.password()));
        user.setRole(Role.ADMIN);
        userRepository.save(user);
        log.info("Created admin account {}", email);
    }
}

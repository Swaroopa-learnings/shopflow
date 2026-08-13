package com.shopflow.auth.service;

import com.shopflow.auth.domain.AppUser;
import com.shopflow.auth.repo.UserRepository;
import com.shopflow.auth.security.JwtService;
import com.shopflow.auth.web.dto.LoginRequest;
import com.shopflow.auth.web.dto.RegisterRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registration and login. */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public String register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered");
        }
        AppUser user = new AppUser(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.fullName());
        userRepository.save(user);
        return jwtService.generateToken(user);
    }

    @Transactional(readOnly = true)
    public String login(LoginRequest request) {
        AppUser user = userRepository.findByEmail(request.email())
                // Same message whether the user or the password is wrong, so
                // registered emails can't be probed.
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        return jwtService.generateToken(user);
    }
}

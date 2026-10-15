package com.parking.backend.auth;

import com.parking.backend.common.ConflictException;
import com.parking.backend.common.UnauthorizedException;
import com.parking.backend.user.Role;
import com.parking.backend.user.User;
import com.parking.backend.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new ConflictException("Email " + email + " is already registered");
        }
        User u = new User();
        u.setName(req.name().trim());
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(req.password()));
        u.setRole(Role.DRIVER); // everyone registers as a driver; managers are seeded
        u = users.save(u);
        return respond(u);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User u = users.findByEmail(req.email().trim().toLowerCase())
                .filter(x -> encoder.matches(req.password(), x.getPasswordHash()))
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
        return respond(u);
    }

    private AuthResponse respond(User u) {
        String token = jwt.generate(u.getId(), u.getEmail(), u.getRole().name());
        return new AuthResponse(token, u.getName(), u.getEmail(), u.getRole().name());
    }
}
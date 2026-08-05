package com.accessparks.asanaapp.controller;

import com.accessparks.asanaapp.dto.AuthDtos.LoginRequest;
import com.accessparks.asanaapp.dto.AuthDtos.LoginResponse;
import com.accessparks.asanaapp.model.AppUser;
import com.accessparks.asanaapp.repository.AppUserRepository;
import com.accessparks.asanaapp.security.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final AppUserRepository appUserRepository;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).body("Invalid email or password");
        }

        AppUser user = appUserRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(); // shouldn't happen if authentication succeeded

        user.setLastLoginAt(LocalDateTime.now());
        appUserRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        return ResponseEntity.ok(new LoginResponse(token, user.getEmail(), user.getRole().name(), user.getFullName()));
    }
}

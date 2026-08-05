package com.accessparks.asanaapp.controller;

import com.accessparks.asanaapp.dto.AuthDtos.CreateUserRequest;
import com.accessparks.asanaapp.dto.AuthDtos.UpdateUserRequest;
import com.accessparks.asanaapp.model.AppUser;
import com.accessparks.asanaapp.model.Role;
import com.accessparks.asanaapp.repository.AppUserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Only SUPER_USER may create or modify users/profiles - enforced both at the
// URL level in SecurityConfig AND here via @PreAuthorize as defense in depth.
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_USER')")
public class UserController {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public List<AppUser> listUsers() {
        return appUserRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserRequest request) {
        if (appUserRepository.existsByEmailIgnoreCase(request.email())) {
            return ResponseEntity.badRequest().body("A user with that email already exists");
        }

        AppUser user = new AppUser();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setRole(Role.valueOf(request.role()));
        user.setEnabled(true);

        appUserRepository.save(user);
        user.setPasswordHash(null); // don't echo the hash back
        return ResponseEntity.ok(user);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody UpdateUserRequest request) {
        AppUser user = appUserRepository.findById(id)
                .orElse(null);
        if (user == null) return ResponseEntity.notFound().build();

        if (request.fullName() != null) user.setFullName(request.fullName());
        if (request.role() != null) user.setRole(Role.valueOf(request.role()));
        if (request.enabled() != null) user.setEnabled(request.enabled());
        if (request.newPassword() != null && !request.newPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        }

        appUserRepository.save(user);
        user.setPasswordHash(null);
        return ResponseEntity.ok(user);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        appUserRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}

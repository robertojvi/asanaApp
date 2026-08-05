package com.accessparks.asanaapp.security;

import com.accessparks.asanaapp.model.AppUser;
import com.accessparks.asanaapp.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final AppUserRepository appUserRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        AppUser user = appUserRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("No user with email " + email));

        if (!user.isEnabled()) {
            throw new UsernameNotFoundException("Account disabled: " + email);
        }

        return User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                // Spring Security expects "ROLE_X" prefix for hasRole("X") checks
                .authorities(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
                .build();
    }
}

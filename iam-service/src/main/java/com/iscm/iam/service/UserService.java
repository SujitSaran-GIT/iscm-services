package com.iscm.iam.service;

import com.iscm.iam.model.User;
import com.iscm.iam.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmailWithRoles(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return buildUserDetails(user);
    }

    @Transactional(readOnly = true)
    public UserDetails loadUserByUserId(String userId) {
        User user = userRepository.findByIdWithRoles(UUID.fromString(userId))
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));

        return buildUserDetails(user);
    }

    private UserDetails buildUserDetails(User user) {
        Collection<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                .collect(Collectors.toList());

        // Add permissions as authorities
        user.getRoles().forEach(role -> 
            role.getPermissions().forEach(permission -> 
                authorities.add(new SimpleGrantedAuthority(permission.getCode()))
            )
        );

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPasswordHash(),
                user.getIsActive(),
                true, // account non-expired
                true, // credentials non-expired
                user.isAccountNonLocked(),
                authorities
        );
    }

    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmailWithRoles(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }

    @Transactional(readOnly = true)
    public User findById(UUID id) {
        return userRepository.findByIdWithRoles(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
}
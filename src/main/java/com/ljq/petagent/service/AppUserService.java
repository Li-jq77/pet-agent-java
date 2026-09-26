package com.ljq.petagent.service;

import com.ljq.petagent.entity.AppUser;
import com.ljq.petagent.repository.AppUserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class AppUserService implements UserDetailsService {

    private final AppUserRepository userRepository;

    public AppUserService(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser user = findByUsername(username);
        return new User(
            user.getUsername(),
            user.getPassword(),
            Boolean.TRUE.equals(user.getActive()),
            true,
            true,
            true,
            Collections.emptyList()
        );
    }

    public AppUser findByUsername(String username) {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));
    }

    public boolean usernameExists(String username) {
        return userRepository.existsByUsername(username);
    }
}

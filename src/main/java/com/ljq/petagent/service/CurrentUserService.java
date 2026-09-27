package com.ljq.petagent.service;

import com.ljq.petagent.entity.AppUser;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    private final AppUserService appUserService;

    public CurrentUserService(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    public AppUser get(Authentication authentication) {
        if (authentication == null
            || !authentication.isAuthenticated()
            || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return appUserService.findByUsername(authentication.getName());
    }

    public AppUser require(Authentication authentication) {
        AppUser user = get(authentication);
        if (user == null) {
            throw new IllegalStateException("需要登录");
        }
        return user;
    }
}

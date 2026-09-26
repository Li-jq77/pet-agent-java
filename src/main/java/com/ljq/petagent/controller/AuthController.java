package com.ljq.petagent.controller;

import com.ljq.petagent.entity.AppUser;
import com.ljq.petagent.repository.AppUserRepository;
import com.ljq.petagent.service.AppUserService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@Controller
public class AuthController {

    private final AppUserRepository userRepository;
    private final AppUserService userService;
    private final PasswordEncoder passwordEncoder;

    public AuthController(
        AppUserRepository userRepository,
        AppUserService userService,
        PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/login")
    public String login(Authentication authentication, Model model) {
        if (authentication != null
            && authentication.isAuthenticated()
            && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:/";
        }
        return "pets/login";
    }

    @GetMapping("/register")
    public String registerForm(Authentication authentication) {
        if (authentication != null
            && authentication.isAuthenticated()
            && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:/";
        }
        return "pets/register";
    }

    @PostMapping("/register")
    public String register(
        @RequestParam String username,
        @RequestParam String password1,
        @RequestParam String password2,
        Model model,
        HttpServletRequest request
    ) {
        if (username == null || username.trim().isEmpty()) {
            model.addAttribute("registerError", "请输入用户名。");
            return "pets/register";
        }
        if (!password1.equals(password2)) {
            model.addAttribute("registerError", "两次输入的密码不一致。");
            return "pets/register";
        }
        if (password1.trim().isEmpty()) {
            model.addAttribute("registerError", "请输入密码。");
            return "pets/register";
        }
        if (userService.usernameExists(username)) {
            model.addAttribute("registerError", "该用户名已被使用。");
            return "pets/register";
        }

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password1));
        user.setFirstName("");
        user.setLastName("");
        user.setEmail("");
        user.setSuperuser(false);
        user.setStaff(false);
        user.setActive(true);
        LocalDateTime now = LocalDateTime.now();
        user.setDateJoined(now);
        user.setLastLogin(now);
        userRepository.save(user);

        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(username, null, java.util.Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        request.getSession().setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
        return "redirect:/";
    }
}

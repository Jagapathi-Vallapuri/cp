package com.project.code_judge.Controller;

import com.project.code_judge.Dto.LoginResponse;
import com.project.code_judge.Dto.RegisterUser;
import com.project.code_judge.Dto.UserLogin;
import com.project.code_judge.Dto.UserResponse;
import com.project.code_judge.Entity.User;
import com.project.code_judge.Repository.UserRepository;
import com.project.code_judge.Service.AuthService;
import com.project.code_judge.Service.OAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import jakarta.transaction.Transactional;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final OAuthService oAuthService;

    @PostMapping("/register")
    @Transactional
    public ResponseEntity<UserResponse> registerUser(@RequestBody RegisterUser dto, HttpServletRequest request){
        UserResponse userResponse = authService.registerUser(dto);
        Authentication auth = authService.authenticate(
                new UserLogin(dto.getEmail(), dto.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        request.getSession(true).setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                SecurityContextHolder.getContext()
        );
        return ResponseEntity.ok(userResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody UserLogin dto, HttpServletRequest request){
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        HttpSession session = request.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, SecurityContextHolder.getContext());

        User user = userRepository.findByEmail(dto.getEmail()).orElseThrow();
        return ResponseEntity.ok(new LoginResponse(user.getUsername(), user.getEmail()));
    }

    @PostMapping("/google")
    public ResponseEntity<UserResponse> googleLogin(@RequestBody Map<String, String> body, HttpServletRequest request){
        String token = body.get("token");

        User user  = oAuthService.googleLogin(token);

        Collection<GrantedAuthority> authorities = user.getRole() != null
            ? List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
            : List.of(new SimpleGrantedAuthority("ROLE_USER"));

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(user.getEmail(), null, authorities);

        SecurityContextHolder.getContext().setAuthentication(auth);

        request.getSession(true).setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                SecurityContextHolder.getContext()
        );

        return ResponseEntity.ok(new UserResponse(user.getUsername(), user.getEmail()));
    }


    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if(session != null){
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok().build();
    }
}

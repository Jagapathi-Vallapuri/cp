package com.project.code_judge.Controller;

import com.project.code_judge.Dto.LoginResponse;
import com.project.code_judge.Dto.RegisterUser;
import com.project.code_judge.Dto.UserLogin;
import com.project.code_judge.Dto.UserResponse;
import com.project.code_judge.Entity.User;
import com.project.code_judge.Repository.UserRepository;
import com.project.code_judge.Service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/register")
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

package com.project.code_judge.Controller;

import com.project.code_judge.Dto.LoginResponse;
import com.project.code_judge.Dto.RegisterUser;
import com.project.code_judge.Dto.UserLogin;
import com.project.code_judge.Dto.UserResponse;
import com.project.code_judge.Service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> registerUser(@RequestBody RegisterUser dto){
        UserResponse userResponse = authService.registerUser(dto);
        return ResponseEntity.ok(userResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody UserLogin dto){
        LoginResponse loginResponse = authService.login(dto);
        return ResponseEntity.ok(loginResponse);
    }
}

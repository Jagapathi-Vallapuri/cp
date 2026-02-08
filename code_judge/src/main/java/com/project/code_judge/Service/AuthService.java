package com.project.code_judge.Service;

import com.project.code_judge.Dto.LoginResponse;
import com.project.code_judge.Dto.RegisterUser;
import com.project.code_judge.Dto.UserLogin;
import com.project.code_judge.Dto.UserResponse;
import com.project.code_judge.Entity.User;
import com.project.code_judge.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserResponse registerUser(RegisterUser dto){
        if(userRepository.findByEmail(dto.getEmail()).isPresent()){
            throw new IllegalArgumentException("User already registered with the mail");
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        userRepository.save(user);
        return mapper(user);
    }

    public LoginResponse login(UserLogin dto){
        User user = userRepository.findByEmail(dto.getEmail()).orElseThrow(() -> new IllegalArgumentException("Invalid email"));
        if(!passwordEncoder.matches(dto.getPassword(), user.getPassword())){
            throw new IllegalArgumentException("Invalid password");
        }
        String username = user.getUsername();
        Map<String, Object> claims = Map.of();
        String token = jwtService.buildToken(claims, username);
        LoginResponse loginResponse = new LoginResponse();
        loginResponse.setUsername(user.getUsername());
        loginResponse.setToken(token);
        return loginResponse;
    }


    public UserResponse mapper(User user){
        UserResponse userResponse = new UserResponse();
        userResponse.setUsername(user.getUsername());
        userResponse.setEmail(user.getEmail());
        return userResponse;
    }
}

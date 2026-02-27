package com.project.code_judge.Service;

import com.project.code_judge.Dto.RegisterUser;
import com.project.code_judge.Dto.UserLogin;
import com.project.code_judge.Dto.UserResponse;
import com.project.code_judge.Entity.AuthProvider;
import com.project.code_judge.Entity.User;
import com.project.code_judge.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public UserResponse registerUser(RegisterUser dto){
        if(userRepository.findByEmail(dto.getEmail()).isPresent()){
            throw new IllegalArgumentException("User already registered with the mail");
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setProvider(AuthProvider.LOCAL);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        userRepository.save(user);
        return mapper(user);
    }

    public Authentication authenticate(UserLogin dto){
        return authenticationManager.authenticate( new UsernamePasswordAuthenticationToken( dto.getEmail(), dto.getPassword()));
    }


    public UserResponse mapper(User user){
        return new UserResponse(user.getUsername(), user.getEmail());
    }
}

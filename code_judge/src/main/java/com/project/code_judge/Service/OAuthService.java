package com.project.code_judge.Service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.project.code_judge.Entity.AuthProvider;
import com.project.code_judge.Entity.User;
import com.project.code_judge.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OAuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    public User googleLogin(String token){
        try {
            String tokenPrefix = token == null ? "null" : token.substring(0, Math.min(12, token.length()));
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(clientId))
                    .build();
            GoogleIdToken gToken = verifier.verify(token);
            if ( gToken == null) throw new IllegalArgumentException("Invalid Google Token");

            GoogleIdToken.Payload payload = gToken.getPayload();
            String email = payload.getEmail();
            String googleId = payload.getSubject();
            String name = (String)payload.get("name");

            return userRepository.findByEmail(email).map(existingUser -> {
                if(existingUser.getProvider() == AuthProvider.LOCAL){
                    existingUser.setProvider(AuthProvider.GOOGLE);
                    existingUser.setProviderId(googleId);
                    userRepository.save(existingUser);
                }
                return existingUser;
            }).orElseGet( () -> {
                User newUser =  new User();
                newUser.setEmail(email);
                newUser.setProvider(AuthProvider.GOOGLE);
                newUser.setProviderId(googleId);
                newUser.setUsername(name);
                newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
                return userRepository.save(newUser);
            });

        } catch (Exception e) {
            throw new RuntimeException("Google Login Failed: " + e.getMessage());
        }
    }

}

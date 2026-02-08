package com.project.code_judge.Dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class RegisterUser {
    @NotNull(message = "Username cannot be null")
    @Length(min = 5, max = 64, message = "Username must be between 5 and 64 characters")
    private String username;

    @NotNull(message = "Email cannot be null")
    @Email(message = "Email should be valid")
    private String email;

    @NotNull(message = "Password cannot be null")
    @Length(min = 6, message = "Password must be greater than 6 characters")
    private String password;
}

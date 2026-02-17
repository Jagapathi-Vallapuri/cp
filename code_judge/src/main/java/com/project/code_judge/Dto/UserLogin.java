package com.project.code_judge.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserLogin {
    private String email;
    private String password;
}

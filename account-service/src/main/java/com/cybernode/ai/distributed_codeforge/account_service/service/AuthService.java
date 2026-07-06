package com.cybernode.ai.distributed_codeforge.account_service.service;


import com.cybernode.ai.distributed_codeforge.account_service.dto.auth.AuthResponse;
import com.cybernode.ai.distributed_codeforge.account_service.dto.auth.LoginRequest;
import com.cybernode.ai.distributed_codeforge.account_service.dto.auth.SignUpRequest;

public interface AuthService {
     AuthResponse signup(SignUpRequest request);
     AuthResponse login(LoginRequest request);
     void forgotPassword(String email);
     void resetPassword(String token, String newPassword);
}

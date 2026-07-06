package com.cybernode.ai.distributed_codeforge.account_service.controller;


import com.cybernode.ai.distributed_codeforge.account_service.dto.auth.AuthResponse;
import com.cybernode.ai.distributed_codeforge.account_service.dto.auth.LoginRequest;
import com.cybernode.ai.distributed_codeforge.account_service.dto.auth.SignUpRequest;
import com.cybernode.ai.distributed_codeforge.account_service.dto.auth.ForgotPasswordRequest;
import com.cybernode.ai.distributed_codeforge.account_service.dto.auth.ResetPasswordRequest;
import com.cybernode.ai.distributed_codeforge.account_service.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@RequestBody @Valid SignUpRequest request){
        return ResponseEntity.ok(authService.signup(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest request){
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request) {
        authService.forgotPassword(request.email());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        authService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok().build();
    }
}

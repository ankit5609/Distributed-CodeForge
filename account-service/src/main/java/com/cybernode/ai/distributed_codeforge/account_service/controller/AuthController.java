package com.cybernode.ai.distributed_codeforge.account_service.controller;


import com.cybernode.ai.distributed_codeforge.account_service.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@RequestBody SignUpRequest request){
        return ResponseEntity.ok(authService.signup(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request){
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile(){
        Long userId=1L;
        return ResponseEntity.ok(userService.getProfile(userId));
    }
}

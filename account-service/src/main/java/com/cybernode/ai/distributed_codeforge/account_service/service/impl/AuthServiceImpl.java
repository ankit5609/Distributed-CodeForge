package com.cybernode.ai.distributed_codeforge.account_service.service.impl;

import com.cybernode.ai.distributed_codeforge.account_service.dto.auth.AuthResponse;
import com.cybernode.ai.distributed_codeforge.account_service.dto.auth.LoginRequest;
import com.cybernode.ai.distributed_codeforge.account_service.dto.auth.SignUpRequest;
import com.cybernode.ai.distributed_codeforge.account_service.entity.User;
import com.cybernode.ai.distributed_codeforge.account_service.mapper.UserMapper;
import com.cybernode.ai.distributed_codeforge.account_service.repository.UserRepository;
import com.cybernode.ai.distributed_codeforge.account_service.service.AuthService;
import com.cybernode.ai.distributed_codeforge.common_lib.error.BadRequestException;
import com.cybernode.ai.distributed_codeforge.common_lib.security.AuthUtil;
import com.cybernode.ai.distributed_codeforge.common_lib.security.JwtUserPrincipal;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.cybernode.ai.distributed_codeforge.common_lib.error.ResourceNotFoundException;

import java.time.Instant;
import java.time.Duration;
import java.util.UUID;
import java.util.ArrayList;

@Service
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    UserRepository userRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;
    AuthUtil authUtil;
    AuthenticationManager authenticationManager;
    JavaMailSender mailSender;

    @NonFinal
    @Value("${app.mail.from}")
    String mailFrom;

    @Override
    public AuthResponse signup(SignUpRequest request) {
        userRepository.findByUsername(request.username()).ifPresent(
                user ->{
                    throw new BadRequestException("User already exists with username: "+request.username());
                }
        );
        User user=userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user=userRepository.save(user);
        JwtUserPrincipal jwtUserPrincipal = new JwtUserPrincipal(user.getId(), user.getName(),
                user.getUsername(), null,  new ArrayList<>());

        String token = authUtil.generateAccessToken(jwtUserPrincipal);
        return new AuthResponse(token, userMapper.toUserProfileResponse(jwtUserPrincipal));
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        Authentication authentication=authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(),request.password())
        );
        JwtUserPrincipal user = (JwtUserPrincipal) authentication.getPrincipal();
        String token = authUtil.generateAccessToken(user);

        return new AuthResponse(token, userMapper.toUserProfileResponse(user));
    }

    @Override
    public void forgotPassword(String email) {
        User user = userRepository.findByUsernameIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));

        String token = UUID.randomUUID().toString();
        user.setResetPasswordToken(token);
        user.setResetPasswordTokenExpiresAt(Instant.now().plus(Duration.ofMinutes(15)));
        userRepository.save(user);

        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setFrom(mailFrom);
            mailMessage.setTo(email);
            mailMessage.setSubject("Password Reset Request");
            mailMessage.setText("To reset your password, please use the following reset token:\n\n" + token +
                    "\n\nThis token will expire in 15 minutes.");
            mailSender.send(mailMessage);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }

    @Override
    public void resetPassword(String token, String newPassword) {
        User user = userRepository.findByResetPasswordToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid or expired password reset token"));

        if (user.getResetPasswordTokenExpiresAt() == null || user.getResetPasswordTokenExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Password reset token has expired");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiresAt(null);
        userRepository.save(user);
    }
}

package com.cybernode.ai.distributed_codeforge.account_service.mapper;


import com.cybernode.ai.distributed_codeforge.account_service.dto.auth.SignUpRequest;
import com.cybernode.ai.distributed_codeforge.account_service.dto.auth.UserProfileResponse;
import com.cybernode.ai.distributed_codeforge.account_service.entity.User;
import com.cybernode.ai.distributed_codeforge.common_lib.dto.UserDto;
import com.cybernode.ai.distributed_codeforge.common_lib.security.JwtUserPrincipal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    User toEntity(SignUpRequest signupRequest);

    UserDto toUserDto(User user);

    @Mapping(source = "userId", target = "id")
    UserProfileResponse toUserProfileResponse(JwtUserPrincipal user);



}

package com.cybernode.ai.distributed_codeforge.account_service.mapper;


import com.cybernode.ai.distributed_codeforge.account_service.dto.auth.SignUpRequest;
import com.cybernode.ai.distributed_codeforge.account_service.dto.auth.UserProfileResponse;
import com.cybernode.ai.distributed_codeforge.account_service.entity.User;
import com.cybernode.ai.distributed_codeforge.common_lib.dto.UserDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    User toEntity(SignUpRequest signupRequest);

    UserProfileResponse toUserProfileResponse(User user);

    UserDto toUserDto(User user);

}

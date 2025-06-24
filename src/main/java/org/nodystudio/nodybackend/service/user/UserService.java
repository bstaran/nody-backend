package org.nodystudio.nodybackend.service.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nodystudio.nodybackend.domain.user.User;
import org.nodystudio.nodybackend.dto.user.UpdateNicknameRequestDto;
import org.nodystudio.nodybackend.dto.user.UserDetailResponseDto;
import org.nodystudio.nodybackend.exception.custom.UserNotFoundException;
import org.nodystudio.nodybackend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserDetailResponseDto getCurrentUser(String userId) {
        User user = userRepository.findById(Long.parseLong(userId))
                .orElseThrow(() -> UserNotFoundException.byUserId(userId));
        
        return UserDetailResponseDto.fromEntity(user);
    }

    @Transactional
    public UserDetailResponseDto updateNickname(String userId, UpdateNicknameRequestDto requestDto) {
        User user = userRepository.findById(Long.parseLong(userId))
                .orElseThrow(() -> UserNotFoundException.byUserId(userId));

        log.info("사용자 닉네임 변경: userId={}, 기존닉네임={}, 새닉네임={}", 
                userId, user.getNickname(), requestDto.getNickname());

        user.updateNickname(requestDto.getNickname());
        
        return UserDetailResponseDto.fromEntity(user);
    }
}
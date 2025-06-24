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
        User user = findUserById(userId);
        return UserDetailResponseDto.fromEntity(user);
    }

    @Transactional
    public UserDetailResponseDto updateNickname(String userId, UpdateNicknameRequestDto requestDto) {
        User user = findUserById(userId);

        log.info("사용자 닉네임 변경: userId={}, 기존닉네임={}, 새닉네임={}", 
                userId, user.getNickname(), requestDto.getNickname());

        user.updateNickname(requestDto.getNickname());
        
        return UserDetailResponseDto.fromEntity(user);
    }

    /**
     * 사용자 ID로 사용자를 조회합니다.
     * 
     * @param userId 사용자 ID (문자열)
     * @return 조회된 사용자 엔티티
     * @throws UserNotFoundException 사용자를 찾을 수 없는 경우
     * @throws IllegalArgumentException 사용자 ID가 유효하지 않은 경우
     */
    private User findUserById(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("사용자 ID는 필수입니다.");
        }

        Long userIdLong;
        try {
            userIdLong = Long.parseLong(userId.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("유효하지 않은 사용자 ID 형식입니다: " + userId);
        }

        if (userIdLong <= 0) {
            throw new IllegalArgumentException("사용자 ID는 양수여야 합니다: " + userId);
        }

        return userRepository.findById(userIdLong)
                .orElseThrow(() -> UserNotFoundException.byUserId(userId));
    }
}
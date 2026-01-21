package com.team8.damo.service;

import com.team8.damo.entity.User;
import com.team8.damo.exception.CustomException;
import com.team8.damo.repository.UserRepository;
import com.team8.damo.service.request.UserBasicUpdateServiceRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.team8.damo.exception.errorcode.ErrorCode.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public void updateUserBasic(Long userId, UserBasicUpdateServiceRequest request) {
        if (userRepository.existsByNickname(request.nickname())) {
            throw new CustomException(DUPLICATE_NICKNAME);
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

        user.updateBasic(request.nickname(), request.gender(), request.ageGroup());
    }
}

package com.team8.damo.cache.store;

import com.team8.damo.cache.dto.UserBasicCache;
import com.team8.damo.entity.User;
import com.team8.damo.exception.CustomException;
import com.team8.damo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.team8.damo.cache.CacheSpec.userBasic;
import static com.team8.damo.exception.errorcode.ErrorCode.USER_NOT_FOUND;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserCacheService {

    private final UserRepository userRepository;

    @Cacheable(
        cacheNames = userBasic,
        key = "#userId"
    )
    public UserBasicCache getUserBasic(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new CustomException(USER_NOT_FOUND));
        return UserBasicCache.from(user);
    }
}

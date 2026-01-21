package com.team8.damo.service;

import com.team8.damo.entity.User;
import com.team8.damo.entity.enumeration.AgeGroup;
import com.team8.damo.entity.enumeration.Gender;
import com.team8.damo.exception.CustomException;
import com.team8.damo.fixture.UserFixture;
import com.team8.damo.repository.UserRepository;
import com.team8.damo.service.request.UserBasicUpdateServiceRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.team8.damo.exception.errorcode.ErrorCode.DUPLICATE_NICKNAME;
import static com.team8.damo.exception.errorcode.ErrorCode.USER_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("사용자 기본 정보를 성공적으로 업데이트한다.")
    void updateUserBasic_success() {
        // given
        Long userId = 1L;
        String nickname = "맛집탐험가";
        Gender gender = Gender.MALE;
        AgeGroup ageGroup = AgeGroup.TWENTIES;

        UserBasicUpdateServiceRequest request = new UserBasicUpdateServiceRequest(
            nickname, gender, ageGroup
        );

        User user = UserFixture.create(userId);

        given(userRepository.existsByNickname(nickname)).willReturn(false);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        userService.updateUserBasic(userId, request);

        // then
        assertThat(user.getNickname()).isEqualTo(nickname);
        assertThat(user.getGender()).isEqualTo(gender);
        assertThat(user.getAgeGroup()).isEqualTo(ageGroup);

        then(userRepository).should().existsByNickname(nickname);
        then(userRepository).should().findById(userId);
    }

    @Test
    @DisplayName("이미 사용 중인 닉네임으로 업데이트할 수 없다.")
    void updateUserBasic_duplicateNickname() {
        // given
        Long userId = 1L;
        String duplicateNickname = "중복닉네임";

        UserBasicUpdateServiceRequest request = new UserBasicUpdateServiceRequest(
            duplicateNickname, Gender.MALE, AgeGroup.TWENTIES
        );

        given(userRepository.existsByNickname(duplicateNickname)).willReturn(true);

        // when // then
        assertThatThrownBy(() -> userService.updateUserBasic(userId, request))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", DUPLICATE_NICKNAME);

        then(userRepository).should().existsByNickname(duplicateNickname);
        then(userRepository).should(never()).findById(userId);
    }

    @Test
    @DisplayName("존재하지 않는 사용자의 기본 정보를 업데이트할 수 없다.")
    void updateUserBasic_userNotFound() {
        // given
        Long userId = 999L;
        String nickname = "새닉네임";

        UserBasicUpdateServiceRequest request = new UserBasicUpdateServiceRequest(
            nickname, Gender.FEMALE, AgeGroup.THIRTIES
        );

        given(userRepository.existsByNickname(nickname)).willReturn(false);
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when // then
        assertThatThrownBy(() -> userService.updateUserBasic(userId, request))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", USER_NOT_FOUND);

        then(userRepository).should().existsByNickname(nickname);
        then(userRepository).should().findById(userId);
    }
}

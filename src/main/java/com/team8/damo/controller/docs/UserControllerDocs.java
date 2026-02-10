package com.team8.damo.controller.docs;

import com.team8.damo.controller.request.UserBasicUpdateRequest;
import com.team8.damo.controller.request.UserCharacteristicsCreateRequest;
import com.team8.damo.controller.request.ImagePathUpdateRequest;
import com.team8.damo.controller.request.UserCharacteristicsUpdateRequest;
import com.team8.damo.controller.response.BaseResponse;
import com.team8.damo.service.response.AvailableLightningResponse;
import com.team8.damo.service.response.LightningResponse;
import com.team8.damo.service.response.UserBasicResponse;
import com.team8.damo.service.response.UserProfileResponse;
import com.team8.damo.security.jwt.JwtUserDetails;
import com.team8.damo.swagger.annotation.ApiErrorResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

import static com.team8.damo.exception.errorcode.ErrorCode.*;

@Tag(name = "User API", description = "사용자 관련 API")
public interface UserControllerDocs {

    @Operation(
        summary = "사용자 기본 정보 조회",
        description = """
            ### 사용자의 기본 정보를 조회합니다.
            - userId: 사용자 ID
            - nickname: 닉네임
            - gender: 성별 (MALE, FEMALE)
            - ageGroup: 연령대 (TWENTIES, THIRTIES, FORTIES, FIFTIES_PLUS)
            """
    )
    @ApiResponse(responseCode = "200", description = "성공")
    @ApiErrorResponses({USER_NOT_FOUND})
    BaseResponse<UserBasicResponse> getBasic(
        @Parameter(hidden = true)
        JwtUserDetails user
    );

    @Operation(
        summary = "사용자 기본 정보 수집",
        description = """
            ### 사용자 기본 정보를 수집합니다.
            - 닉네임: 1~10자, 공백/특수문자 불가, 영문/숫자/한글만 허용
            - 성별: MALE, FEMALE
            - 연령대: TWENTIES, THIRTIES, FORTIES, FIFTIES_PLUS
            - imagePath: Presigned URL 발급 시 받은 오브젝트 키
            """
    )
    @ApiResponse(responseCode = "204", description = "성공")
    @ApiErrorResponses({USER_NOT_FOUND, DUPLICATE_NICKNAME})
    BaseResponse<Void> updateBasic(
        @Parameter(hidden = true)
        JwtUserDetails user,
        UserBasicUpdateRequest request
    );

    @Operation(
        summary = "사용자 프로필 이미지 경로 수정",
        description = """
            ### 사용자의 프로필 이미지 경로를 수정합니다.
            - imagePath: S3 업로드 후 반환된 objectKey
            """
    )
    @ApiResponse(responseCode = "204", description = "성공")
    @ApiErrorResponses({USER_NOT_FOUND})
    BaseResponse<Void> updateImagePath(
        @Parameter(hidden = true)
        JwtUserDetails user,
        ImagePathUpdateRequest request
    );

    @Operation(
        summary = "사용자 개인 특성 수집",
        description = """
            ### 사용자의 개인 특성 정보를 수집합니다.
            - allergies: 알레르기 타입 목록 (SHRIMP, CRAB, EGG, MILK 등)
            - likeFoods: 선호 음식 타입 목록 (KOREAN, CHINESE, JAPANESE 등)
            - likeIngredients: 선호 재료 타입 목록 (MEAT, SEAFOOD, VEGETABLE 등)
            - otherCharacteristics: 기타 특성 (최대 100자)
            """
    )
    @ApiResponse(responseCode = "201", description = "성공")
    @ApiErrorResponses({USER_NOT_FOUND, INVALID_CATEGORY, DUPLICATE_ALLERGY_CATEGORY, DUPLICATE_LIKE_FOOD_CATEGORY, DUPLICATE_LIKE_INGREDIENT_CATEGORY})
    BaseResponse<Void> createCharacteristics(
        @Parameter(hidden = true)
        JwtUserDetails user,
        UserCharacteristicsCreateRequest request
    );

    @Operation(
        summary = "사용자 프로필 조회",
        description = """
            ### 사용자의 프로필 정보를 조회합니다.
            - userId: 사용자 ID
            - nickname: 닉네임
            - allergies: 알레르기 카테고리 목록
            - likeFoods: 선호 음식 카테고리 목록
            - likeIngredients: 선호 재료 카테고리 목록
            """
    )
    @ApiResponse(responseCode = "200", description = "성공")
    @ApiErrorResponses({USER_NOT_FOUND})
    BaseResponse<UserProfileResponse> getProfile(
        @Parameter(hidden = true)
        JwtUserDetails user
    );

    @Operation(
        summary = "사용자 프로필 수정",
        description = """
            ### 사용자의 프로필 정보를 수정합니다.
            - allergies: 알레르기 타입 목록 (SHRIMP, CRAB, EGG, MILK 등) - nullable, 빈 리스트 허용
            - likeFoods: 선호 음식 타입 목록 (KOREAN, CHINESE, JAPANESE 등) - 필수
            - likeIngredients: 선호 재료 타입 목록 (MEAT, SEAFOOD, VEGETABLE 등) - 필수
            - otherCharacteristics: 기타 특성 (최대 100자)

            기존 카테고리와 비교하여 추가/삭제 대상만 처리합니다.
            """
    )
    @ApiResponse(responseCode = "204", description = "성공")
    @ApiErrorResponses({USER_NOT_FOUND, INVALID_CATEGORY, DUPLICATE_ALLERGY_CATEGORY, DUPLICATE_LIKE_FOOD_CATEGORY, DUPLICATE_LIKE_INGREDIENT_CATEGORY})
    BaseResponse<Void> updateProfile(
        @Parameter(hidden = true)
        JwtUserDetails user,
        UserCharacteristicsUpdateRequest request
    );

    @Operation(
        summary = "참가중인 번개 모임 목록 조회",
        description = """
            ### 사용자가 참가중인 번개 모임 목록을 조회합니다.
            - lightningDate 기준 최근 3일 이내의 번개 모임만 반환
            - lightningId: 번개 모임 ID
            - restaurantName: 식당 이름
            - description: 설명
            - maxParticipants: 최대 참여 인원
            - participantsCount: 현재 참여 인원
            - lightningStatus: 상태 (OPEN, CLOSED)
            - myRole: 나의 역할 (LEADER, PARTICIPANT)
            """
    )
    @ApiResponse(responseCode = "200", description = "성공")
    @ApiErrorResponses({USER_NOT_FOUND})
    BaseResponse<List<LightningResponse>> getParticipantLightningList(
        @Parameter(hidden = true)
        JwtUserDetails user
    );

    @Operation(
        summary = "참가하지 않은 전체 번개 목록 조회",
        description = """
            ### 사용자가 참가하지 않은 OPEN 상태의 번개 모임 목록을 조회합니다.
            - lightningId: 번개 모임 ID
            - restaurantName: 식당 이름
            - description: 설명
            - maxParticipants: 최대 참여 인원
            - participantsCount: 현재 참여 인원
            - lightningStatus: 상태 (OPEN)
            """
    )
    @ApiResponse(responseCode = "200", description = "성공")
    @ApiErrorResponses({USER_NOT_FOUND})
    BaseResponse<List<AvailableLightningResponse>> getAvailableLightningList(
        @Parameter(hidden = true)
        JwtUserDetails user
    );

    @Operation(
        summary = "회원 탈퇴",
        description = """
            ### 회원 탈퇴를 처리합니다.
            - 카카오 연동 해제
            - 사용자 소프트 삭제 (is_withdraw = true, withdraw_at 설정)
            - Redis 리프레시 토큰 삭제
            - access_token, refresh_token 쿠키 삭제
            """
    )
    @ApiResponse(responseCode = "204", description = "성공")
    @ApiErrorResponses({USER_NOT_FOUND, ALREADY_WITHDRAWN, KAKAO_UNLINK_FAILED})
    BaseResponse<Void> withdraw(
        @Parameter(hidden = true) JwtUserDetails user,
        @Parameter(hidden = true) HttpServletResponse response
    );
}

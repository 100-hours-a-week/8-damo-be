package com.team8.damo.controller.docs;

import com.team8.damo.controller.request.UserBasicUpdateRequest;
import com.team8.damo.controller.request.UserCharacteristicsCreateRequest;
import com.team8.damo.controller.response.BaseResponse;
import com.team8.damo.service.response.UserBasicResponse;
import com.team8.damo.service.response.UserProfileResponse;
import com.team8.damo.security.jwt.JwtUserDetails;
import com.team8.damo.swagger.annotation.ApiErrorResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

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
}

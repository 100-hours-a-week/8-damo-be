package com.team8.damo.controller;

import com.team8.damo.controller.docs.DiningControllerDocs;
import com.team8.damo.controller.request.AttendanceVoteRequest;
import com.team8.damo.controller.request.DiningCreateRequest;
import com.team8.damo.controller.request.RestaurantVoteRequest;
import com.team8.damo.controller.response.BaseResponse;
import com.team8.damo.entity.enumeration.DiningStatus;
import com.team8.damo.entity.enumeration.AttendanceVoteStatus;
import com.team8.damo.security.jwt.JwtUserDetails;
import com.team8.damo.service.DiningService;
import com.team8.damo.service.SseEmitterService;
import com.team8.damo.service.response.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class DiningController implements DiningControllerDocs {

    private final DiningService diningService;
    private final SseEmitterService emitterService;

    @Override
    @PostMapping("/groups/{groupId}/dining")
    public BaseResponse<Long> createDining(
        @AuthenticationPrincipal JwtUserDetails user,
        @PathVariable Long groupId,
        @Valid @RequestBody DiningCreateRequest request
    ) {
        return BaseResponse.created(
            diningService.createDining(user.getUserId(), groupId, request.toServiceRequest(), LocalDateTime.now())
        );
    }

    @Override
    @GetMapping("/groups/{groupId}/dining")
    public BaseResponse<List<DiningResponse>> getDiningList(
        @AuthenticationPrincipal JwtUserDetails user,
        @PathVariable Long groupId,
        @RequestParam DiningStatus status
    ) {
        return BaseResponse.ok(
            diningService.getDiningList(user.getUserId(), groupId, status)
        );
    }

    @Override
    @GetMapping("/groups/{groupId}/dining/{diningId}")
    public BaseResponse<DiningDetailResponse> getDiningDetail(
        @AuthenticationPrincipal JwtUserDetails user,
        @PathVariable Long groupId,
        @PathVariable Long diningId
    ) {
        return BaseResponse.ok(
            diningService.getDiningDetail(user.getUserId(), groupId, diningId)
        );
    }

    @Override
    @GetMapping("/groups/{groupId}/dining/{diningId}/attendance-vote")
    public BaseResponse<AttendanceVoteDetailResponse> getAttendanceVoteDetail(
        @AuthenticationPrincipal JwtUserDetails user,
        @PathVariable Long groupId,
        @PathVariable Long diningId
    ) {
        return BaseResponse.ok(
            diningService.getAttendanceVoteDetail(user.getUserId(), groupId, diningId)
        );
    }

    @Override
    @PatchMapping("/groups/{groupId}/dining/{diningId}/attendance-vote")
    public BaseResponse<AttendanceVoteStatus> voteAttendance(
        @AuthenticationPrincipal JwtUserDetails user,
        @PathVariable Long groupId,
        @PathVariable Long diningId,
        @Valid @RequestBody AttendanceVoteRequest request
    ) {
        return BaseResponse.ok(
            diningService.voteAttendance(user.getUserId(), groupId, diningId, request.getAttendanceVoteStatus())
        );
    }

    @Override
    @PostMapping("/groups/{groupId}/dining/{diningId}/restaurants-vote/{recommendRestaurantsId}")
    public BaseResponse<RestaurantVoteResponse> voteRestaurant(
        @AuthenticationPrincipal JwtUserDetails user,
        @PathVariable Long groupId,
        @PathVariable Long diningId,
        @PathVariable Long recommendRestaurantsId,
        @Valid @RequestBody RestaurantVoteRequest request
    ) {
        return BaseResponse.created(
            diningService.voteRestaurant(
                user.getUserId(), groupId, diningId,
                recommendRestaurantsId, request.toServiceRequest()
            )
        );
    }

    @Override
    @GetMapping("/groups/{groupId}/dining/{diningId}/restaurant-vote")
    public BaseResponse<List<RestaurantVoteDetailResponse>> getRestaurantVoteDetail(
        @AuthenticationPrincipal JwtUserDetails user,
        @PathVariable Long groupId,
        @PathVariable Long diningId
    ) {
        return BaseResponse.ok(
            diningService.getRestaurantVoteDetail(user.getUserId(), groupId, diningId)
        );
    }

    @Override
    @GetMapping("/groups/{groupId}/dining/{diningId}/confirmed")
    public BaseResponse<DiningConfirmedResponse> getDiningConfirmed(
        @AuthenticationPrincipal JwtUserDetails user,
        @PathVariable Long groupId,
        @PathVariable Long diningId
    ) {
        return BaseResponse.ok(
            diningService.getDiningConfirmed(user.getUserId(), groupId, diningId)
        );
    }

    @Override
    @PatchMapping("/groups/{groupId}/dining/{diningId}/recommend-restaurants/{recommendRestaurantsId}/confirmed")
    public BaseResponse<DiningConfirmedResponse> confirmDiningRestaurant(
        @AuthenticationPrincipal JwtUserDetails user,
        @PathVariable Long groupId,
        @PathVariable Long diningId,
        @PathVariable Long recommendRestaurantsId
    ) {
        return BaseResponse.ok(
            diningService.confirmDiningRestaurant(
                user.getUserId(), groupId, diningId, recommendRestaurantsId
            )
        );
    }

    @Override
    @PostMapping("/groups/{groupId}/dining/{diningId}/recommend-restaurant/refresh")
    public BaseResponse<Void> refreshRecommendRestaurants(
        @AuthenticationPrincipal JwtUserDetails user,
        @PathVariable Long groupId,
        @PathVariable Long diningId
    ) {
        diningService.refreshRecommendRestaurants(user.getUserId(), groupId, diningId);
        return BaseResponse.noContent();
    }

    @Override
    @GetMapping(
        value = "/groups/{groupId}/dining/{diningId}/recommendation-streaming",
        produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public SseEmitter streamingSubscribe(
        @PathVariable Long groupId,
        @PathVariable Long diningId,
        @AuthenticationPrincipal JwtUserDetails user
    ) {
        return emitterService.subscribe(user.getUserId(), diningId);
    }

    @Override
    @GetMapping("/groups/{groupId}/dining/{diningId}/recommendation-streaming/history")
    public BaseResponse<CursorPageResponse<RecommendationStreamingResponse>> getRecommendationStreaming(
        @PathVariable Long groupId,
        @PathVariable Long diningId,
        @AuthenticationPrincipal JwtUserDetails user
    ) {
        return BaseResponse.ok(diningService.getRecommendationStreaming(diningId));
    }
}

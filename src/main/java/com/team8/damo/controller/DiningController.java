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
import com.team8.damo.service.response.DiningDetailResponse;
import com.team8.damo.service.response.DiningResponse;
import com.team8.damo.service.response.RestaurantVoteResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class DiningController implements DiningControllerDocs {

    private final DiningService diningService;

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
}

package com.team8.damo.controller;

import com.team8.damo.controller.docs.DiningControllerDocs;
import com.team8.damo.controller.request.DiningCreateRequest;
import com.team8.damo.controller.response.BaseResponse;
import com.team8.damo.entity.enumeration.DiningStatus;
import com.team8.damo.security.jwt.JwtUserDetails;
import com.team8.damo.service.DiningService;
import com.team8.damo.service.response.DiningResponse;
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
}

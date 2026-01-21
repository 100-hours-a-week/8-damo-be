package com.team8.damo.controller;

import com.team8.damo.controller.request.GroupCreateRequest;
import com.team8.damo.controller.response.BaseResponse;
import com.team8.damo.security.jwt.JwtUserDetails;
import com.team8.damo.service.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.team8.damo.controller.docs.GroupControllerDocs;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class GroupController implements GroupControllerDocs {

    private final GroupService groupService;

    @PostMapping("/groups")
    public BaseResponse<Long> createGroup(
        @AuthenticationPrincipal JwtUserDetails user,
        @Valid @RequestBody GroupCreateRequest request
    ) {
        return BaseResponse.created(groupService.createGroup(user.getUserId(), request.toServiceRequest()));
    }
}

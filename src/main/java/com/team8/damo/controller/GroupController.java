package com.team8.damo.controller;

import com.team8.damo.controller.request.GroupCreateRequest;
import com.team8.damo.controller.response.BaseResponse;
import com.team8.damo.security.jwt.JwtUserDetails;
import com.team8.damo.service.GroupService;
import com.team8.damo.service.response.UserGroupResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.team8.damo.controller.docs.GroupControllerDocs;

import java.util.List;

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

    @GetMapping("/users/me/groups")
    public BaseResponse<List<UserGroupResponse>> groupList(
        @AuthenticationPrincipal JwtUserDetails user
    ) {
        return BaseResponse.ok(groupService.getGroupList(user.getUserId()));
    }
}

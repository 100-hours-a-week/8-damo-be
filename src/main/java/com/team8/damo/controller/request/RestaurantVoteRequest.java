package com.team8.damo.controller.request;

import com.team8.damo.entity.enumeration.VoteStatus;
import com.team8.damo.service.request.RestaurantVoteServiceRequest;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class RestaurantVoteRequest {

    @NotNull(message = "투표 상태는 필수입니다.")
    private VoteStatus voteStatus;

    public RestaurantVoteServiceRequest toServiceRequest() {
        return new RestaurantVoteServiceRequest(voteStatus);
    }
}

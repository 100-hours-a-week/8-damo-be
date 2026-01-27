package com.team8.damo.controller.request;

import com.team8.damo.entity.enumeration.AttendanceVoteStatus;
import com.team8.damo.validator.ValidVoteStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class AttendanceVoteRequest {

    @NotNull(message = "투표 상태는 필수입니다.")
    @ValidVoteStatus
    private AttendanceVoteStatus attendanceVoteStatus;

}

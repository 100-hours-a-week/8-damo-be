package com.team8.damo.service.response;

import com.team8.damo.entity.Dining;
import com.team8.damo.entity.DiningParticipant;
import com.team8.damo.entity.enumeration.AttendanceVoteStatus;

public record AttendanceVoteDetailResponse(
    AttendanceVoteStatus attendanceVoteStatus,
    int completedVoteCount,
    int totalGroupMemberCount
) {
    public static AttendanceVoteDetailResponse of(DiningParticipant participant, Dining dining) {
        return new AttendanceVoteDetailResponse(
            participant.getAttendanceVoteStatus(),
            dining.getAttendanceVoteDoneCount(),
            dining.getGroup().getTotalMembers()
        );
    }
}

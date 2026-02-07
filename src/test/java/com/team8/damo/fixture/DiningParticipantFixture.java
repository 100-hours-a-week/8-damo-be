package com.team8.damo.fixture;

import com.team8.damo.entity.Dining;
import com.team8.damo.entity.DiningParticipant;
import com.team8.damo.entity.User;
import com.team8.damo.entity.enumeration.AttendanceVoteStatus;

public class DiningParticipantFixture {

    public static DiningParticipant create(Long id, Dining dining, User user) {
        return DiningParticipant.builder()
            .id(id)
            .dining(dining)
            .user(user)
            .attendanceVoteStatus(AttendanceVoteStatus.PENDING)
            .build();
    }

    public static DiningParticipant create(Long id, Dining dining, User user, AttendanceVoteStatus attendanceVoteStatus) {
        return DiningParticipant.builder()
            .id(id)
            .dining(dining)
            .user(user)
            .attendanceVoteStatus(attendanceVoteStatus)
            .build();
    }
}

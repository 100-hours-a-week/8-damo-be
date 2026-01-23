package com.team8.damo.fixture;

import com.team8.damo.entity.Dining;
import com.team8.damo.entity.DiningParticipant;
import com.team8.damo.entity.User;
import com.team8.damo.entity.enumeration.VotingStatus;

public class DiningParticipantFixture {

    public static DiningParticipant create(Long id, Dining dining, User user) {
        return DiningParticipant.builder()
            .id(id)
            .dining(dining)
            .user(user)
            .votingStatus(VotingStatus.PENDING)
            .build();
    }

    public static DiningParticipant create(Long id, Dining dining, User user, VotingStatus votingStatus) {
        return DiningParticipant.builder()
            .id(id)
            .dining(dining)
            .user(user)
            .votingStatus(votingStatus)
            .build();
    }
}

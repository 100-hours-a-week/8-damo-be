package com.team8.damo.repository;

import com.team8.damo.entity.DiningParticipant;
import com.team8.damo.entity.enumeration.VotingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiningParticipantRepository extends JpaRepository<DiningParticipant, Long> {

    int countByDiningIdAndVotingStatus(Long diningId, VotingStatus votingStatus);

    List<Integer> countByDiningIdInAndVotingStatus(List<Long> diningIds, VotingStatus votingStatus);

}

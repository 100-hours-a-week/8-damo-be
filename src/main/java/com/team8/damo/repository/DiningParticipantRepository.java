package com.team8.damo.repository;

import com.team8.damo.entity.Dining;
import com.team8.damo.entity.DiningParticipant;
import com.team8.damo.entity.enumeration.VotingStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DiningParticipantRepository extends JpaRepository<DiningParticipant, Long> {

    int countByDiningIdAndVotingStatus(Long diningId, VotingStatus votingStatus);

    List<Integer> countByDiningIdInAndVotingStatus(List<Long> diningIds, VotingStatus votingStatus);

    Optional<DiningParticipant> findByDiningIdAndUserId(Long diningId, Long userId);

    int countByDiningId(Long diningId);

    @EntityGraph(attributePaths = {"dining"})
    List<DiningParticipant> findByDiningIdInAndVotingStatus(
        List<Long> diningIds,
        VotingStatus votingStatus
    );

    @EntityGraph(attributePaths = {"user"})
    List<DiningParticipant> findAllByDiningAndVotingStatus(Dining dining, VotingStatus votingStatus);
}

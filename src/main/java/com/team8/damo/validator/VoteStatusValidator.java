package com.team8.damo.validator;

import com.team8.damo.entity.enumeration.VotingStatus;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class VoteStatusValidator implements ConstraintValidator<ValidVoteStatus, VotingStatus> {

    @Override
    public boolean isValid(VotingStatus value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }
        return value == VotingStatus.ATTEND || value == VotingStatus.NON_ATTEND;
    }
}

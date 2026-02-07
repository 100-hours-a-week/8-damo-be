package com.team8.damo.validator;

import com.team8.damo.entity.enumeration.AttendanceVoteStatus;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class VoteStatusValidator implements ConstraintValidator<ValidVoteStatus, AttendanceVoteStatus> {

    @Override
    public boolean isValid(AttendanceVoteStatus value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }
        return value == AttendanceVoteStatus.ATTEND || value == AttendanceVoteStatus.NON_ATTEND;
    }
}

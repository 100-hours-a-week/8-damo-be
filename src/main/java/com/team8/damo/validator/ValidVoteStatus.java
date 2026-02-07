package com.team8.damo.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = VoteStatusValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidVoteStatus {
    String message() default "유효하지 않은 투표 상태입니다.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

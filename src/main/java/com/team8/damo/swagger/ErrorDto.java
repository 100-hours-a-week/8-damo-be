package com.team8.damo.swagger;

import com.team8.damo.exception.errorcode.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ErrorDto {
    private String httpStatus;
    private Object data;
    private String errorMessage;

    static ErrorDto from(ErrorCode errorCode) {
        return new ErrorDto(
          errorCode.getHttpStatus().getReasonPhrase(),
          null,
          errorCode.getMessage()
        );
    }
}

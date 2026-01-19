package com.team8.damo.exception;

import com.team8.damo.controller.response.BaseResponse;
import com.team8.damo.exception.errorcode.ErrorCode;
import com.team8.damo.exception.errorcode.GlobalErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

@Slf4j
@RestControllerAdvice(annotations = RestController.class)
public class GlobalExceptionHandler {
    // 존재하지 않는 요청에 대한 예외
    @ExceptionHandler(value = {NoHandlerFoundException.class, HttpRequestMethodNotSupportedException.class})
    public BaseResponse<?> handleNoPageFoundException(Exception e) {
        log.error("[Not Found Exception]", e);
        return BaseResponse.fail(new CustomException(GlobalErrorCode.NOT_FOUND_END_POINT));
    }

    // 커스텀 예외
    @ExceptionHandler(value = {CustomException.class})
    public BaseResponse<?> handleCustomException(CustomException e) {
        log.error("[Custom Exception]", e);
        return BaseResponse.fail(e);
    }

    // validation 예외
    @ExceptionHandler(value = {MethodArgumentNotValidException.class})
    public BaseResponse<?> handleValidationException(MethodArgumentNotValidException e) {
        log.error("[Validation Exception]", e);
        return BaseResponse.validationFail(e);
    }

    // 기본 예외
    @ExceptionHandler(value = {Exception.class})
    public BaseResponse<?> handleException(Exception e) {
        log.error("[Unhandled Exception]", e);
        return BaseResponse.fail(new CustomException(GlobalErrorCode.INTERNAL_SERVER_ERROR));
    }
}

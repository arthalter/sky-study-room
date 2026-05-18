package com.sky.study.exception;

import com.sky.study.vo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public Result<String> exceptionHandler(BaseException e) {
        log.warn("Business exception", e);
        return Result.error(e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<String> exceptionHandler(Exception e) {
        log.error("Unhandled exception", e);
        String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        return Result.error("服务器异常：" + message);
    }
}

package com.sky.study.exception;

import com.sky.study.vo.Result;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public Result<String> exceptionHandler(BaseException e) {
        log.warn("Business exception", e);
        return Result.error(e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<String> exceptionHandler(MethodArgumentNotValidException e) {
        log.warn("Request body validation failed", e);
        return Result.error(getFieldErrorMessage(e.getBindingResult().getFieldError()));
    }

    @ExceptionHandler(BindException.class)
    public Result<String> exceptionHandler(BindException e) {
        log.warn("Request parameter validation failed", e);
        return Result.error(getFieldErrorMessage(e.getBindingResult().getFieldError()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Result<String> exceptionHandler(ConstraintViolationException e) {
        log.warn("Constraint validation failed", e);
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("；"));
        return Result.error(message);
    }

    @ExceptionHandler(Exception.class)
    public Result<String> exceptionHandler(Exception e) {
        log.error("Unhandled exception", e);
        String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        return Result.error("服务器异常：" + message);
    }

    private String getFieldErrorMessage(FieldError fieldError) {
        if (fieldError == null) {
            return "请求参数错误";
        }
        return fieldError.getDefaultMessage();
    }
}

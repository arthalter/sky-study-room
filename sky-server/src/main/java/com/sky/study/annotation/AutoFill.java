package com.sky.study.annotation;

import com.sky.study.enumeration.OperationType;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AutoFill {
    OperationType value();
}
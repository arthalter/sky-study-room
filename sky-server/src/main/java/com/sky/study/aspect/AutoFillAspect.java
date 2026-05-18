package com.sky.study.aspect;

import com.sky.study.annotation.AutoFill;
import com.sky.study.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.aspectj.lang.reflect.MethodSignature;


import java.lang.reflect.Method;
import java.time.LocalDateTime;

@Aspect
@Component
@Slf4j
public class AutoFillAspect {

    /**
     * 切入点：
     * 拦截 mapper 包及其子包下所有带有 AutoFill 注解的方法
     */
    @Before("execution(* com.sky.study.mapper..*.*(..)) && @annotation(com.sky.study.annotation.AutoFill)")
    public void autoFill(JoinPoint joinPoint) {
        log.info("开始公共字段自动填充...");

        // 1. 获取方法签名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();

        // 2. 获取方法上的 AutoFill 注解
        AutoFill autoFill = signature.getMethod().getAnnotation(AutoFill.class);

        // 3. 获取数据库操作类型
        OperationType operationType = autoFill.value();

        // 4. 获取当前被拦截方法的参数
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            return;
        }

        // 默认第一个参数就是实体对象
        Object entity = args[0];

        // 5. 准备自动填充的数据
        LocalDateTime now = LocalDateTime.now();

        // 6. 根据不同操作类型，通过反射为实体赋值
        try {
            if (operationType == OperationType.INSERT) {
                Method setCreateTime = entity.getClass().getDeclaredMethod("setCreateTime", LocalDateTime.class);
                Method setUpdateTime = entity.getClass().getDeclaredMethod("setUpdateTime", LocalDateTime.class);

                setCreateTime.invoke(entity, now);
                setUpdateTime.invoke(entity, now);
            } else if (operationType == OperationType.UPDATE) {
                Method setUpdateTime = entity.getClass().getDeclaredMethod("setUpdateTime", LocalDateTime.class);
                setUpdateTime.invoke(entity, now);
            }
        } catch (Exception e) {
            log.error("公共字段自动填充失败", e);
        }
    }
}
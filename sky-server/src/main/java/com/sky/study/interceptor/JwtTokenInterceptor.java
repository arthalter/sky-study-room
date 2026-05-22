package com.sky.study.interceptor;

import com.sky.study.context.BaseContext;
import com.sky.study.properties.JwtProperties;
import com.sky.study.service.JwtBlacklistService;
import com.sky.study.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@Slf4j
public class JwtTokenInterceptor implements HandlerInterceptor {

    @Resource
    private JwtProperties jwtProperties;
    @Resource
    private JwtBlacklistService jwtBlacklistService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String token = request.getHeader(jwtProperties.getTokenName());
        String uri = request.getRequestURI();
        if (token == null || token.isEmpty()) {
            response.setStatus(401);
            return false;
        }
        if (jwtBlacklistService.isBlacklisted(token)) {
            response.setStatus(401);
            return false;
        }

        try {
            Claims claims = JwtUtil.parseJWT(jwtProperties.getSecretKey(), token);
            String role = claims.get("role", String.class);

            log.info("token:{}", token);
            log.info("role:{}", role);
            if (uri.startsWith("/api/admin")&&!"ADMIN".equals(role)) {
                log.info("权限不足");
                response.setStatus(403);
                return false;
            }

            Long currentId = Long.valueOf(claims.get("id").toString());
            BaseContext.setCurrentId(currentId);

            return true;
        } catch (Exception e) {
            response.setStatus(401);
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        BaseContext.removeCurrentId();
    }
}

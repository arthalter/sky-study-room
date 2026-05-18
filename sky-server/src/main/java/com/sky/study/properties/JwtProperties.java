package com.sky.study.properties;

import com.sky.study.service.UserService;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "sky.jwt")
public class JwtProperties {
    private String secretKey;
    private Long ttl;
    private String tokenName;
}

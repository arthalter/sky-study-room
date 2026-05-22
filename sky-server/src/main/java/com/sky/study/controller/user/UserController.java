package com.sky.study.controller.user;

import com.sky.study.constant.MessageConstant;
import com.sky.study.constant.RoleConstant;
import com.sky.study.dto.UserDTO;
import com.sky.study.entity.User;
import com.sky.study.properties.JwtProperties;
import com.sky.study.service.JwtBlacklistService;
import com.sky.study.service.UserService;
import com.sky.study.utils.JwtUtil;
import com.sky.study.vo.Result;
import com.sky.study.vo.UserLoginVO;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@Slf4j
public class UserController {
    @Autowired
    UserService userService;
    @Autowired
    private JwtProperties jwtProperties;
    @Autowired
    private JwtBlacklistService jwtBlacklistService;

    @PostMapping("/login")
    public Result<UserLoginVO> login(@RequestBody @Valid UserDTO userDTO) {
        log.info("用户登录请求: {}", userDTO);
        User user = userService.findByUsername(userDTO.getName());
        //检验用户名是否存在
        if (user == null) {
            return Result.error(MessageConstant.LOGIN_FAILED);
        }
        //检验密码正确性
        String password = userDTO.getPassword();
        String md5Password = DigestUtils.md5DigestAsHex(password.getBytes());
        if (!md5Password.equals(user.getPassword())) {
            return Result.error(MessageConstant.LOGIN_FAILED);
        }
        //检验权限是否正确
        if (!RoleConstant.USER.equals(user.getRole())) {
            return Result.error(MessageConstant.NO_PERMISSION);
        }
        //登录成功，返回用户信息
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", RoleConstant.USER);
        claims.put("name", user.getUsername());
        claims.put("id", user.getId());

        String token = JwtUtil.createJWT(
                jwtProperties.getSecretKey(),
                jwtProperties.getTtl(),
                claims
        );
        UserLoginVO userLoginVO = new UserLoginVO();
        userLoginVO.setToken(token);
        userLoginVO.setUsername(userDTO.getName());
        userLoginVO.setRole(RoleConstant.USER);
        userLoginVO.setId(user.getId());
        return Result.success(userLoginVO);
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        String token = request.getHeader(jwtProperties.getTokenName());
        jwtBlacklistService.blacklist(token);
        return Result.success();
    }
}

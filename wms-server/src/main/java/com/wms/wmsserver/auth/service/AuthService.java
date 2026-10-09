package com.wms.wmsserver.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wms.common.BusinessException;
import com.wms.common.ErrorCode;
import com.wms.common.RegexPatterns;
import com.wms.common.dto.LoginRequest;
import com.wms.common.dto.LoginResponse;
import com.wms.wmsserver.auth.util.JwtUtil;
import com.wms.wmsserver.entity.Person;
import com.wms.wmsserver.entity.User;
import com.wms.wmsserver.mapper.PersonMapper;
import com.wms.wmsserver.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 登录鉴权业务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final PersonMapper personMapper;
    private final JwtUtil jwtUtil;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /** 登录：校验 → 查权限 → 签发 JWT → 返回 */
    public LoginResponse login(LoginRequest req) {
        // 服务端二次正则校验（客户端已做，双校验）
        if (!req.getUsername().matches(RegexPatterns.USERNAME)
                || !req.getPassword().matches(RegexPatterns.PASSWORD)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "用户名或口令格式不符合规则");
        }

        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, req.getUsername()));
        if (user == null || user.getStatus() != 1
                || !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            // 信息模糊化：错误提示不分「用户不存在」和「口令错」，防用户名枚举
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或口令错误");
        }

        // 查人员档案
        Person person = personMapper.selectById(user.getPersonId());

        // 查权限码
        List<String> permissions = userMapper.selectPermCodesByUserId(user.getId());

        // 签发 JWT
        String token = jwtUtil.issue(user.getId(), user.getUsername(), permissions);

        log.info("用户登录成功：userId={}, username={}", user.getId(), user.getUsername());

        return LoginResponse.builder()
                .token(token)
                .username(user.getUsername())
                .personCode(person == null ? null : person.getPersonCode())
                .realName(person == null ? null : person.getName())
                .permissions(permissions)
                .build();
    }

    /** 当前用户信息（启动恢复/刷新权限集用）：从数据库取最新权限，覆盖 token 内可能陈旧的 permissions */
    public LoginResponse me(Long userId, String clientToken) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getStatus() != 1) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户不存在或已停用");
        }
        Person person = personMapper.selectById(user.getPersonId());
        List<String> permissions = userMapper.selectPermCodesByUserId(user.getId());
        return LoginResponse.builder()
                .token(clientToken)
                .username(user.getUsername())
                .personCode(person == null ? null : person.getPersonCode())
                .realName(person == null ? null : person.getName())
                .permissions(permissions)
                .build();
    }
}

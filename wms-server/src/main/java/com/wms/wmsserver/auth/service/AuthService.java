package com.wms.wmsserver.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wms.common.BusinessException;
import com.wms.common.ErrorCode;
import com.wms.common.RegexPatterns;
import com.wms.common.WmsConstants;
import com.wms.common.dto.LoginRequest;
import com.wms.common.dto.LoginResponse;
import com.wms.wmsserver.auth.config.RsaKeyHolder;
import com.wms.wmsserver.auth.util.JwtUtil;
import com.wms.wmsserver.entity.Permission;
import com.wms.wmsserver.entity.Person;
import com.wms.wmsserver.entity.User;
import com.wms.wmsserver.mapper.PersonMapper;
import com.wms.wmsserver.mapper.PermissionMapper;
import com.wms.wmsserver.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 登录鉴权业务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final PersonMapper personMapper;
    private final PermissionMapper permissionMapper;
    private final JwtUtil jwtUtil;
    private final RsaKeyHolder rsaKeyHolder;

    private final PasswordEncoder passwordEncoder;

    /** 登录：校验 → 查权限 → 签发 JWT → 返回 */
    public LoginResponse login(LoginRequest req) {
        // 解密口令：优先 encryptedPassword（生产路径），回退 password（开发联调）
        String rawPassword = resolvePassword(req);

        // 服务端二次正则校验（客户端已做，双校验）
        if (!req.getUsername().matches(RegexPatterns.USERNAME)
                || !rawPassword.matches(RegexPatterns.PASSWORD)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "用户名或口令格式不符合规则");
        }

        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, req.getUsername()));
        if (user == null || user.getStatus() != 1
                || !passwordEncoder.matches(rawPassword, user.getPassword())) {
            // 信息模糊化：错误提示不分「用户不存在」和「口令错」，防用户名枚举
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或口令错误");
        }

        // 查人员档案
        Person person = personMapper.selectById(user.getPersonId());

        // 查权限码（系统管理员恒为全部允许，与其 user_permission 行无关，新增权限自动包含）
        List<String> permissions = resolvePermissions(user);

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
        List<String> permissions = resolvePermissions(user);
        return LoginResponse.builder()
                .token(clientToken)
                .username(user.getUsername())
                .personCode(person == null ? null : person.getPersonCode())
                .realName(person == null ? null : person.getName())
                .permissions(permissions)
                .build();
    }

    /** 有效权限码：系统管理员恒为权限表全部（登录/me 共用）；其余用户按直授 allow − deny */
    private List<String> resolvePermissions(User user) {
        if (WmsConstants.SYSTEM_ADMIN_USERNAME.equals(user.getUsername())) {
            return permissionMapper.selectList(Wrappers.<Permission>lambdaQuery())
                    .stream().map(Permission::getPermCode).collect(Collectors.toList());
        }
        return userMapper.selectPermCodesByUserId(user.getId());
    }

    /**
     * 解密口令：encryptedPassword 非空走 RSA 解密（生产路径），
     * 否则回退明文 password（开发联调，log.warn）；两者皆空抛 BAD_REQUEST。
     */
    private String resolvePassword(LoginRequest req) {
        String enc = req.getEncryptedPassword();
        if (enc != null && !enc.isBlank()) {
            return rsaKeyHolder.decrypt(enc);
        }
        String plain = req.getPassword();
        if (plain == null || plain.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "缺少口令");
        }
        log.warn("登录请求携带明文口令，仅开发模式允许；生产应走 encryptedPassword");
        return plain;
    }
}

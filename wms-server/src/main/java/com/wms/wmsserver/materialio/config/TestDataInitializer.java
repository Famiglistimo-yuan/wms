package com.wms.wmsserver.materialio.config;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wms.wmsserver.entity.User;
import com.wms.wmsserver.mapper.PersonMapper;
import com.wms.wmsserver.mapper.UserMapper;
import com.wms.wmsserver.entity.Person;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 子系统2 —— 本地开发测试数据自动初始化。
 * <p>
 * <b>仅 {@code local} profile 生效</b>（通过 {@code @Profile("local")} 限定），
 * 不会污染 test / prod 环境。
 * <p>
 * 启动时自动做两件事：
 * <ol>
 *   <li>检查 {@code rg2402_11_12_13_person} 里是否有 person_code = "P0001" 的人员，
 *       没有就先插一条（因为 user 表的 person_id 是 NOT NULL + UNIQUE）</li>
 *   <li>检查 {@code rg2402_11_12_13_user} 里是否有 username = "admin"，
 *       没有就用 BCrypt 加密后的密码 "admin123" 插一条</li>
 * </ol>
 * 插完后 curl 先调用 {@code POST /api/auth/login} 拿 token，
 * 再用 token 调用子系统2 的进仓/出仓接口。
 * <p>
 * ⚠️ password 是通过 Spring 容器里的 {@code PasswordEncoder} Bean 加密的，
 * 不需要单独跑 Java 命令行工具生成 BCrypt hash。
 */
@Component
@Profile("local")
@RequiredArgsConstructor
@Slf4j
public class TestDataInitializer implements CommandLineRunner {

    private final UserMapper userMapper;
    private final PersonMapper personMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // 已有 admin 用户则跳过
        if (userMapper.selectCount(Wrappers.<User>lambdaQuery().eq(User::getUsername, "admin")) > 0) {
            return;
        }
        Person person = personMapper.selectOne(
                Wrappers.<Person>lambdaQuery().eq(Person::getPersonCode, "P0001"));
        if (person == null) {
            person = new Person();
            person.setPersonCode("P0001");
            person.setName("系统管理员");
            person.setGender("M");
            personMapper.insert(person);
        }

        User user = new User();
        user.setPersonId(person.getId());
        user.setUsername("admin");
        user.setPassword(passwordEncoder.encode("admin123"));
        user.setStatus(1);
        userMapper.insert(user);
        log.info("===== 测试用户已创建：admin / admin123 =====");
    }
}

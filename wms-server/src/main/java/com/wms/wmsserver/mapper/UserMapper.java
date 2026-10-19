package com.wms.wmsserver.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wms.wmsserver.auth.dto.UserVO;
import com.wms.wmsserver.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    /** 用户列表（联表人员档案取人员代码/姓名；驼峰映射已开，列别名自动对接 VO 属性） */
    @Select("""
            SELECT u.id, u.username, u.status, u.person_id,
                   p.person_code, p.name AS real_name
            FROM rg2402_11_12_13_user u
            LEFT JOIN rg2402_11_12_13_person p ON p.id = u.person_id
            ORDER BY u.id
            """)
    List<UserVO> selectUserVOs();

    /** 用户有效权限码 = (角色继承并集 ∪ 直授 allow) − 禁止 deny（用户直接权限优先于角色） */
    @Select("""
            SELECT DISTINCT p.perm_code
            FROM rg2402_11_12_13_permission p
            WHERE p.id IN (
                    SELECT rp.perm_id
                    FROM rg2402_11_12_13_user_role ur
                    JOIN rg2402_11_12_13_role_permission rp ON rp.role_id = ur.role_id
                    WHERE ur.user_id = #{userId}
                    UNION
                    SELECT upa.perm_id
                    FROM rg2402_11_12_13_user_permission upa
                    WHERE upa.user_id = #{userId} AND upa.effect = 'allow'
                  )
              AND p.id NOT IN (
                    SELECT upd.perm_id
                    FROM rg2402_11_12_13_user_permission upd
                    WHERE upd.user_id = #{userId} AND upd.effect = 'deny'
                  )
            """)
    List<String> selectPermCodesByUserId(@Param("userId") Long userId);

    /** 用户经角色继承的权限 id 并集（用户授权界面回显「继承」来源用，不含直接权限） */
    @Select("""
            SELECT DISTINCT rp.perm_id
            FROM rg2402_11_12_13_user_role ur
            JOIN rg2402_11_12_13_role_permission rp ON rp.role_id = ur.role_id
            WHERE ur.user_id = #{userId}
            """)
    List<Long> selectRolePermIdsByUserId(@Param("userId") Long userId);
}

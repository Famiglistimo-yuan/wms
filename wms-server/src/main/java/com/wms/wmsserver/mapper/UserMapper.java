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

    /** 根据 userId 查询该用户所有权限码 */
    @Select("""
            SELECT DISTINCT p.perm_code
            FROM rg2402_11_12_13_user u
            JOIN rg2402_11_12_13_user_role ur ON ur.user_id = u.id
            JOIN rg2402_11_12_13_role_permission rp ON rp.role_id = ur.role_id
            JOIN rg2402_11_12_13_permission p ON p.id = rp.perm_id
            WHERE u.id = #{userId}
            """)
    List<String> selectPermCodesByUserId(@Param("userId") Long userId);
}

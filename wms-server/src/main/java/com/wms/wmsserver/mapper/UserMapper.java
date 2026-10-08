package com.wms.wmsserver.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wms.wmsserver.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    /** 根据 userId 查询该用户所有权限码 */
    @Select("""
            SELECT DISTINCT p.perm_code
            FROM user u
            JOIN user_role ur ON ur.user_id = u.id
            JOIN role_permission rp ON rp.role_id = ur.role_id
            JOIN permission p ON p.id = rp.perm_id
            WHERE u.id = #{userId}
            """)
    List<String> selectPermCodesByUserId(@Param("userId") Long userId);
}

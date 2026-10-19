package com.wms.wmsserver.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wms.wmsserver.entity.UserPermission;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserPermissionMapper extends BaseMapper<UserPermission> {

    /** 某用户指定 effect（allow/deny）的权限 id 列表（授权界面回显 + 权限计算用） */
    @Select("SELECT perm_id FROM rg2402_11_12_13_user_permission WHERE user_id = #{userId} AND effect = #{effect}")
    List<Long> selectPermIdsByUserIdAndEffect(@Param("userId") Long userId, @Param("effect") String effect);

    /** 清空用户的直接权限（全量覆盖前调用；删除用户时连带清理） */
    @Delete("DELETE FROM rg2402_11_12_13_user_permission WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);

    /** 持有指定权限（allow 且账号启用）的用户数——授权保存防护用 */
    @Select("""
            SELECT COUNT(*) FROM rg2402_11_12_13_user_permission up
            JOIN rg2402_11_12_13_user u ON u.id = up.user_id AND u.status = 1
            JOIN rg2402_11_12_13_permission p ON p.id = up.perm_id
            WHERE p.perm_code = #{permCode} AND up.effect = 'allow'
            """)
    long countEnabledAllowUsersByPermCode(@Param("permCode") String permCode);
}

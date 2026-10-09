package com.wms.wmsserver.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wms.wmsserver.entity.RolePermission;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RolePermissionMapper extends BaseMapper<RolePermission> {

    /** 角色已分配的权限码（联表 permission 取码，角色列表展示用） */
    @Select("""
            SELECT p.perm_code
            FROM rg2402_11_12_13_role_permission rp
            JOIN rg2402_11_12_13_permission p ON p.id = rp.perm_id
            WHERE rp.role_id = #{roleId}
            """)
    List<String> selectPermCodesByRoleId(@Param("roleId") Long roleId);

    /** 角色已分配的权限 id（授权界面回显勾选用） */
    @Select("SELECT perm_id FROM rg2402_11_12_13_role_permission WHERE role_id = #{roleId}")
    List<Long> selectPermIdsByRoleId(@Param("roleId") Long roleId);

    /** 清空角色的权限分配（全量覆盖前调用） */
    @Delete("DELETE FROM rg2402_11_12_13_role_permission WHERE role_id = #{roleId}")
    int deleteByRoleId(@Param("roleId") Long roleId);
}

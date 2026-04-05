package com.userservice.mapper;

import com.userservice.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RoleMapper {
    @Select("select * from tb_role where id=#{id}")
    Role selectById(Long id);

    @Select("select * from tb_role order by id asc")
    List<Role> selectAll();

    @Select("select * from tb_role where role_code = #{roleCode} limit 1")
    Role selectByCode(@Param("roleCode") String roleCode);

    @Select("""
            select p.permission_code
            from tb_role r
            left join tb_role_permission rp on r.id = rp.role_id
            left join tb_permission p on rp.permission_id = p.id
            where r.id = #{roleId}
              and p.permission_code is not null
            order by p.id asc
            """)
    List<String> selectPermissionCodesByRoleId(@Param("roleId") Long roleId);

    @Select("""
            select p.permission_code
            from tb_role r
            left join tb_role_permission rp on r.id = rp.role_id
            left join tb_permission p on rp.permission_id = p.id
            where r.role_code = #{roleCode}
              and p.permission_code is not null
            order by p.id asc
            """)
    List<String> selectPermissionCodesByRoleCode(@Param("roleCode") String roleCode);
}

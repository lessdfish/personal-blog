package com.userservice.mapper;

import com.userservice.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * ClassName:RoleMapper
 * Package:com.userservice.mapper
 * Description:
 *
 * @Author:lyp
 * @Create:2026/3/28 - 20:57
 * @Version: v1.0
 *
 */
@Mapper
public interface RoleMapper {
    @Select("select * from tb_role where id=#{id}")
    Role selectById(Long id);
}

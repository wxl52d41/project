package com.wxl52d41.demo.mapper;

import com.wxl52d41.demo.entity.Role;

public interface RoleMapper {
    /**
    # 查询角色id=? 的角色以及对应的用户
         select * from role r inner join user_role ur inner join user u
	        on r.id = ur.rid and u.id = ur.uid
		        where r.id = ?;

		 1. 参数类型: Integer
		 2. 返回类型: Role
    * */
    Role findRoleByIdWithUsers(Integer rid);

}
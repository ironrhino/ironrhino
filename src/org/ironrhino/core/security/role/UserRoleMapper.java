package org.ironrhino.core.security.role;

import org.springframework.security.core.userdetails.UserDetails;

/**
 * 用户/角色映射关系接口
 */
public interface UserRoleMapper {

	public String[] map(UserDetails user);

}

package org.ironrhino.security.service;

import org.hibernate.criterion.DetachedCriteria;
import org.ironrhino.core.service.BaseManager;
import org.ironrhino.core.spring.security.ConcreteUserDetailsService;
import org.ironrhino.security.model.User;

/**
 * 用户接口定义
 */
public interface UserManager extends BaseManager<User>,
		ConcreteUserDetailsService {

    /**
     * 当用户名重复时此方法实现建议名字，在原名字基础上增加后缀，
     * 如果是邮件地址则在@前的字符末尾增加一位随机数字
     * @param candidate 原名字
     * @return 建议名字
     */
	public String suggestUsername(String candidate);
	
	public DetachedCriteria detachedCriteria(String role);

}

package org.ironrhino.core.model;

import java.util.Date;

import org.springframework.security.core.userdetails.UserDetails;

public interface Recordable<T extends UserDetails> {

	public Date getModifyDate();

	public Date getCreateDate();

	public void setModifyDate(Date date);

	public void setCreateDate(Date date);

	public void setCreateUserDetails(T userDetails);

	public void setModifyUserDetails(T userDetails);

}

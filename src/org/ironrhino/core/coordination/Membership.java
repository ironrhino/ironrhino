package org.ironrhino.core.coordination;

import java.util.List;

public interface Membership {

	public void join(String group) throws Exception;

	public void leave(String group) throws Exception;

	public boolean isLeader(String group) throws Exception;

	public String getLeader(String group) throws Exception;

	public List<String> getMembers(String group) throws Exception;

}
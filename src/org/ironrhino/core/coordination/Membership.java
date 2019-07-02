package org.ironrhino.core.coordination;

import java.util.List;

public interface Membership {

	void join(String group) throws Exception;

	void leave(String group) throws Exception;

	boolean isLeader(String group) throws Exception;

	String getLeader(String group) throws Exception;

	List<String> getMembers(String group) throws Exception;

}
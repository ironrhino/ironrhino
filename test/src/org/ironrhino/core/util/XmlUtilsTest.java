package org.ironrhino.core.util;

import static org.junit.Assert.assertEquals;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.junit.Test;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

public class XmlUtilsTest {

	@XmlRootElement
	@Getter
	@Setter
	public static class User implements Serializable {

		private static final long serialVersionUID = -7632092470064636390L;
		private String username;
		private String password;
		private boolean enabled;
		private Date createDate;
		private List<Name> names;

		public User() {
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.MILLISECOND, 0);
			createDate = cal.getTime();
		}

	}

	@XmlRootElement
	@Data
	public static class Name implements Serializable {

		private static final long serialVersionUID = 2600540506012485655L;
		private String first;
		private String last;

	}

	@Test
	public void test() throws Exception {
		User user = new User();
		user.setUsername("test");
		user.setPassword("password");
		List<Name> names = new ArrayList<>();
		user.setNames(names);
		Name name = new Name();
		name.setFirst("hello");
		name.setLast("world");
		names.add(name);
		String xml = XmlUtils.toXml(user);
		User u = XmlUtils.fromXml(xml, User.class);
		assertEquals(user.getUsername(), u.getUsername());
		assertEquals(user.getPassword(), u.getPassword());
		assertEquals(user.isEnabled(), u.isEnabled());
		assertEquals(user.getCreateDate(), u.getCreateDate());
		assertEquals(user.getNames(), u.getNames());
	}

}

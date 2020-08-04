package org.ironrhino.core.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ironrhino.core.metadata.NotInCopy;
import org.ironrhino.core.model.Persistable;
import org.ironrhino.core.model.Treeable;
import org.ironrhino.core.spring.converter.CustomConversionService;
import org.junit.Test;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class BeanUtilsTest {

	public static @Data class Base implements Serializable, Persistable<String> {

		private static final long serialVersionUID = 1616212908678942555L;
		protected String id;
		private List<String> names;
		private Set<String> tags;
		private Map<String, String> attributes;

		@Override
		public boolean isNew() {
			return id == null;
		}

	}

	public static enum UserType {
		A, B
	}

	public static enum UserType2 {
		A, B
	}

	@Getter
	@Setter
	public static class User extends Base {

		private static final long serialVersionUID = -1634488900558289348L;
		private String username;
		@NotInCopy
		private String password;

		private Team team;

		private UserType type = UserType.A;

	}

	@Getter
	@Setter
	public static class User2 extends Base {

		private static final long serialVersionUID = 5095445311043690504L;
		private String username;
		@NotInCopy
		private String password;

		private UserType2 type = UserType2.A;

	}

	public static enum TeamType {
		A, B
	}

	@Getter
	@Setter
	public static class Team extends Base {

		private static final long serialVersionUID = 8360294456202382419L;

		private String name;

		private User owner;

		private TeamType type;

		private List<User> users;

		private Date createDate = new Date();

	}

	public static enum TeamType2 {
		A, B
	}

	@Getter
	@Setter
	public static class Team2 extends Base {

		private static final long serialVersionUID = 3455118342183020069L;

		private String name;

		private User2 owner;

		private TeamType2 type;

		private List<User2> users;

	}

	@Getter
	@Setter
	public static class Team3 extends Base {

		private static final long serialVersionUID = 1709649066766563947L;

		private String name;

		private String owner;

		private String createDate;

	}

	@Getter
	@Setter
	public static class Team4 extends Base {

		private static final long serialVersionUID = 4712258219610091729L;

		private String name;

		private User owner;

		private Date createDate;

		public Team4(Team team) {
			this.id = team.getId();
			this.name = team.getName();
			this.owner = team.getOwner();
			this.createDate = team.getCreateDate();
		}

	}

	private static class MyList<E> extends ArrayList<E> {
		private static final long serialVersionUID = 1L;
	}

	private static class MySet<E> extends HashSet<E> {
		private static final long serialVersionUID = 1L;
	}

	private static class MyMap<K, V> extends HashMap<K, V> {
		private static final long serialVersionUID = 1L;
	}

	@Getter
	@Setter
	@NoArgsConstructor
	public static class TreeA implements Treeable<TreeA> {
		private String name;
		private TreeA parent;
		private Collection<TreeA> children = new ArrayList<>();

		public TreeA(String name) {
			this.name = name;
		}
	}

	@Getter
	@Setter
	@NoArgsConstructor
	public static class TreeB implements Treeable<TreeB> {
		private String name;
		private TreeB parent;
		private Collection<TreeB> children = new ArrayList<>();

		public TreeB(String name) {
			this.name = name;
		}
	}

	@Test
	public void testCopyTree() {
		TreeA root = new TreeA("root");
		TreeA leaf1 = new TreeA("leaf1");
		TreeA leaf2 = new TreeA("leaf2");
		leaf1.addChild(new TreeA("leaf3"), new TreeA("leaf4"));
		leaf2.addChild(new TreeA("leaf5"), new TreeA("leaf6"));
		root.addChild(leaf1, leaf2);
		assertThat(root.getLevel(), equalTo(1));
		assertThat(leaf1.getLevel(), equalTo(2));
		assertThat(leaf2.getLevel(), equalTo(2));
		TreeA rootCopy = BeanUtils.copyTree(root);
		assertThat(rootCopy.getLevel(), equalTo(1));
		assertThat(rootCopy.getChildren().size(), equalTo(2));
		TreeA leaf1Copy = rootCopy.getChildren().iterator().next();
		assertThat(leaf1Copy.getName(), equalTo("leaf1"));
		assertThat(leaf1Copy.getLevel(), equalTo(2));
		assertThat(leaf1Copy.getChildren().size(), equalTo(2));
		TreeB rootCopyB = BeanUtils.copyTree(root, TreeB::new);
		assertThat(rootCopyB.getLevel(), equalTo(1));
		assertThat(rootCopyB.getChildren().size(), equalTo(2));
		TreeB leaf1CopyB = rootCopyB.getChildren().iterator().next();
		assertThat(leaf1CopyB.getName(), equalTo("leaf1"));
		assertThat(leaf1CopyB.getLevel(), equalTo(2));
		assertThat(leaf1CopyB.getChildren().size(), equalTo(2));
	}

	@Test
	public void testHasProperty() {
		assertThat(BeanUtils.hasProperty(User.class, "id"), equalTo(true));
		assertThat(BeanUtils.hasProperty(User.class, "id"), equalTo(true));
		assertThat(BeanUtils.hasProperty(User.class, "username"), equalTo(true));
		assertThat(!BeanUtils.hasProperty(User.class, "test"), equalTo(true));
	}

	@Test
	public void testCopyProperties() {
		User user1 = new User();
		user1.setId("test");
		user1.setUsername("username");
		user1.setPassword("password");

		User user2 = new User();
		BeanUtils.copyProperties(user1, user2);
		assertThat(user2.getId(), notNullValue());
		assertThat(user2.getId(), notNullValue());
		assertThat(user2.getUsername(), notNullValue());
		assertThat(user2.getPassword(), nullValue());

		user2 = new User();
		BeanUtils.copyProperties(user1, user2, "id");
		assertThat(user2.getId(), nullValue());
		assertThat(user2.getUsername(), notNullValue());
		assertThat(user2.getPassword(), nullValue());

		user2 = new User();
		BeanUtils.copyProperties(user1, user2, "id", "username");
		assertThat(user2.getId(), nullValue());
		assertThat(user2.getUsername(), nullValue());
		assertThat(user2.getPassword(), nullValue());

		User user3 = new User();
		BeanUtils.copyProperties(user1, user3, false);
		assertThat(user3.getId(), notNullValue());
		assertThat(user3.getUsername(), notNullValue());
		assertThat(user3.getPassword(), notNullValue());

	}

	@Test
	public void testCopyPropertiesIfNotNull() {
		User user1 = new User();
		user1.setId("test");
		user1.setUsername("username");

		User user2 = new User();
		user2.setPassword("password");
		BeanUtils.copyPropertiesIfNotNull(user1, user2, "id", "username", "password");
		assertThat(user2.getId(), equalTo("test"));
		assertThat(user2.getUsername(), equalTo("username"));
		assertThat(user2.getPassword(), equalTo("password"));
	}

	@Test
	public void testGetPropertyDescriptor() {
		assertThat(BeanUtils.getPropertyDescriptor(User.class, "none"), nullValue());
		assertThat(BeanUtils.getPropertyDescriptor(User.class, "team.none"), nullValue());
		assertThat(BeanUtils.getPropertyDescriptor(User.class, "team"), notNullValue());
		assertThat(BeanUtils.getPropertyDescriptor(User.class, "team.owner.id"), notNullValue());
	}
	
	@Test
	public void testGetPropertyType() {
		assertThat(BeanUtils.getPropertyType(Team2.class, "none"), nullValue());
		assertThat(BeanUtils.getPropertyType(Team2.class, "team.none"), nullValue());
		assertThat(BeanUtils.getPropertyType(Team2.class, "name"), equalTo(String.class));
		assertThat(BeanUtils.getPropertyType(Team2.class, "owner"), equalTo(User2.class));
		assertThat(BeanUtils.getPropertyType(Team2.class, "owner.username"), equalTo(String.class));
		assertThat(BeanUtils.getPropertyType(Team2.class, "users[0]"), equalTo(User2.class));
		assertThat(BeanUtils.getPropertyType(Team2.class, "users[0].username"), equalTo(String.class));
	}


	@Test
	public void testSetPropertyValue() {
		User u = new User();
		Team team = new Team();
		team.setName("test");
		assertThat(u.getTeam(), nullValue());
		BeanUtils.setPropertyValue(u, "team", team);
		assertThat(u.getTeam(), notNullValue());
		u = new User();
		BeanUtils.setPropertyValue(u, "team.name", "test");
		assertThat(u.getTeam(), notNullValue());
		assertThat(u.getTeam().getName(), equalTo("test"));
		u = new User();
		BeanUtils.setPropertyValue(u, "team.owner.username", "test");
		assertThat(u.getTeam(), notNullValue());
		assertThat(u.getTeam().getOwner(), notNullValue());
		assertThat(u.getTeam().getOwner().getUsername(), equalTo("test"));
	}

	@Test
	public void testCopyEnumByName() {
		Team team = new Team();
		team.setName("name");
		team.setType(TeamType.A);
		Team team1 = new Team();
		BeanUtils.copyProperties(team, team1);
		assertThat(team1.getName(), equalTo(team.getName()));
		assertThat(team1.getType().name(), equalTo(team.getType().name()));
		Team2 team2 = new Team2();
		BeanUtils.copyProperties(team, team2);
		assertThat(team2.getName(), equalTo(team.getName()));
		assertThat(team2.getType().name(), equalTo(team.getType().name()));
	}

	@Test
	public void testSerializableToSerializable() {
		Team team = new Team();
		User user = new User();
		user.setId("id");
		user.setUsername("username");
		team.setName("name");
		team.setOwner(user);
		team.setUsers(Collections.singletonList(user));

		Team team1 = new Team();
		BeanUtils.copyProperties(team, team1);
		assertThat(team1.getOwner(), notNullValue());
		assertThat(team1.getUsers().size(), equalTo(1));
		assertThat(team1.getUsers().get(0).getUsername(), equalTo("username"));
		assertThat(team1.getUsers().get(0).getType().name(), equalTo("A"));

		Team2 team2 = new Team2();
		BeanUtils.copyProperties(team, team2);
		assertThat(team2.getOwner(), notNullValue());
		assertThat(team2.getUsers().size(), equalTo(1));
		assertThat(team2.getUsers().get(0).getUsername(), equalTo("username"));
		assertThat(team2.getUsers().get(0).getType().name(), equalTo("A"));
		assertThat(team2.getUsers().get(0).getClass(), equalTo(User2.class));

		Team3 team3 = new Team3();
		BeanUtils.copyProperties(team, team3);
		assertThat(team3.getOwner(), equalTo(team.getOwner().getId()));
		assertThat(team3.getName(), notNullValue());
		assertThat(team3.getCreateDate(), notNullValue());

		Team4 team4 = CustomConversionService.getSharedInstance().convert(team, Team4.class);
		assertThat(team4, notNullValue());
		assertThat(team4.getOwner(), equalTo(team1.getOwner()));
		assertThat(team4.getCreateDate(), equalTo(team1.getCreateDate()));
	}

	@Test
	public void testNormalizeCollectionFields() {
		User user = new User();
		user.setId("id");
		user.setUsername("username");
		BeanUtils.normalizeCollectionFields(user);
		assertThat(user.getNames(), nullValue());
		assertThat(user.getTags(), nullValue());
		assertThat(user.getAttributes(), nullValue());
		List<String> names = new ArrayList<>();
		names.add("abc");
		Set<String> tags = new HashSet<>();
		tags.add("test");
		Map<String, String> attributes = new HashMap<>();
		attributes.put("test", "test");
		user.setNames(names);
		user.setTags(tags);
		user.setAttributes(attributes);
		BeanUtils.normalizeCollectionFields(user);
		assertThat(user.getNames(), sameInstance(names));
		assertThat(user.getTags(), sameInstance(tags));
		assertThat(user.getAttributes(), sameInstance(attributes));

		List<String> names2 = new MyList<>();
		names2.addAll(names);
		Set<String> tags2 = new MySet<>();
		tags2.addAll(tags);
		Map<String, String> attributes2 = new MyMap<>();
		attributes2.putAll(attributes);
		user.setNames(names2);
		user.setTags(tags2);
		user.setAttributes(attributes2);
		assertThat(user.getNames(), sameInstance(names2));
		assertThat(user.getTags(), sameInstance(tags2));
		assertThat(user.getAttributes(), sameInstance(attributes2));
		BeanUtils.normalizeCollectionFields(user);
		assertThat(user.getNames(), not(sameInstance(names2)));
		assertThat(user.getTags(), not(sameInstance(tags2)));
		assertThat(user.getAttributes(), not(sameInstance(attributes2)));
		assertThat(user.getNames(), equalTo(names));
		assertThat(user.getTags(), equalTo(tags));
		assertThat(user.getAttributes(), equalTo(attributes));
	}

	@Test
	public void testCreateParentIfNull() {
		User u = new User();
		assertThat(u.getAttributes(), nullValue());
		assertThat(u.getTeam(), nullValue());
		BeanUtils.createParentIfNull(u, "attributes");
		assertThat(u.getAttributes(), nullValue());
		BeanUtils.createParentIfNull(u, "attributes['test']");
		assertThat(u.getAttributes(), notNullValue());
		BeanUtils.createParentIfNull(u, "team");
		assertThat(u.getTeam(), nullValue());
		BeanUtils.createParentIfNull(u, "team.owner");
		assertThat(u.getTeam(), notNullValue());
		u = new User();
		Team t = new Team();
		u.setTeam(t);
		BeanUtils.createParentIfNull(u, "team.owner");
		BeanUtils.createParentIfNull(u, "team.owner.username");
		assertThat(u.getTeam(), notNullValue());
		assertThat(u.getTeam(), equalTo(t));
		assertThat(u.getTeam().getOwner(), notNullValue());
		u = new User();
		BeanUtils.createParentIfNull(u, "team.users[0].username");
		assertThat(u.getTeam().getUsers(), notNullValue());
	}

	@Test
	public void testIsEmpty() {
		Team3 team = new Team3();
		assertThat(BeanUtils.isEmpty(team), equalTo(true));
		team.setCreateDate("");
		assertThat(!BeanUtils.isEmpty(team), equalTo(true));
	}

}

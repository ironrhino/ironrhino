package org.ironrhino.core.dataroute;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = RoutingDataSourceConfiguration.class)
public class RoutingDataSourceTest {

	@Autowired
	private PetRepository petRepository;

	@Autowired
	private BeanFactory beanFactory;

	@Autowired
	private ShardingsTemplateHolder shardingsTemplateHolder;

	@Test
	public void testOnMethod() throws Exception {
		String name = "test";
		for (int i = 0; i < 10; i++) {
			name += "0";
			Pet p = new Pet();
			p.setName(name);
			petRepository.save(p);
			assertThat(petRepository.get(name), is(p));
			verify(name);
		}
	}

	@Test
	public void testOnClass() throws Exception {
		String name = "test";
		for (int i = 0; i < 10; i++) {
			petRepository.saveOwnership(name, "owner");
		}
		assertThat(petRepository.findOwnership("owner").size(), is(10));
		verifyOwnership("owner");
	}

	private void verify(String name) throws SQLException {
		int index = name.length() % 4;
		DataSource ds = beanFactory.getBean("sharding" + index, DataSource.class);
		try (Connection conn = ds.getConnection();
				PreparedStatement stmt = conn.prepareStatement("select * from pet where name = ?")) {
			stmt.setString(1, name);
			try (ResultSet rs = stmt.executeQuery()) {
				assertThat(name + " doesn't exists in " + ds, rs.next(), is(true));
			}
		}
		Long count = shardingsTemplateHolder.route(name).jdbc.queryForObject("select count(*) from pet where name = ?",
				Long.class, name);
		boolean exists = count != null && count > 0;
		assertThat(name + " doesn't exists", exists, is(true));
	}

	private void verifyOwnership(String owner) throws SQLException {
		DataSource ds = beanFactory.getBean("sharding3", DataSource.class);
		try (Connection conn = ds.getConnection();
				PreparedStatement stmt = conn.prepareStatement("select count(*) from ownership where owner = ?")) {
			stmt.setString(1, owner);
			try (ResultSet rs = stmt.executeQuery()) {
				assertThat("owner doesn't exists in " + ds, rs.next(), is(true));
				assertThat("owner doesn't exists in " + ds, rs.getLong(1), is(10L));
			}
		}
	}

}

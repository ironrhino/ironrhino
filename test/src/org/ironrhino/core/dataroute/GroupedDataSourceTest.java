package org.ironrhino.core.dataroute;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.annotation.Resource;
import javax.sql.DataSource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = GroupedDataSourceConfiguration.class)
public class GroupedDataSourceTest {

	@Autowired
	private PetRepository petRepository;

	@Resource
	private DataSource masterDataSource;

	@Resource
	private DataSource slaveDataSource;

	@Test
	public void test() throws Exception {
		Pet p = new Pet();
		p.setName("test");
		petRepository.save(p);
		Pet p2 = petRepository.get(p.getName());
		assertThat(p2, is(nullValue()));
		sync(masterDataSource, slaveDataSource);
		p2 = petRepository.get(p.getName());
		assertThat(p2, is(p));
	}

	private static void sync(DataSource from, DataSource to) throws SQLException {
		try (Connection conn1 = from.getConnection();
				Statement stmt1 = conn1.createStatement();
				ResultSet rs = stmt1.executeQuery("select * from pet")) {
			try (Connection conn2 = to.getConnection();
					PreparedStatement stmt2 = conn2.prepareStatement("insert into pet(name) values(?)");
					Statement stmt3 = conn2.createStatement()) {
				stmt3.execute("delete from pet");
				while (rs.next()) {
					stmt2.setString(1, rs.getString(1));
					stmt2.addBatch();
				}
				stmt2.executeBatch();
				conn2.commit();
			}
		}
	}

}
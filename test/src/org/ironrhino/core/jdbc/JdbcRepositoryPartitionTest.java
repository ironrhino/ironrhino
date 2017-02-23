package org.ironrhino.core.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(locations = { "ctx.xml" })
public class JdbcRepositoryPartitionTest {

	@Autowired
	private CustomerRepository customerRepository;

	@Autowired
	private CustomerPartitioner customerPartitioner;

	@Before
	public void setup() {
		for (char c = '0'; c <= '9'; c++)
			customerRepository.createTable(customerPartitioner.buildPartition(c));
		customerRepository.createTable(customerPartitioner.buildPartition('X'));
	}

	@After
	public void cleanup() {
		for (char c = '0'; c <= '9'; c++)
			customerRepository.dropTable(customerPartitioner.buildPartition(c));
		customerRepository.dropTable(customerPartitioner.buildPartition('X'));
	}

	@Test
	public void test() throws Exception {
		for (char c = '0'; c <= '9'; c++) {
			String identifyNo = "123332222" + c;
			test(identifyNo);
		}
		String identifyNo = "123332222X";
		test(identifyNo);
	}

	private void test(String identifyNo) {
		Customer customer = new Customer();
		customer.setIdentifyNo(identifyNo);
		customer.setName("test" + identifyNo);
		customerRepository.save(customer);
		Customer customer2 = customerRepository.get(customer.getIdentifyNo());
		assertEquals(customer.getIdentifyNo(), customer2.getIdentifyNo());
		assertEquals(customer.getName(), customer2.getName());
		List<Customer> customers = customerRepository.list(customerPartitioner.partition(identifyNo));
		assertEquals(1, customers.size());
		assertEquals(customer.getIdentifyNo(), customers.get(0).getIdentifyNo());
		assertEquals(customer.getName(), customers.get(0).getName());
		customerRepository.delete(customer.getIdentifyNo());
		Customer customer3 = customerRepository.get(customer.getIdentifyNo());
		assertNull(customer3);
	}

}

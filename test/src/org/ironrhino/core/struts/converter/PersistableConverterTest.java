package org.ironrhino.core.struts.converter;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.UUID;

import org.ironrhino.sample.crud.Customer;
import org.junit.Test;

public class PersistableConverterTest extends ConverterTestBase<PersistableConverter> {

	@Test
	public void convertFromString() {
		String id = UUID.randomUUID().toString();
		Customer customer = (Customer) converter.convertFromString(null, new String[]{id}, Customer.class);
		assertThat(customer.getId(), is(id));
	}

	@Test
	public void convertToString() {
		String id = UUID.randomUUID().toString();
		Customer customer = new Customer();
		customer.setId(id);
		assertThat(converter.convertToString(null, customer), is(id));
	}
}
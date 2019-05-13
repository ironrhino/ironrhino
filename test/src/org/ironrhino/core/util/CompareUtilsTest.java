package org.ironrhino.core.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;

import org.ironrhino.sample.crud.Customer;
import org.ironrhino.sample.crud.CustomerAddress;
import org.ironrhino.sample.crud.Employee;
import org.junit.Test;

public class CompareUtilsTest {

	@Test
	public void testEquals() {
		assertThat(CompareUtils.equals(null, null), equalTo(true));
		assertThat(CompareUtils.equals(1, 2), equalTo(false));
		assertThat(CompareUtils.equals(1, 1), equalTo(true));
		assertThat(CompareUtils.equals(1, 1L), equalTo(false));
		assertThat(CompareUtils.equals(YearMonth.of(2012, 8), YearMonth.of(2012, 8)), equalTo(true));
		assertThat(CompareUtils.equals(YearMonth.of(2012, 9), YearMonth.of(2012, 8)), equalTo(false));
		assertThat(CompareUtils.equals(new int[] { 1, 2 }, new int[] { 1, 2 }), equalTo(true));
		assertThat(CompareUtils.equals(new int[] { 1, 2 }, new int[] { 1, 2, 3 }), equalTo(false));
		assertThat(CompareUtils.equals(new Object[] { 1, "" }, new Object[] { 1, "", "" }), equalTo(false));
		assertThat(CompareUtils.equals(new Object[] { 1, "" }, new Object[] { 1, "" }), equalTo(true));
		assertThat(CompareUtils.equals(new Object[] {}, new Object[] {}), equalTo(true));
		assertThat(CompareUtils.equals(new Object[] {}, null), equalTo(true));
		assertThat(CompareUtils.equals(new ArrayList<String>(), new ArrayList<String>()), equalTo(true));
		assertThat(CompareUtils.equals(new ArrayList<String>(), null), equalTo(true));
		Customer c1 = new Customer();
		c1.setId("id");
		c1.setName("name");
		Customer c2 = new Customer();
		assertThat(CompareUtils.equals(c1, c2), equalTo(false));
		c2.setId("id");
		assertThat(CompareUtils.equals(c1, c2), equalTo(true));
		assertThat(CompareUtils.equals(Collections.singletonList(c1), Collections.singletonList(c2)), equalTo(true));
		assertThat(CompareUtils.equals(new Customer[] { c1 }, new Customer[] { c2 }), equalTo(true));
		CustomerAddress ca1 = new CustomerAddress();
		CustomerAddress ca2 = new CustomerAddress();
		assertThat(CompareUtils.equals(ca1, ca2), equalTo(true));
		Employee emp1 = new Employee();
		ca1.setReceiver(emp1);
		assertThat(CompareUtils.equals(ca1, ca2), equalTo(false));
		Employee emp2 = new Employee();
		ca2.setReceiver(emp2);
		assertThat(CompareUtils.equals(ca1, ca2), equalTo(true));
		assertThat(CompareUtils.equals(Collections.singletonList(ca1), Collections.singletonList(ca2)), equalTo(true));
		assertThat(CompareUtils.equals(new CustomerAddress[] { ca1 }, new CustomerAddress[] { ca2 }), equalTo(true));
	}

}

package org.ironrhino.core.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
		assertTrue(CompareUtils.equals(null, null));
		assertFalse(CompareUtils.equals(1, 2));
		assertTrue(CompareUtils.equals(1, 1));
		assertFalse(CompareUtils.equals(1, 1L));
		assertTrue(CompareUtils.equals(YearMonth.of(2012, 8), YearMonth.of(2012, 8)));
		assertFalse(CompareUtils.equals(YearMonth.of(2012, 9), YearMonth.of(2012, 8)));
		assertTrue(CompareUtils.equals(new int[] { 1, 2 }, new int[] { 1, 2 }));
		assertFalse(CompareUtils.equals(new int[] { 1, 2 }, new int[] { 1, 2, 3 }));
		assertFalse(CompareUtils.equals(new Object[] { 1, "" }, new Object[] { 1, "", "" }));
		assertTrue(CompareUtils.equals(new Object[] { 1, "" }, new Object[] { 1, "" }));
		assertTrue(CompareUtils.equals(new Object[] {}, new Object[] {}));
		assertTrue(CompareUtils.equals(new Object[] {}, null));
		assertTrue(CompareUtils.equals(new ArrayList<String>(), new ArrayList<String>()));
		assertTrue(CompareUtils.equals(new ArrayList<String>(), null));
		Customer c1 = new Customer();
		c1.setId("id");
		c1.setName("name");
		Customer c2 = new Customer();
		assertFalse(CompareUtils.equals(c1, c2));
		c2.setId("id");
		assertTrue(CompareUtils.equals(c1, c2));
		assertTrue(CompareUtils.equals(Collections.singletonList(c1), Collections.singletonList(c2)));
		assertTrue(CompareUtils.equals(new Customer[] { c1 }, new Customer[] { c2 }));
		CustomerAddress ca1 = new CustomerAddress();
		CustomerAddress ca2 = new CustomerAddress();
		assertTrue(CompareUtils.equals(ca1, ca2));
		Employee emp1 = new Employee();
		ca1.setReceiver(emp1);
		assertFalse(CompareUtils.equals(ca1, ca2));
		Employee emp2 = new Employee();
		ca2.setReceiver(emp2);
		assertTrue(CompareUtils.equals(ca1, ca2));
		assertTrue(CompareUtils.equals(Collections.singletonList(ca1), Collections.singletonList(ca2)));
		assertTrue(CompareUtils.equals(new CustomerAddress[] { ca1 }, new CustomerAddress[] { ca2 }));
	}

}

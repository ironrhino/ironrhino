package org.ironrhino.core.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import org.ironrhino.common.model.Coordinate;
import org.junit.Test;

public class CoordinateTest {

	@Test
	public void testDistanceFrom() {
		test(new Coordinate(23.00, 143.00), 3000);
		test(new Coordinate(23.00, -143.00), 3000);
		test(new Coordinate(-23.00, 143.00), 3000);
		test(new Coordinate(-23.00, -143.00), 3000);
		test(new Coordinate(23.00, 0.00), 3000);
		test(new Coordinate(-23.00, 0.00), 3000);
		test(new Coordinate(0.00, 143.00), 3000);
		test(new Coordinate(0.00, -143.00), 3000);
		test(new Coordinate(90.00, 143.00), 3000);
		test(new Coordinate(90.00, -143.00), 3000);
		test(new Coordinate(-89.99, 143.00), 3000);
		test(new Coordinate(-89.99, -143.00), 3000);
	}

	public void test(Coordinate coord, int distance) {
		Coordinate c2 = coord.moveVertical(distance);
		assertThat(coord.distanceFrom(c2) - distance <= 1, equalTo(true));
		c2 = coord.moveVertical(-distance);
		assertThat(coord.distanceFrom(c2) - distance <= 1, equalTo(true));
		if (Math.abs(coord.getLatitude()) != 90) {
			c2 = coord.moveHorizontal(distance);
			assertThat(coord.distanceFrom(c2) - distance <= 1, equalTo(true));
			c2 = coord.moveHorizontal(-distance);
			assertThat(coord.distanceFrom(c2) - distance <= 1, equalTo(true));
		}
	}

}

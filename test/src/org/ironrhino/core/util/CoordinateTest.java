package org.ironrhino.core.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.ironrhino.common.model.Coordinate.EARTH_PERIMETER;
import static org.ironrhino.common.model.Coordinate.EARTH_RADIUS;

import org.ironrhino.common.model.Coordinate;
import org.junit.Test;

public class CoordinateTest {

	@Test
	public void testDistanceFrom() {
		for (int i = -64; i < 64; i++) {
			int distance = i * (int) EARTH_PERIMETER >> 4;
			test(new Coordinate(23.00, 143.00), distance);
			test(new Coordinate(23.00, -143.00), distance);
			test(new Coordinate(-23.00, 143.00), distance);
			test(new Coordinate(-23.00, -143.00), distance);
			test(new Coordinate(23.00, 0.00), distance);
			test(new Coordinate(-23.00, 0.00), distance);
			test(new Coordinate(0.00, 143.00), distance);
			test(new Coordinate(0.00, -143.00), distance);
			test(new Coordinate(90.00, 143.00), distance);
			test(new Coordinate(90.00, -143.00), distance);
			test(new Coordinate(-89.99, 143.00), distance);
			test(new Coordinate(-89.99, -143.00), distance);

			for (double j = -90; j <= 90; j += 10) {
				for (double k = -170; k <= 180; k++) {
					test(new Coordinate(j, k), distance);
				}
			}
		}
	}

	public void test(Coordinate coord, int distance) {
		Coordinate c2 = verify(coord.moveVertical(distance));
		distance = (int) (Math.abs(distance) % EARTH_PERIMETER);
		assertThat(Math.abs(coord.distanceFrom(c2) - distance) <= 2
				|| Math.abs(coord.distanceFrom(c2) + distance - EARTH_PERIMETER) <= 2, equalTo(true));

		c2 = verify(coord.moveHorizontal(distance));
		distance = (int) (Math.abs(distance)
				% (2 * Math.PI * EARTH_RADIUS * Math.cos(Coordinate.radian(coord.getLatitude()))));
		assertThat(distance >= coord.distanceFrom(c2), is(true));
	}

	private Coordinate verify(Coordinate c) {
		assertThat(c, is(notNullValue()));
		assertThat(c.getLatitude() <= 90, is(true));
		assertThat(c.getLatitude() >= -90, is(true));
		assertThat(c.getLongitude() <= 180, is(true));
		assertThat(c.getLongitude() >= -180, is(true));
		return c;
	}
}

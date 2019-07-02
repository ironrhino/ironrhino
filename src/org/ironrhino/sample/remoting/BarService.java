package org.ironrhino.sample.remoting;

import org.ironrhino.core.remoting.Remoting;

@Remoting
public interface BarService {

	String test(String value);

}

package org.ironrhino.sample.remoting;

import org.ironrhino.core.remoting.Remoting;

@Remoting
public interface FooService {

	public String test(String value);

}

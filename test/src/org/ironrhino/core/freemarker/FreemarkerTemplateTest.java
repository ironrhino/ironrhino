package org.ironrhino.core.freemarker;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.ironrhino.core.metadata.Scope;
import org.ironrhino.core.util.DateUtils;
import org.junit.Test;

import freemarker.template.Configuration;
import freemarker.template.Template;

public class FreemarkerTemplateTest {

	@Test
	public void testMapModel() throws Exception {
		String template0 = "${map.isEmpty()?c} ${map.empty?c}";
		String template1 = "<#list map.values() as value>${value}</#list>";
		String template2 = "<#list map?keys as key>${key}</#list>";
		String template3 = "<#list map?values as value>${value}</#list>";
		String template4 = "<#list map as key,value>${value}</#list>";
		String template5 = "<#list map as key,value>${key.class.simpleName} ${value.class.simpleName}</#list>";
		Map<String, String> map = new HashMap<>();
		map.put("size", "size");
		Map<String, Object> dataModel = Collections.singletonMap("map", map);
		assertThat(render(template0, dataModel), is("false false"));
		assertThat(render(template1, dataModel), is("size"));
		assertThat(render(template2, dataModel), is("size"));
		assertThat(render(template3, dataModel), is("size"));
		assertThat(render(template4, dataModel), is("size"));
		assertThat(render(template5, dataModel), is("String String"));
	}

	@Test
	public void testStatics() throws Exception {
		String template1 = "${statics['org.ironrhino.core.util.DateUtils'].formatDate8(today)}";
		String template2 = "${statics['org.ironrhino.core.metadata.Scope'].LOCAL.name()}";
		Date today = new Date();
		Map<String, Object> dataModel = Collections.singletonMap("today", today);
		assertThat(render(template1, dataModel), is(DateUtils.formatDate8(today)));
		assertThat(render(template2, dataModel), is(Scope.LOCAL.name()));
	}

	@Test
	public void testConstants() throws Exception {
		String template = "${constants['org.ironrhino.core.metadata.Scope.LOCAL'].name()}";
		Map<String, Object> dataModel = Collections.emptyMap();
		assertThat(render(template, dataModel), is(Scope.LOCAL.name()));
	}

	static String render(String template, Object dataModel) throws Exception {
		Configuration configuration = new FreemarkerConfigurer().createConfiguration();
		StringWriter sw = new StringWriter();
		new Template("", "", new StringReader(template), configuration, "UTF-8").process(dataModel, sw);
		return sw.getBuffer().toString();
	}

}

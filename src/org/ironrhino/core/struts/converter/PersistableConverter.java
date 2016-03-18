package org.ironrhino.core.struts.converter;

import java.io.Serializable;
import java.util.Map;

import org.apache.struts2.util.StrutsTypeConverter;
import org.ironrhino.core.model.Persistable;
import org.springframework.beans.BeanWrapperImpl;

@SuppressWarnings("rawtypes")
public class PersistableConverter extends StrutsTypeConverter {

	@Override
	public Object convertFromString(Map context, String[] values, Class toClass) {
		if (values[0] == null || values[0].trim().equals(""))
			return null;
		try {
			Persistable<?> persistable = (Persistable<?>) toClass.newInstance();
			BeanWrapperImpl bw = new BeanWrapperImpl(persistable);
			bw.setPropertyValue("id", values[0].trim());
			return persistable;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public String convertToString(Map arg0, Object o) {
		if (o instanceof Persistable) {
			Serializable id = ((Persistable) o).getId();
			return String.valueOf(id);
		}
		return "";
	}

}

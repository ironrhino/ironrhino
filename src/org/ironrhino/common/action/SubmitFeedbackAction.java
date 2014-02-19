package org.ironrhino.common.action;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.common.model.Feedback;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.service.EntityManager;
import org.ironrhino.core.struts.BaseAction;

import com.opensymphony.xwork2.interceptor.annotations.InputConfig;

@AutoConfig(namespace = "/")
public class SubmitFeedbackAction extends BaseAction {

	private static final long serialVersionUID = -8376029703532190694L;

	private String domain;

	private String name;

	private String contact;

	private String content;

	@Resource
	private EntityManager<Feedback> entityManager;

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getContact() {
		return contact;
	}

	public void setContact(String contact) {
		this.contact = contact;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	@Override
	@InputConfig(resultName = INPUT)
	public String execute() {
		if (StringUtils.isNotBlank(content)) {
			Feedback f = new Feedback();
			f.setName(name);
			f.setContact(contact);
			f.setContent(content);
			if (StringUtils.isBlank(domain))
				domain = null;
			f.setDomain(domain);
			entityManager.save(f);
			addActionMessage(getText("save.success"));
		}
		return INPUT;
	}

}

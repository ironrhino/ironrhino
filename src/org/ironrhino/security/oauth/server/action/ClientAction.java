package org.ironrhino.security.oauth.server.action;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.ironrhino.core.metadata.Authorize;
import org.ironrhino.core.model.ResultPage;
import org.ironrhino.core.security.role.UserRole;
import org.ironrhino.core.service.BaseManager;
import org.ironrhino.core.struts.EntityAction;
import org.ironrhino.core.util.AuthzUtils;
import org.ironrhino.security.model.User;
import org.ironrhino.security.oauth.server.model.Client;

import com.opensymphony.xwork2.interceptor.annotations.InputConfig;
import com.opensymphony.xwork2.validator.annotations.RequiredStringValidator;
import com.opensymphony.xwork2.validator.annotations.Validations;
import com.opensymphony.xwork2.validator.annotations.ValidatorType;

public class ClientAction extends EntityAction<Client> {

	private static final long serialVersionUID = -4833030589707102084L;

	private Client client;

	public Client getClient() {
		return client;
	}

	public void setClient(Client client) {
		this.client = client;
	}

	@InputConfig(resultName = "apply")
	@Authorize(ifAllGranted = UserRole.ROLE_BUILTIN_USER)
	@Validations(requiredStrings = {
			@RequiredStringValidator(type = ValidatorType.FIELD, fieldName = "client.name", trim = true, key = "validation.required"),
			@RequiredStringValidator(type = ValidatorType.FIELD, fieldName = "client.redirectUri", trim = true, key = "validation.required") })
	public String apply() {
		BaseManager<Client> entityManager = getEntityManager(Client.class);
		client.setOwner(AuthzUtils.<User> getUserDetails());
		entityManager.save(client);
		addActionMessage(getText("save.success"));
		return SUCCESS;
	}

	@Authorize(ifAllGranted = UserRole.ROLE_BUILTIN_USER)
	@SuppressWarnings("unchecked")
	public String mine() {
		BaseManager<Client> entityManager = getEntityManager(Client.class);
		DetachedCriteria dc = entityManager.detachedCriteria();
		dc.add(Restrictions.eq("owner", AuthzUtils.<User> getUserDetails()));
		if (resultPage == null)
			resultPage = new ResultPage<Client>();
		resultPage.setCriteria(dc);
		resultPage = entityManager.findByResultPage(resultPage);
		return "mine";
	}

	@Authorize(ifAllGranted = UserRole.ROLE_BUILTIN_USER)
	public String show() {
		BaseManager<Client> entityManager = getEntityManager(Client.class);
		client = entityManager.get(getUid());
		return "show";
	}

	@Override
	@Authorize(ifAllGranted = UserRole.ROLE_BUILTIN_USER)
	public String disable() {
		BaseManager<Client> entityManager = getEntityManager(Client.class);
		String[] id = getId();
		if (id != null) {
			List<Client> list;
			if (id.length == 1) {
				list = new ArrayList<Client>(1);
				list.add(entityManager.get(id[0]));
			} else {
				DetachedCriteria dc = entityManager.detachedCriteria();
				dc.add(Restrictions.in("id", id));
				list = entityManager.findListByCriteria(dc);
			}
			if (list.size() > 0) {
				for (Client temp : list) {
					if (AuthzUtils.getUsername().equals(
							temp.getOwner().getUsername())) {
						temp.setEnabled(false);
						entityManager.save(temp);
					}
				}
				addActionMessage(getText("operate.success"));
			}
		}
		return REFERER;
	}

}

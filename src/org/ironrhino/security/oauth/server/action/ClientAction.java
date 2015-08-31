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

	@Override
	@Authorize(ifAllGranted = UserRole.ROLE_BUILTIN_USER)
	public String checkavailable() {
		return super.checkavailable();
	}

	@InputConfig(resultName = "apply")
	@Authorize(ifAllGranted = UserRole.ROLE_BUILTIN_USER)
	@Validations(requiredStrings = {
			@RequiredStringValidator(type = ValidatorType.FIELD, fieldName = "client.name", trim = true, key = "validation.required"),
			@RequiredStringValidator(type = ValidatorType.FIELD, fieldName = "client.redirectUri", trim = true, key = "validation.required") })
	public String apply() {
		client.setEnabled(false);
		client.setOwner(AuthzUtils.<User> getUserDetails());
		getEntityManager(Client.class).save(client);
		addActionMessage(getText("save.success"));
		return SUCCESS;
	}

	@Authorize(ifAllGranted = UserRole.ROLE_BUILTIN_USER)
	public String mine() {
		BaseManager<Client> clientManager = getEntityManager(Client.class);
		DetachedCriteria dc = clientManager.detachedCriteria();
		dc.add(Restrictions.eq("owner", AuthzUtils.<User> getUserDetails()));
		if (resultPage == null)
			resultPage = new ResultPage<>();
		resultPage.setCriteria(dc);
		resultPage = clientManager.findByResultPage(resultPage);
		return "mine";
	}

	@Authorize(ifAllGranted = UserRole.ROLE_BUILTIN_USER)
	public String show() {
		client = getEntityManager(Client.class).get(getUid());
		return "show";
	}

	@Override
	@Authorize(ifAllGranted = UserRole.ROLE_BUILTIN_USER)
	public String disable() {
		BaseManager<Client> clientManager = getEntityManager(Client.class);
		String[] id = getId();
		if (id != null) {
			List<Client> list;
			if (id.length == 1) {
				list = new ArrayList<>(1);
				list.add(clientManager.get(id[0]));
			} else {
				DetachedCriteria dc = clientManager.detachedCriteria();
				dc.add(Restrictions.in("id", id));
				list = clientManager.findListByCriteria(dc);
			}
			if (list.size() > 0) {
				for (Client temp : list) {
					if (AuthzUtils.authorize(null, UserRole.ROLE_ADMINISTRATOR, null)
							|| AuthzUtils.getUsername().equals(temp.getOwner().getUsername())) {
						temp.setEnabled(false);
						clientManager.save(temp);
					}
				}
				addActionMessage(getText("operate.success"));
			}
		}
		return REFERER;
	}

}

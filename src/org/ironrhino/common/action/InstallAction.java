package org.ironrhino.common.action;

import java.io.File;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import org.ironrhino.common.support.InstallationManager;
import org.ironrhino.common.support.InstallationManager.Component;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.struts.BaseAction;

@AutoConfig(fileupload = "application/java-archive,application/octet-stream")
public class InstallAction extends BaseAction {

	private static final long serialVersionUID = 625504391613761721L;

	private File file;

	@Autowired
	private InstallationManager installationManager;

	public List<Component> getList() {
		return installationManager.getInstalledComponents();
	}

	public void setFile(File file) {
		this.file = file;
	}

	@Override
	public String input() {
		return INPUT;
	}

	@Override
	public String execute() {
		return LIST;
	}

	public String install() {
		installationManager.install(file);
		return SUCCESS;
	}

	public String rollback() {
		installationManager.rollback(getUid());
		return SUCCESS;
	}

	public String uninstall() {
		installationManager.uninstall(getUid());
		return SUCCESS;
	}

}

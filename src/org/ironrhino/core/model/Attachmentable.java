package org.ironrhino.core.model;

import java.util.List;

import org.ironrhino.core.metadata.UiConfig;

public interface Attachmentable {

	@UiConfig(hidden = true)
	public List<String> getAttachments();

	public void setAttachments(List<String> attachments);

}

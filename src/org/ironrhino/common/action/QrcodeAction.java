package org.ironrhino.common.action;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Base64;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.metadata.JsonConfig;
import org.ironrhino.core.struts.BaseAction;
import org.ironrhino.core.util.BarcodeUtils;
import org.ironrhino.core.util.ErrorMessage;

import com.google.zxing.NotFoundException;
import com.opensymphony.xwork2.interceptor.annotations.InputConfig;

import lombok.Getter;
import lombok.Setter;

@AutoConfig(fileupload = "image/*")
public class QrcodeAction extends BaseAction {

	private static final long serialVersionUID = 8180265410790553918L;

	@Getter
	@Setter
	private boolean decode = false;

	@Getter
	@Setter
	private String content;

	@Getter
	@Setter
	private String encoding = "UTF-8";

	@Getter
	@Setter
	private String url;

	@Getter
	@Setter
	private File file;

	@Getter
	@Setter
	private int width = 400;

	@Getter
	@Setter
	private int height = 400;

	@Override
	@InputConfig(resultName = SUCCESS)
	@JsonConfig(root = "content")
	public String execute() {
		try {
			if (decode) {
				if (file == null && StringUtils.isBlank(url) && StringUtils.isBlank(requestBody)) {
					addActionError("validation.required");
					return SUCCESS;
				}
				if (file != null)
					try {
						content = BarcodeUtils.decode(new FileInputStream(file), encoding);
					} catch (NotFoundException e) {
						addActionError(getText("notfound"));
					}
				else if (StringUtils.isNotBlank(url))
					try {
						content = BarcodeUtils.decode(url, encoding);
					} catch (NotFoundException e) {
						addActionError(getText("notfound"));
					}
				else if (StringUtils.isNotBlank(requestBody)) {
					if (requestBody.startsWith("data:image"))
						requestBody = requestBody.substring(requestBody.indexOf(',') + 1);
					try {
						content = BarcodeUtils.decode(new ByteArrayInputStream(Base64.getDecoder().decode(requestBody)),
								encoding);
					} catch (NotFoundException e) {
						content = "";
					}
					return JSON;
				}
				return SUCCESS;
			} else {
				if (content == null) {
					addFieldError("content", getText("validation.required"));
					return SUCCESS;
				}
				BarcodeUtils.encodeQRCode(content, null, null, width, height,
						ServletActionContext.getResponse().getOutputStream(),
						file != null ? new FileInputStream(file) : null);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new ErrorMessage(e.getMessage());
		}
		return NONE;
	}
}

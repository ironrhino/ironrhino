package org.ironrhino.core.struts.result;

import javax.servlet.http.HttpServletResponse;

import org.apache.struts2.ServletActionContext;
import org.apache.struts2.StrutsConstants;
import org.ironrhino.core.util.BarcodeUtils;

import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.Result;
import com.opensymphony.xwork2.inject.Inject;

public class QRCodeResult implements Result {

	private static final long serialVersionUID = 5984356746581381755L;

	@Inject(StrutsConstants.STRUTS_I18N_ENCODING)
	private String encoding = "UTF-8";

	@Inject(value = "qrcode.format", required = false)
	private String format = "png";

	@Inject(value = "qrcode.width", required = false)
	private int width = -1;

	@Inject(value = "qrcode.height", required = false)
	private int height = -1;

	@Override
	public void execute(ActionInvocation invocation) throws Exception {
		String content = invocation.getStack().findValue("responseBody")
				.toString();
		HttpServletResponse response = ServletActionContext.getResponse();
		response.setContentType("image/" + format);
		if (!response.containsHeader("Cache-Control")) {
			response.setHeader("Cache-Control", "no-cache");
			response.setHeader("Pragma", "no-cache");
			response.setDateHeader("Expires", 0);
		}
		BarcodeUtils.encodeQRCode(content, encoding, format, width, height,
				response.getOutputStream());
	}
}
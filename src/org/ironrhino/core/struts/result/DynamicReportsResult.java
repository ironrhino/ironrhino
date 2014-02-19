package org.ironrhino.core.struts.result;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.StrutsStatics;
import org.apache.struts2.dispatcher.StrutsResultSupport;

import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.util.ValueStack;

public class DynamicReportsResult extends StrutsResultSupport {

	private static final long serialVersionUID = -2433174799621182907L;

	protected String format;
	protected String documentName;
	protected String contentDisposition;
	protected String jasperReportBuilder;

	public DynamicReportsResult() {
		super();
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public void setDocumentName(String documentName) {
		this.documentName = documentName;
	}

	public void setContentDisposition(String contentDisposition) {
		this.contentDisposition = contentDisposition;
	}

	public void setJasperReportBuilder(String jasperReportBuilder) {
		this.jasperReportBuilder = jasperReportBuilder;
	}

	@Override
	protected void doExecute(String finalLocation, ActionInvocation invocation)
			throws Exception {
		initializeProperties(invocation);
		HttpServletRequest request = (HttpServletRequest) invocation
				.getInvocationContext().get(StrutsStatics.HTTP_REQUEST);
		HttpServletResponse response = (HttpServletResponse) invocation
				.getInvocationContext().get(StrutsStatics.HTTP_RESPONSE);

		// Handle IE special case: it sends a "contype" request first.
		if ("contype".equals(request.getHeader("User-Agent"))) {
			try {
				response.setContentType("application/pdf");
				response.setContentLength(0);

				ServletOutputStream outputStream = response.getOutputStream();
				outputStream.close();
			} catch (IOException e) {
				throw new ServletException(e.getMessage(), e);
			}
			return;
		}

		ValueStack stack = invocation.getStack();
		if (StringUtils.isBlank(jasperReportBuilder))
			jasperReportBuilder = "jasperReportBuilder";
		JasperReportBuilder jrb = (JasperReportBuilder) stack
				.findValue(jasperReportBuilder);

		// Export the print object to the desired output format
		if (contentDisposition != null || documentName != null) {
			final StringBuffer tmp = new StringBuffer();
			tmp.append((contentDisposition == null) ? "inline"
					: contentDisposition);

			if (documentName != null) {
				tmp.append("; filename=");
				tmp.append(documentName);
				tmp.append(".");
				tmp.append(format.toLowerCase());
			}

			response.setHeader("Content-disposition", tmp.toString());
		}

		if (format.equalsIgnoreCase("PDF")) {
			response.setContentType("application/pdf");
			jrb.toPdf(response.getOutputStream());
		} else if (format.equalsIgnoreCase("XLS")) {
			response.setContentType("application/vnd.ms-excel");
			jrb.toXls(response.getOutputStream());
		} else {
			throw new ServletException("Unknown report format: " + format);
		}
	}

	private void initializeProperties(ActionInvocation invocation)
			throws Exception {
		ValueStack stack = invocation.getStack();
		format = conditionalParse(format, invocation);
		if (StringUtils.isEmpty(format))
			format = (String) stack.findValue("format");
		if (StringUtils.isEmpty(format))
			format = "PDF";

		if (contentDisposition != null)
			contentDisposition = conditionalParse(contentDisposition,
					invocation);
		if (StringUtils.isEmpty(contentDisposition))
			contentDisposition = (String) stack.findValue("contentDisposition");

		if (documentName != null)
			documentName = conditionalParse(documentName, invocation);
		if (StringUtils.isEmpty(documentName))
			documentName = (String) stack.findValue("documentName");

	}

}

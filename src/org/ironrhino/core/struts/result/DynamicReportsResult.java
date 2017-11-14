package org.ironrhino.core.struts.result;

import java.io.IOException;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.StrutsStatics;
import org.apache.struts2.dispatcher.StrutsResultSupport;

import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.util.ValueStack;

import lombok.Setter;
import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;

public class DynamicReportsResult extends StrutsResultSupport {

	private static final long serialVersionUID = -2433174799621182907L;

	public static final String FORMAT_PDF = "pdf";
	public static final String FORMAT_XLS = "xls";
	public static final String FORMAT_XLSX = "xlsx";

	@Setter
	protected String format;
	@Setter
	protected String documentName;
	@Setter
	protected String contentDisposition;
	@Setter
	protected String jasperReportBuilder;

	@Override
	protected void doExecute(String finalLocation, ActionInvocation invocation) throws Exception {
		initializeProperties(invocation);
		HttpServletRequest request = (HttpServletRequest) invocation.getInvocationContext()
				.get(StrutsStatics.HTTP_REQUEST);
		HttpServletResponse response = (HttpServletResponse) invocation.getInvocationContext()
				.get(StrutsStatics.HTTP_RESPONSE);
		if (format == null) {
			format = request.getParameter("format");
			if (format != null)
				format = format.toLowerCase(Locale.ROOT);
		}
		if (format == null)
			format = FORMAT_PDF;
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
		JasperReportBuilder jrb = (JasperReportBuilder) stack.findValue(jasperReportBuilder);

		// Export the print object to the desired output format
		if (contentDisposition != null || documentName != null) {
			final StringBuffer tmp = new StringBuffer();
			tmp.append((contentDisposition == null) ? "inline" : contentDisposition);

			if (documentName != null) {
				tmp.append("; filename=");
				tmp.append(documentName);
				tmp.append(".");
				tmp.append(format.toLowerCase(Locale.ROOT));
			}

			response.setHeader("Content-disposition", tmp.toString());
		}

		if (format.equals(FORMAT_PDF)) {
			response.setContentType("application/pdf");
			jrb.toPdf(response.getOutputStream());
		} else if (format.equals(FORMAT_XLS)) {
			response.setContentType("application/vnd.ms-excel");
			jrb.toXls(response.getOutputStream());
		} else if (format.equals(FORMAT_XLSX)) {
			response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
			jrb.toXlsx(response.getOutputStream());
		} else {
			throw new ServletException("Unknown report format: " + format);
		}
	}

	private void initializeProperties(ActionInvocation invocation) throws Exception {
		ValueStack stack = invocation.getStack();

		if (contentDisposition != null)
			contentDisposition = conditionalParse(contentDisposition, invocation);
		if (StringUtils.isEmpty(contentDisposition))
			contentDisposition = (String) stack.findValue("contentDisposition");

		if (documentName != null)
			documentName = conditionalParse(documentName, invocation);
		if (StringUtils.isEmpty(documentName))
			documentName = (String) stack.findValue("documentName");

	}

}

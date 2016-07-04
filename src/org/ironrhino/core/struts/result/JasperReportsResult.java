package org.ironrhino.core.struts.result;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.util.Map;
import java.util.TimeZone;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.apache.struts2.dispatcher.StrutsResultSupport;
import org.apache.struts2.views.jasperreports.ValueStackDataSource;
import org.apache.struts2.views.jasperreports.ValueStackShadowMap;

import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.util.ValueStack;
import com.opensymphony.xwork2.util.logging.Logger;
import com.opensymphony.xwork2.util.logging.LoggerFactory;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.export.HtmlExporter;
import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.export.JRRtfExporter;
import net.sf.jasperreports.engine.export.JRXlsExporter;
import net.sf.jasperreports.engine.export.JRXmlExporter;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.export.Exporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleHtmlExporterOutput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleWriterExporterOutput;
import net.sf.jasperreports.export.SimpleXmlExporterOutput;

public class JasperReportsResult extends StrutsResultSupport {

	public static final String FORMAT_PDF = "PDF";
	public static final String FORMAT_XLS = "XLS";
	public static final String FORMAT_XLSX = "XLSX";
	public static final String FORMAT_HTML = "HTML";
	public static final String FORMAT_XML = "XML";
	public static final String FORMAT_CSV = "CSV";
	public static final String FORMAT_RTF = "RTF";

	private static final long serialVersionUID = -2523174799621182907L;

	private final static Logger LOG = LoggerFactory.getLogger(JasperReportsResult.class);

	protected String dataSource;
	protected String format;
	protected String documentName;
	protected String contentDisposition;
	protected String timeZone;

	protected String connection;

	protected String reportParameters;

	protected String exportParameters;

	public void setDataSource(String dataSource) {
		this.dataSource = dataSource;
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

	/**
	 * set time zone id
	 *
	 * @param timeZone
	 */
	public void setTimeZone(final String timeZone) {
		this.timeZone = timeZone;
	}

	public String getReportParameters() {
		return reportParameters;
	}

	public void setReportParameters(String reportParameters) {
		this.reportParameters = reportParameters;
	}

	public String getExportParameters() {
		return exportParameters;
	}

	public void setExportParameters(String exportParameters) {
		this.exportParameters = exportParameters;
	}

	public String getConnection() {
		return connection;
	}

	public void setConnection(String connection) {
		this.connection = connection;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	protected void doExecute(String finalLocation, ActionInvocation invocation) throws Exception {
		initializeProperties(invocation);

		if (LOG.isDebugEnabled()) {
			LOG.debug("Creating JasperReport for dataSource = " + dataSource + ", format = " + format, new Object[0]);
		}

		HttpServletRequest request = ServletActionContext.getRequest();
		HttpServletResponse response = ServletActionContext.getResponse();

		// Handle IE special case: it sends a "contype" request first.
		if ("contype".equals(request.getHeader("User-Agent"))) {
			try {
				response.setContentType("application/pdf");
				response.setContentLength(0);

				ServletOutputStream outputStream = response.getOutputStream();
				outputStream.close();
			} catch (IOException e) {
				LOG.error("Error writing report output", e);
				throw new ServletException(e.getMessage(), e);
			}
			return;
		}

		ValueStack stack = invocation.getStack();
		ValueStackDataSource stackDataSource = null;

		Connection conn = (Connection) stack.findValue(connection);
		if (conn == null)
			stackDataSource = new ValueStackDataSource(stack, dataSource);

		ServletContext servletContext = ServletActionContext.getServletContext();
		String systemId = servletContext.getRealPath(finalLocation);

		Map<String, Object> parameters = new ValueStackShadowMap(stack);
		File directory = new File(systemId.substring(0, systemId.lastIndexOf(File.separator)));
		parameters.put("reportDirectory", directory);
		parameters.put(JRParameter.REPORT_LOCALE, invocation.getInvocationContext().getLocale());

		if (timeZone != null) {
			timeZone = conditionalParse(timeZone, invocation);
			final TimeZone tz = TimeZone.getTimeZone(timeZone);
			if (tz != null) {
				parameters.put(JRParameter.REPORT_TIME_ZONE, tz);
			}
		}

		Map<String, Object> reportParams = (Map<String, Object>) stack.findValue(reportParameters);
		if (reportParams != null) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Found report parameters; adding to parameters...", new Object[0]);
			}
			parameters.putAll(reportParams);
		}

		JasperPrint jasperPrint;

		try {
			JasperReport jasperReport = (JasperReport) JRLoader.loadObject(new File(systemId));
			if (conn == null)
				jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, stackDataSource);
			else
				jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, conn);
		} catch (JRException e) {
			LOG.error("Error building report for uri " + systemId, e);
			throw new ServletException(e.getMessage(), e);
		} finally {
			if (conn != null)
				try {
					conn.close();
				} catch (Exception e) {
					LOG.warn("Could not close db connection properly", e);
				}
		}

		try {
			if (contentDisposition != null || documentName != null) {
				final StringBuffer tmp = new StringBuffer();
				tmp.append((contentDisposition == null) ? "inline" : contentDisposition);

				if (documentName != null) {
					tmp.append("; filename=");
					tmp.append(documentName);
					tmp.append(".");
					tmp.append(format.toLowerCase());
				}

				response.setHeader("Content-disposition", tmp.toString());
			}

			Exporter exporter;
			format = format.toUpperCase();
			switch (format) {
			case FORMAT_PDF:
				response.setContentType("application/pdf");
				exporter = new JRPdfExporter();
				exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(response.getOutputStream()));
				break;
			case FORMAT_XLS:
				response.setContentType("application/vnd.ms-excel");
				exporter = new JRXlsExporter();
				exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(response.getOutputStream()));
				break;
			case FORMAT_XLSX:
				response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
				exporter = new JRXlsxExporter();
				exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(response.getOutputStream()));
				break;
			case FORMAT_HTML:
				response.setContentType("text/html");
				exporter = new HtmlExporter();
				exporter.setExporterOutput(new SimpleHtmlExporterOutput(response.getWriter()));
				break;
			case FORMAT_XML:
				response.setContentType("text/xml");
				exporter = new JRXmlExporter();
				exporter.setExporterOutput(new SimpleXmlExporterOutput(response.getWriter()));
				break;
			case FORMAT_CSV:
				response.setContentType("text/csv");
				exporter = new JRCsvExporter();
				exporter.setExporterOutput(new SimpleWriterExporterOutput(response.getWriter()));
				break;
			case FORMAT_RTF:
				response.setContentType("application/rtf");
				exporter = new JRRtfExporter();
				exporter.setExporterOutput(new SimpleWriterExporterOutput(response.getWriter()));
				break;
			default:
				throw new ServletException("Unknown report format: " + format);
			}

			Map<String, Object> exportParams = (Map) stack.findValue(exportParameters);
			if (exportParams != null) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("Found export parameters; adding to exporter parameters...", new Object[0]);
				}
				for (Map.Entry<String, Object> entry : exportParams.entrySet())
					exporter.getReportContext().setParameterValue(entry.getKey(), entry.getValue());
			}
			exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
			exporter.exportReport();
		} catch (JRException e) {
			String message = "Error producing " + format + " report for uri " + systemId;
			LOG.error(message, e);
			throw new ServletException(e.getMessage(), e);
		}

	}

	private void initializeProperties(ActionInvocation invocation) throws Exception {
		if (dataSource == null && connection == null) {
			String message = "No dataSource specified...";
			LOG.error(message);
			throw new RuntimeException(message);
		}
		if (dataSource != null)
			dataSource = conditionalParse(dataSource, invocation);

		format = conditionalParse(format, invocation);
		if (StringUtils.isEmpty(format)) {
			format = FORMAT_PDF;
		}

		if (contentDisposition != null) {
			contentDisposition = conditionalParse(contentDisposition, invocation);
		}

		if (documentName != null) {
			documentName = conditionalParse(documentName, invocation);
		}

		reportParameters = conditionalParse(reportParameters, invocation);
		exportParameters = conditionalParse(exportParameters, invocation);
	}

}

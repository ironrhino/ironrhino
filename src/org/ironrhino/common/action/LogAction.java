package org.ironrhino.common.action;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.Writer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.ironrhino.core.metadata.Authorize;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.security.role.UserRole;
import org.ironrhino.core.struts.BaseAction;
import org.ironrhino.core.util.AppInfo;

@AutoConfig
@Authorize(ifAnyGranted = UserRole.ROLE_ADMINISTRATOR)
public class LogAction extends BaseAction {

	private static final long serialVersionUID = 29792886600858873L;

	@Override
	public String execute() {
		return SUCCESS;
	}

	public String download() {
		File file = new File(AppInfo.getAppHome() + File.separator + "logs",
				getUid());
		if (file.exists()) {
			HttpServletResponse response = ServletActionContext.getResponse();
			response.addHeader("Content-Disposition", "attachment;filename="
					+ getUid());
			response.setContentType("application/octet-stream");
			try {
				FileInputStream fis = new FileInputStream(file);
				OutputStream os = response.getOutputStream();
				byte[] buffer = new byte[16 * 1024];
				int i;
				while ((i = fis.read(buffer, 0, buffer.length)) > -1)
					os.write(buffer, 0, i);
				fis.close();
				os.flush();
				os.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return NONE;
		}
		return execute();
	}

	public String event() {
		HttpServletRequest request = ServletActionContext.getRequest();
		request.setAttribute("decorator", "none");
		String id = request.getHeader("Last-Event-Id");
		long position = 0;
		long tail = -1;
		String temp;
		if ((temp = request.getParameter("tail")) != null)
			try {
				tail = Long.parseLong(temp);
			} catch (Exception e) {

			}
		if (StringUtils.isNotBlank(id))
			try {
				position = Long.parseLong(request.getHeader("Last-Event-Id"));
			} catch (Exception e) {

			}
		final HttpServletResponse response = ServletActionContext.getResponse();
		response.setContentType("text/event-stream");
		response.setHeader("Cache-Control", "no-cache");
		File file = new File(AppInfo.getAppHome() + File.separator + "logs",
				getUid());
		try {
			Writer w = response.getWriter();
			w.write("retry: 5000\n\n");

			RandomAccessFile raf = new RandomAccessFile(file, "r");
			if (position == 0 && tail > 0) {
				position = raf.length() - tail;
				if (position < 0)
					position = 0;
			}
			long length = raf.length();
			if (position != length) {
				if (position < length && position > 0) {
					raf.seek(position);
					w.write("event: append\n");
				} else {
					w.write("event: replace\n");
				}
				String line = null;
				while ((line = raf.readLine()) != null) {
					position = raf.getFilePointer();
					w.write("data: ");
					w.write(new String(line.getBytes("ISO-8859-1"), "UTF-8")
							+ "\n");
				}
				raf.close();
				w.write("id: " + position + "\n\n");
				w.flush();
			} else {
				if (length == 0) {
					w.write("event: remove\n");
				} else {

				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return NONE;
	}
}

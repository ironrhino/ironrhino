package org.ironrhino.common.action;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.ironrhino.core.freemarker.TemplateProvider;
import org.ironrhino.core.fs.FileStorage;
import org.ironrhino.core.metadata.Authorize;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.metadata.JsonConfig;
import org.ironrhino.core.security.role.UserRole;
import org.ironrhino.core.struts.BaseAction;
import org.ironrhino.core.util.ErrorMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.google.common.io.Files;
import com.opensymphony.xwork2.interceptor.annotations.InputConfig;

import lombok.Getter;
import lombok.Setter;

@AutoConfig(fileupload = "image/*,video/*,text/*,application/x-shockwave-flash,application/pdf,application/msword,application/vnd.ms-powerpoint,application/octet-stream,application/zip,application/x-rar-compressed")
public class UploadAction extends BaseAction {

	private static final long serialVersionUID = 625509291613761721L;

	public static final String UPLOAD_DIR = "/upload";

	@Setter
	private File[] file;

	@Setter
	private String[] fileFileName;

	@Getter
	@Setter
	private String[] filename; // for override default filename

	@Getter
	@Setter
	private String folder;

	private String folderEncoded;

	@Getter
	@Setter
	private boolean autorename;

	@Getter
	@Setter
	private boolean json;

	@Getter
	private Map<String, Boolean> files;

	@Value("${upload.excludeSuffix:jsp,jspx,php,asp,rb,py,sh}")
	private String excludeSuffix;

	@Value("${fileStorage.path:/assets}")
	protected String fileStoragePath;

	@Autowired
	private FileStorage fileStorage;

	@Autowired
	private TemplateProvider templateProvider;

	@Getter
	@Setter
	private String suffix;

	public String getUploadRootDir() {
		return UPLOAD_DIR;
	}

	public String getFolderEncoded() {
		if (folderEncoded == null) {
			if (folder != null) {
				if (folder.equals("/")) {
					folderEncoded = folder;
				} else {
					String[] arr = folder.split("/");
					StringBuilder sb = new StringBuilder();
					try {
						for (int i = 1; i < arr.length; i++) {
							sb.append("/").append(URLEncoder.encode(arr[i], "UTF-8").replaceAll("\\+", "%20"));
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
					folderEncoded = sb.toString();
				}
			} else {
				folderEncoded = folder;
			}
		}
		return folderEncoded;
	}

	@Override
	public String input() {
		return INPUT;
	}

	@Override
	@JsonConfig(root = "filename")
	@InputConfig(methodName = "list")
	public String execute() throws IOException {
		if (file != null) {
			int i = 0;
			String[] arr = excludeSuffix.split(",");
			List<String> excludes = Arrays.asList(arr);
			String[] array = new String[file.length];
			for (File f : file) {
				String fn = fileFileName[i];
				if (filename != null && filename.length > i)
					fn = filename[i];
				String suffix = fn.substring(fn.lastIndexOf('.') + 1);
				if (!excludes.contains(suffix))
					try {
						String path = createPath(fn, autorename);
						String url = doGetFileUrl(path);
						if (url.startsWith("/"))
							url = templateProvider.getAssetsBase() + url;
						array[i] = url;
						fileStorage.write(new FileInputStream(f), path);
					} catch (IOException e) {
						e.printStackTrace();
						throw new ErrorMessage(e.getMessage());
					}
				i++;
			}
			filename = array;
			notify("operate.success");
		} else if (StringUtils.isNotBlank(requestBody) && filename != null && filename.length > 0) {
			if (requestBody.startsWith("data:image"))
				requestBody = requestBody.substring(requestBody.indexOf(',') + 1);
			InputStream is = new ByteArrayInputStream(Base64.getDecoder().decode(requestBody));
			try {
				fileStorage.write(is, createPath(filename[0], autorename));
			} catch (IOException e) {
				e.printStackTrace();
				throw new ErrorMessage(e.getMessage());
			}
		}
		return json ? JSON : list();
	}

	public String list() throws IOException {
		if (folder == null) {
			folder = getUid();
			if (folder != null) {
				try {
					folder = folder.replace("__", "..");
					folder = folder.replaceAll(" ", "%20");
					folder = new URI(folder).normalize().toString();
					folder = folder.replaceAll("%20", " ");
					if (folder.contains(".."))
						folder = "";
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				folder = "";
			}
			if (folder.length() > 0 && !folder.startsWith("/"))
				folder = '/' + folder;
		}
		files = new LinkedHashMap<>();
		if (StringUtils.isNotBlank(folder))
			files.put("..", Boolean.FALSE);
		files.putAll(fileStorage.listFilesAndDirectory(Files.simplifyPath(getUploadRootDir() + folder)));
		return ServletActionContext.getRequest().getParameter("pick") != null ? "pick" : LIST;
	}

	@Override
	public String pick() throws IOException {
		list();
		return "pick";
	}

	@Override
	@Authorize(ifAnyGranted = UserRole.ROLE_ADMINISTRATOR)
	public String delete() throws IOException {
		String[] paths = getId();
		if (paths != null) {
			for (String path : paths) {
				if (!fileStorage.delete(Files.simplifyPath(getUploadRootDir() + '/' + folder + '/' + path)))
					addActionError(getText("delete.forbidden", new String[] { path }));
			}
		}
		return list();
	}

	public String mkdir() throws IOException {
		String path = getUid();
		if (path != null) {
			if (!path.startsWith("/"))
				path = '/' + path;
			folder = path;
			fileStorage.mkdir(Files.simplifyPath(getUploadRootDir() + (folder.startsWith("/") ? "" : "/") + folder));
		}
		return list();
	}

	@Authorize(ifAnyGranted = UserRole.ROLE_ADMINISTRATOR)
	public String rename() throws IOException {
		String oldName = getUid();
		if (filename == null || filename.length == 0) {
			addActionError(getText("validation.required"));
			return list();
		}
		String newName = filename[0];
		if (oldName.equals(newName))
			return list();
		if (!fileStorage.exists(Files.simplifyPath(getUploadRootDir() + '/' + folder + '/' + oldName))) {
			addActionError(getText("validation.not.exists"));
			return list();
		}
		if (fileStorage.exists(Files.simplifyPath(getUploadRootDir() + '/' + folder + '/' + newName))) {
			addActionError(getText("validation.already.exists"));
			return list();
		}
		fileStorage.rename(Files.simplifyPath(getUploadRootDir() + '/' + folder + '/' + oldName),
				Files.simplifyPath(getUploadRootDir() + '/' + folder + '/' + newName));
		return list();
	}

	@JsonConfig(root = "files")
	public String files() throws IOException {
		String path = Files.simplifyPath(getUploadRootDir() + '/' + folder);
		Map<String, Boolean> map = fileStorage.listFilesAndDirectory(path);
		files = new LinkedHashMap<>();
		String[] suffixes = null;
		if (StringUtils.isNotBlank(suffix))
			suffixes = suffix.toLowerCase(Locale.ROOT).split("\\s*,\\s*");
		for (Map.Entry<String, Boolean> entry : map.entrySet()) {
			String s = entry.getKey();
			if (!entry.getValue()) {
				files.put(Files.simplifyPath(folder + '/' + s) + "/", false);
			} else {
				if (suffixes != null) {
					boolean matches = false;
					for (String sf : suffixes)
						if (s.toLowerCase(Locale.ROOT).endsWith("." + sf))
							matches = true;
					if (!matches)
						continue;
				}
				String url = doGetFileUrl(path);
				if (url.startsWith("/"))
					url = templateProvider.getAssetsBase() + url;
				files.put(new StringBuilder(url).append("/").append(s).toString(), true);
			}
		}
		return JSON;
	}

	private String doGetFileUrl(String path) {
		String url = fileStorage.getFileUrl(path);
		if (url.indexOf("://") < 0)
			url = fileStoragePath + url;
		return url;
	}

	public String getFileUrl(String filename) {
		String path = getUploadRootDir() + getFolderEncoded() + '/' + filename;
		return doGetFileUrl(path);
	}

	private String createPath(String filename, boolean autorename) throws IOException {
		String dir = getUploadRootDir() + "/";
		if (StringUtils.isNotBlank(folder))
			dir = dir + folder + "/";
		String path = dir + filename;
		if (autorename) {
			boolean exists = fileStorage.exists(Files.simplifyPath(path));
			int i = 2;
			while (exists) {
				path = dir + '(' + (i++) + ')' + filename;
				exists = fileStorage.exists(Files.simplifyPath(path));
			}
		}
		path = Files.simplifyPath(path);
		return path;
	}

}

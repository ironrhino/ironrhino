package org.ironrhino.common.action;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.ironrhino.common.Constants;
import org.ironrhino.common.support.SettingControl;
import org.ironrhino.core.fs.FileStorage;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.metadata.JsonConfig;
import org.ironrhino.core.struts.BaseAction;
import org.ironrhino.core.struts.TemplateProvider;
import org.ironrhino.core.util.ErrorMessage;

import com.google.common.io.Files;
import com.opensymphony.xwork2.interceptor.annotations.InputConfig;

@AutoConfig(fileupload = "image/*,text/*,application/x-shockwave-flash,application/pdf,application/msword,application/vnd.ms-powerpoint,application/octet-stream")
public class UploadAction extends BaseAction {

	private static final long serialVersionUID = 625509291613761721L;

	public static final String UPLOAD_DIR = "/upload";

	private File[] file;

	private String[] fileFileName;

	private String[] filename; // for override default filename

	private String folder;

	private String folderEncoded;

	private boolean autorename;

	private Map<String, Boolean> files;

	@Autowired
	private transient FileStorage fileStorage;

	@Autowired
	private transient SettingControl settingControl;

	@Autowired
	private transient TemplateProvider templateProvider;

	private String suffix;

	public String getSuffix() {
		return suffix;
	}

	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}

	public Map<String, Boolean> getFiles() {
		return files;
	}

	public boolean isAutorename() {
		return autorename;
	}

	public void setAutorename(boolean autorename) {
		this.autorename = autorename;
	}

	public void setFolder(String folder) {
		this.folder = folder;
	}

	public String getFolder() {
		return folder;
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
							sb.append("/").append(
									URLEncoder.encode(arr[i], "UTF-8"));
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

	public void setFile(File[] file) {
		this.file = file;
	}

	public void setFileFileName(String[] fileFileName) {
		this.fileFileName = fileFileName;
	}

	public void setFilename(String[] filename) {
		this.filename = filename;
	}

	public String getFileStoragePath() {
		return settingControl.getStringValue(
				Constants.SETTING_KEY_FILE_STORAGE_PATH, "/assets");
	}

	@Override
	public String input() {
		return INPUT;
	}

	@Override
	@InputConfig(methodName = "list")
	public String execute() {
		if (file != null) {
			int i = 0;
			String[] arr = settingControl
					.getStringArray("upload.excludeSuffix");
			if (arr == null || arr.length == 0)
				arr = "jsp,jspx,php,asp,rb,py,sh".split(",");
			List<String> excludes = Arrays.asList(arr);
			for (File f : file) {
				String fn = fileFileName[i];
				if (filename != null && filename.length > i)
					fn = filename[i];
				String suffix = fn.substring(fn.lastIndexOf('.') + 1);
				if (!excludes.contains(suffix))
					try {
						fileStorage.write(new FileInputStream(f),
								createPath(fn, autorename));
					} catch (IOException e) {
						e.printStackTrace();
						throw new ErrorMessage(e.getMessage());
					}
				i++;
			}
			addActionMessage(getText("operate.success"));
		} else if (StringUtils.isNotBlank(requestBody) && filename != null
				&& filename.length > 0) {
			if (requestBody.startsWith("data:image"))
				requestBody = requestBody
						.substring(requestBody.indexOf(',') + 1);
			InputStream is = new ByteArrayInputStream(
					Base64.decodeBase64(requestBody));
			try {
				fileStorage.write(is, createPath(filename[0], autorename));
			} catch (IOException e) {
				e.printStackTrace();
				throw new ErrorMessage(e.getMessage());
			}
		}
		return list();
	}

	public String list() {
		if (folder == null) {
			folder = getUid();
			if (folder != null) {
				try {
					folder = folder.replace("__", "..");
					folder = new URI(folder).normalize().toString();
					if (folder.contains(".."))
						folder = "";
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				folder = "";
			}
			if (folder.length() > 0 && !folder.startsWith("/"))
				folder = "/" + folder;
		}
		files = new LinkedHashMap<String, Boolean>();
		if (StringUtils.isNotBlank(folder))
			files.put("..", Boolean.FALSE);
		files.putAll(fileStorage.listFilesAndDirectory(Files
				.simplifyPath(UPLOAD_DIR + folder)));
		return ServletActionContext.getRequest().getParameter("pick") != null ? "pick"
				: LIST;
	}

	@Override
	public String pick() {
		list();
		return "pick";
	}

	@Override
	public String delete() {
		String[] paths = getId();
		if (paths != null) {
			for (String path : paths) {
				if (!fileStorage.delete(Files.simplifyPath(UPLOAD_DIR + "/"
						+ folder + "/" + path)))
					addActionError(getText("delete.forbidden",
							new String[] { path }));
			}
		}
		return list();
	}

	public String mkdir() {
		String path = getUid();
		if (path != null) {
			if (!path.startsWith("/"))
				path = "/" + path;
			folder = path;
			fileStorage.mkdir(Files.simplifyPath(UPLOAD_DIR
					+ (folder.startsWith("/") ? "" : "/") + folder));
		}
		return list();
	}

	public String rename() {
		String oldName = getUid();
		if (filename == null || filename.length == 0) {
			addActionError(getText("validation.required"));
			return list();
		}
		String newName = filename[0];
		if (!fileStorage.exists(Files.simplifyPath(UPLOAD_DIR + "/" + folder
				+ "/" + oldName))) {
			addActionError(getText("validation.not.exists"));
			return list();
		}
		if (fileStorage.exists(Files.simplifyPath(UPLOAD_DIR + "/" + folder
				+ "/" + newName))) {
			addActionError(getText("validation.already.exists"));
			return list();
		}
		fileStorage.rename(
				Files.simplifyPath(UPLOAD_DIR + "/" + folder + "/" + oldName),
				Files.simplifyPath(UPLOAD_DIR + "/" + folder + "/" + newName));
		addActionMessage(getText("operate.success"));
		return list();
	}

	@JsonConfig(root = "files")
	public String files() {
		String path = Files.simplifyPath(UPLOAD_DIR + "/" + folder);
		Map<String, Boolean> map = fileStorage.listFilesAndDirectory(path);
		files = new LinkedHashMap<String, Boolean>();
		String[] suffixes = null;
		if (StringUtils.isNotBlank(suffix))
			suffixes = suffix.toLowerCase().split("\\s*,\\s*");
		for (Map.Entry<String, Boolean> entry : map.entrySet()) {
			String s = entry.getKey();
			if (!entry.getValue()) {
				files.put(Files.simplifyPath(folder + "/" + s) + "/", false);
			} else {
				if (suffixes != null) {
					boolean matches = false;
					for (String sf : suffixes)
						if (s.toLowerCase().endsWith("." + sf))
							matches = true;
					if (!matches)
						continue;
				}
				files.put(
						new StringBuilder(templateProvider.getAssetsBase())
								.append(settingControl
										.getStringValue(
												Constants.SETTING_KEY_FILE_STORAGE_PATH,
												"/assets")).append(path)
								.append("/").append(s).toString(), true);
			}
		}
		return JSON;
	}

	private String createPath(String filename, boolean autorename) {
		String dir = UPLOAD_DIR + "/";
		if (StringUtils.isNotBlank(folder))
			dir = dir + folder + "/";
		String path = dir + filename;
		if (autorename) {
			boolean exists = fileStorage.exists(Files.simplifyPath(path));
			int i = 2;
			while (exists) {
				path = dir + "(" + (i++) + ")" + filename;
				exists = fileStorage.exists(Files.simplifyPath(path));
			}
		}
		path = Files.simplifyPath(path);
		return path;
	}
}

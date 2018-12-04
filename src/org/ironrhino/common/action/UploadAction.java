package org.ironrhino.common.action;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.ironrhino.common.support.UploadFilesHandler;
import org.ironrhino.core.freemarker.FreemarkerConfigurer;
import org.ironrhino.core.fs.FileInfo;
import org.ironrhino.core.fs.FileStorage;
import org.ironrhino.core.fs.Paged;
import org.ironrhino.core.metadata.Authorize;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.metadata.JsonConfig;
import org.ironrhino.core.security.role.UserRole;
import org.ironrhino.core.spring.configuration.PriorityQualifier;
import org.ironrhino.core.struts.BaseAction;
import org.ironrhino.core.util.ErrorMessage;
import org.ironrhino.core.util.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import com.opensymphony.xwork2.interceptor.annotations.InputConfig;

import lombok.Getter;
import lombok.Setter;

@AutoConfig(fileupload = "image/*,video/*,text/*,application/javascript,application/x-shockwave-flash,application/pdf,application/msword,application/vnd.ms-excel,application/vnd.ms-powerpoint,application/vnd.openxmlformats-*,application/font-*,application/octet-stream,application/zip,application/x-rar-compressed")
public class UploadAction extends BaseAction {

	private static final long serialVersionUID = 625509291613761721L;

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
	@Setter
	private String marker;

	@Getter
	@Setter
	private String previousMarker;

	@Getter
	@Setter
	private Integer limit;

	@Getter
	private Paged<FileInfo> pagedFiles;

	@Getter
	private List<FileInfo> files;

	@Getter
	private Map<String, Boolean> fileMap; // for json output

	@Value("${upload.excludeSuffix:jsp,jspx,php,asp,rb,py,sh}")
	private String excludeSuffix;

	@Autowired
	private UploadFilesHandler uploadFilesHandler;

	@Autowired
	@Qualifier("fileStorage")
	@PriorityQualifier
	private FileStorage uploadFileStorage;

	@Autowired
	private FreemarkerConfigurer freemarkerConfigurer;

	@Getter
	@Setter
	private String suffix;

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
							url = freemarkerConfigurer.getAssetsBase() + url;
						array[i] = url;
						uploadFileStorage.write(new FileInputStream(f), path, f.length());
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
				uploadFileStorage.write(is, createPath(filename[0], autorename));
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
		String path = FileUtils.normalizePath(getUploadRootDir() + folder);
		boolean paging = uploadFileStorage.isBucketBased() || marker != null || limit != null;
		if (paging) {
			int ps = FileStorage.DEFAULT_PAGE_SIZE;
			if (limit != null)
				ps = limit;
			pagedFiles = uploadFileStorage.listFilesAndDirectory(path, ps, marker);
			List<FileInfo> temp = new ArrayList<>();
			if (StringUtils.isNotBlank(folder))
				temp.add(new FileInfo("..", false));
			temp.addAll(pagedFiles.getResult());
			pagedFiles = new Paged<>(pagedFiles.getMarker(), pagedFiles.getNextMarker(), temp);
		} else {
			files = new ArrayList<>();
			if (StringUtils.isNotBlank(folder))
				files.add(new FileInfo("..", false));
			files.addAll(uploadFileStorage.listFilesAndDirectory(path));
		}
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
				if (!uploadFileStorage.delete(FileUtils.normalizePath(getUploadRootDir() + '/' + folder + '/' + path)))
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
			uploadFileStorage
					.mkdir(FileUtils.normalizePath(getUploadRootDir() + (folder.startsWith("/") ? "" : "/") + folder));
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
		if (!uploadFileStorage.exists(FileUtils.normalizePath(getUploadRootDir() + '/' + folder + '/' + oldName))) {
			addActionError(getText("validation.not.exists"));
			return list();
		}
		if (uploadFileStorage.exists(FileUtils.normalizePath(getUploadRootDir() + '/' + folder + '/' + newName))) {
			addActionError(getText("validation.already.exists"));
			return list();
		}
		uploadFileStorage.rename(FileUtils.normalizePath(getUploadRootDir() + '/' + folder + '/' + oldName),
				FileUtils.normalizePath(getUploadRootDir() + '/' + folder + '/' + newName));
		return list();
	}

	@JsonConfig(root = "fileMap")
	public String files() throws IOException {
		String path = FileUtils.normalizePath(getUploadRootDir() + '/' + folder);
		List<FileInfo> list = uploadFileStorage.listFilesAndDirectory(path);
		fileMap = new LinkedHashMap<>();
		String[] suffixes = null;
		if (StringUtils.isNotBlank(suffix))
			suffixes = suffix.toLowerCase(Locale.ROOT).split("\\s*,\\s*");
		for (FileInfo entry : list) {
			String s = entry.getName();
			if (!entry.isFile()) {
				fileMap.put(FileUtils.normalizePath(folder + '/' + s) + "/", false);
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
					url = freemarkerConfigurer.getAssetsBase() + url;
				StringBuilder sb = new StringBuilder(url);
				if (!url.endsWith("/"))
					sb.append("/");
				sb.append(s);
				fileMap.put(sb.toString(), true);
			}
		}
		return JSON;
	}

	public String getFileUrl(String filename) {
		String path = getUploadRootDir() + getFolderEncoded() + '/' + filename;
		return doGetFileUrl(path);
	}

	private String doGetFileUrl(String path) {
		String url = uploadFileStorage.getFileUrl(path);
		if (url.indexOf("://") > 0 && uploadFileStorage.isRelativeProtocolAllowed()) {
			url = url.substring(url.indexOf(':') + 1);
		} else if (url.indexOf("://") < 0 && url.indexOf("//") != 0) {
			url = uploadFilesHandler.getFileUrl(path);
		}
		return url;
	}

	protected String getUploadRootDir() {
		return uploadFilesHandler.getUploadDir();
	}

	private String createPath(String filename, boolean autorename) throws IOException {
		String dir = getUploadRootDir() + "/";
		if (StringUtils.isNotBlank(folder))
			dir = dir + folder + "/";
		String path = dir + filename;
		if (autorename) {
			boolean exists = uploadFileStorage.exists(FileUtils.normalizePath(path));
			int i = 2;
			while (exists) {
				path = dir + '(' + (i++) + ')' + filename;
				exists = uploadFileStorage.exists(FileUtils.normalizePath(path));
			}
		}
		path = FileUtils.normalizePath(path);
		return path;
	}

}

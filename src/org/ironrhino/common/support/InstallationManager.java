package org.ironrhino.common.support;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.util.ErrorMessage;
import org.ironrhino.core.util.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class InstallationManager {

	public static final String IRONRHINO_COMPONENT_DEPENDENCE = "Ironrhino-Component-Dependence";

	public static final String IRONRHINO_COMPONENT_VERSION = "Ironrhino-Component-Version";

	public static final String IRONRHINO_COMPONENT_VENDOR = "Ironrhino-Component-Vendor";

	public static final String IRONRHINO_COMPONENT_ID = "Ironrhino-Component-Id";

	private List<Component> installedComponents;

	private List<Component> backupedComponents;

	@Value("${installationManager.directory:}")
	private String directory;

	@Autowired
	private ServletContext servletContext;

	public List<Component> getInstalledComponents() {
		if (installedComponents == null)
			scanComponents();
		return installedComponents;
	}

	public List<Component> getBackupedComponents() {
		if (backupedComponents == null)
			scanComponents();
		return backupedComponents;
	}

	@PostConstruct
	public void init() {
		if (StringUtils.isBlank(directory))
			directory = servletContext.getRealPath("/WEB-INF/lib");
	}

	private synchronized void scanComponents() {
		List<Component> list = new ArrayList<Component>();
		backupedComponents = new ArrayList<Component>();
		File dir = new File(this.directory);
		if (dir.isDirectory()) {
			File[] files = dir.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.endsWith(".jar") || name.endsWith(".jar.bak");
				}
			});
			for (File f : files) {
				Map<String, String> manifest = FileUtils.parseManifestFile(f);
				if (manifest == null || manifest.isEmpty()
						|| !manifest.containsKey(IRONRHINO_COMPONENT_ID))
					continue;
				Component component = new Component(manifest);
				component.setRealPath(f.getAbsolutePath());
				(f.getName().endsWith(".jar") ? list : backupedComponents)
						.add(component);
			}
			Collections.sort(list, new Comparator<Component>() {
				@Override
				public int compare(Component o1, Component o2) {
					return o1.getId().compareTo(o2.getId());
				}
			});
			this.installedComponents = new ArrayList<Component>();
			for (Component c : list)
				addInstalledComponent(c);
		}

	}

	private void addInstalledComponent(Component c) {
		installedComponents = getInstalledComponents();
		boolean rollbackable = false;
		for (Component back : backupedComponents) {
			if (back.getId().equals(c.getId())) {
				rollbackable = true;
				break;
			}
		}
		c.setRollbackable(rollbackable);
		Map<String, String> dependence = c.getDependence();
		if (dependence == null || dependence.isEmpty()) {
			installedComponents.add(0, c);
		} else {
			int j = -1;
			for (int i = 0; i < installedComponents.size(); i++) {
				Component temp = installedComponents.get(i);
				Map<String, String> map = temp.getDependence();
				if (map != null) {
					boolean depended = false;
					for (String ss : map.keySet()) {
						if (ss.equals(c.getId())) {
							depended = true;
							break;
						}
					}
					if (depended) {
						j = i;
						break;
					}
				}
			}
			if (j == -1)
				installedComponents.add(c);
			else
				installedComponents.add(j, c);
		}
	}

	private void checkDependence(Map<String, String> dependence) {
		if (dependence != null) {
			for (Map.Entry<String, String> entry : dependence.entrySet()) {
				boolean satisfied = false;
				for (Component c : getInstalledComponents()) {
					if (c.getId().equals(entry.getKey())) {
						if (c.getVersion().compareTo(entry.getValue()) < 0)
							throw new ErrorMessage(entry.getKey()
									+ " required version(" + entry.getValue()
									+ "),but version(" + c.getVersion()
									+ ") found!");
						else
							satisfied = true;
						break;
					}
				}
				if (!satisfied)
					throw new ErrorMessage(entry.getKey() + " not found!");
			}
		}
	}

	public void install(File f) {
		Map<String, String> manifest = FileUtils.parseManifestFile(f);
		if (manifest == null || !manifest.containsKey(IRONRHINO_COMPONENT_ID)) {
			throw new ErrorMessage("invalid component");
		}
		Component newcomp = new Component(manifest);
		Component oldcomp = null;
		for (Component c : getInstalledComponents())
			if (c.getId().equals(newcomp.getId())) {
				oldcomp = c;
				break;
			}
		checkDependence(newcomp.getDependence());
		if (oldcomp != null) {
			if (oldcomp.getVersion().compareTo(newcomp.getVersion()) >= 0)
				throw new ErrorMessage("component has installed");
			try {
				new File(oldcomp.getRealPath() + ".bak").delete();
				org.apache.commons.io.FileUtils.moveFile(
						new File(oldcomp.getRealPath()),
						new File(oldcomp.getRealPath() + ".bak"));
			} catch (IOException e) {
				throw new ErrorMessage(e.getMessage());
			}
			oldcomp.setRealPath(oldcomp.getRealPath() + ".bak");
			getBackupedComponents().add(oldcomp);
		}
		String newfilename = new StringBuilder(directory)
				.append(File.separator).append(newcomp.getId()).append("-")
				.append(newcomp.getVersion()).append(".jar").toString();
		try {
			org.apache.commons.io.FileUtils.copyFile(f, new File(newfilename));
		} catch (IOException e) {
			throw new ErrorMessage(e.getMessage());
		}
		newcomp.setRealPath(newfilename);
		addInstalledComponent(newcomp);
	}

	public void uninstall(String id) {
		Component comp = null;
		for (Component c : getInstalledComponents()) {
			if (c.getId().equals(id)) {
				comp = c;
				continue;
			}
			if (c.getDependence() == null)
				continue;
			if (c.getDependence().keySet().contains(id))
				throw new ErrorMessage("installed component(" + c.getId()
						+ ") requires component(" + id
						+ ") and cannot be uninstalled");
		}
		if (comp == null)
			throw new ErrorMessage("component(" + id
					+ ") doesn't installed yet");
		try {
			new File(comp.getRealPath() + ".uninstall").delete();
			org.apache.commons.io.FileUtils.moveFile(
					new File(comp.getRealPath()), new File(comp.getRealPath()
							+ ".uninstall"));
		} catch (IOException e) {
			throw new ErrorMessage(e.getMessage());
		}
		getInstalledComponents().remove(comp);
		comp.setRealPath(comp.getRealPath() + ".bak");
		getBackupedComponents().add(comp);
	}

	public void rollback(String id) {
		Component backcomp = null;
		for (Component c : getBackupedComponents()) {
			if (c.getId().equals(id)) {
				backcomp = c;
				break;
			}
		}
		if (backcomp == null)
			throw new ErrorMessage("component(" + id + ") doesn't backuped");

		Component instcomp = null;
		for (Component c : getInstalledComponents()) {
			if (c.getId().equals(id)) {
				instcomp = c;
				continue;
			}
			if (c.getDependence() == null)
				continue;
			if (c.getDependence().keySet().contains(id)) {
				String requiredVersion = c.getDependence().get(id);
				if (requiredVersion.compareTo(backcomp.getVersion()) > 0)
					throw new ErrorMessage("installed component(" + c.getId()
							+ ") require version(" + requiredVersion
							+ ") ,and rollback is version("
							+ backcomp.getVersion() + "),cannot rollback it");
			}
		}

		if (instcomp != null) {
			new File(instcomp.getRealPath()).delete();
			getInstalledComponents().remove(instcomp);
		}

		String newfilename = new StringBuilder(directory)
				.append(File.separator).append(backcomp.getId()).append("-")
				.append(backcomp.getVersion()).append(".jar").toString();
		try {
			new File(newfilename).delete();
			org.apache.commons.io.FileUtils.moveFile(
					new File(backcomp.getRealPath()), new File(newfilename));
		} catch (IOException e) {
			throw new ErrorMessage(e.getMessage());
		}
		backcomp.setRealPath(newfilename);
		getBackupedComponents().remove(backcomp);
		addInstalledComponent(backcomp);
	}

	public static class Component implements Serializable {

		private static final long serialVersionUID = 6347907725689068877L;

		private String id;
		private String vendor;
		private String version;
		private Map<String, String> dependence;

		private String realPath;
		private boolean rollbackable;

		public Component() {

		}

		public Component(Map<String, String> manifest) {
			this.setId(manifest.get(IRONRHINO_COMPONENT_ID));
			this.setVendor(manifest.get(IRONRHINO_COMPONENT_VENDOR));
			this.setVersion(manifest.get(IRONRHINO_COMPONENT_VERSION));
			String dependence = manifest.get(IRONRHINO_COMPONENT_DEPENDENCE);
			if (StringUtils.isNotBlank(dependence)) {
				String[] arr = dependence.split("\\s*,\\s*");
				Map<String, String> map = new HashMap<String, String>();
				for (String s : arr) {
					String[] arr2 = s.split(";", 2);
					map.put(arr2[0], arr2.length > 1 ? arr2[1] : "1.0");
				}
				this.setDependence(map);
			}
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getVendor() {
			return vendor;
		}

		public void setVendor(String vendor) {
			this.vendor = vendor;
		}

		public String getVersion() {
			return version;
		}

		public void setVersion(String version) {
			this.version = version;
		}

		public Map<String, String> getDependence() {
			return dependence;
		}

		public void setDependence(Map<String, String> dependence) {
			this.dependence = dependence;
		}

		public boolean isRollbackable() {
			return rollbackable;
		}

		public void setRollbackable(boolean rollbackable) {
			this.rollbackable = rollbackable;
		}

		public String getRealPath() {
			return realPath;
		}

		public void setRealPath(String realPath) {
			this.realPath = realPath;
		}

	}
}

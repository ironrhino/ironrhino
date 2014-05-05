package org.ironrhino.common;

/**
 * 系统常量定义
 */
public interface Constants {
    // CMS前缀定义
	String SETTING_KEY_CMS_PREFIX = "cms.";
	// CMS路径映射前缀
	String SETTING_KEY_CMS_SERIESES = SETTING_KEY_CMS_PREFIX + "serieses";
	// CMS栏目前缀
	String SETTING_KEY_CMS_COLUMNS = SETTING_KEY_CMS_PREFIX + "columns";
	// CMS内容前缀
	String SETTING_KEY_CMS_ISSUES = SETTING_KEY_CMS_PREFIX + "issues";
	// CMS栏目后缀
	String SETTING_KEY_CMS_COLUMN_SUFFIX = ".columns";
	// 可用的本地化关键词
	String SETTING_KEY_AVAILABLE_LOCALES = "availableLocales";
	// 版本号设置
	String SETTING_KEY_MANIFEST_VERSION = "manifest.version";
	// 上传文件存储路径
	String SETTING_KEY_FILE_STORAGE_PATH = "fileStorage.path";

}

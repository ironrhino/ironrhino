package org.ironrhino.security;

/**
 * 重载系统常量定义
 */
public interface Constants extends org.ironrhino.common.Constants {
    // 设置开放注册的开关
	String SETTING_KEY_SIGNUP_ENABLED = "signup.enabled";
	// 设置注册激活的开关
	String SETTING_KEY_SIGNUP_ACTIVATION_REQUIRED = "signup.activation.required";
	// 设置oauth接入的开关
	String SETTING_KEY_OAUTH_ENABLED = "oauth.enabled";
}

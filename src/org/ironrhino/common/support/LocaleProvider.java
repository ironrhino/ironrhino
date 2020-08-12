package org.ironrhino.common.support;

@FunctionalInterface
public interface LocaleProvider {

	String[] getAvailableLocales();

}

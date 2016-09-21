package org.ironrhino.core.freemarker;

import java.lang.ref.ReferenceQueue;
import java.util.Map;
import java.util.WeakHashMap;

import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.BeansWrapperConfiguration;
import freemarker.ext.beans._BeansAPI;
import freemarker.template.Version;

@SuppressWarnings("rawtypes")
public class MyBeansWrapperBuilder extends BeansWrapperConfiguration {

	private final static WeakHashMap/*
									 * <ClassLoader, Map<PropertyAssignments,
									 * WeakReference<MyBeansWrapper>>
									 */
	INSTANCE_CACHE = new WeakHashMap();
	private final static ReferenceQueue INSTANCE_CACHE_REF_QUEUE = new ReferenceQueue();

	private static class BeansWrapperFactory implements _BeansAPI._BeansWrapperSubclassFactory {

		private static final BeansWrapperFactory INSTANCE = new BeansWrapperFactory();

		@Override
		public BeansWrapper create(BeansWrapperConfiguration bwConf) {
			return new MyBeansWrapper(bwConf, true);
		}

	}

	public MyBeansWrapperBuilder(Version incompatibleImprovements) {
		super(incompatibleImprovements);
	}

	/** For unit testing only */
	static void clearInstanceCache() {
		synchronized (INSTANCE_CACHE) {
			INSTANCE_CACHE.clear();
		}
	}

	/** For unit testing only */
	static Map getInstanceCache() {
		return INSTANCE_CACHE;
	}

	public BeansWrapper build() {
		return _BeansAPI.getBeansWrapperSubclassSingleton(this, INSTANCE_CACHE, INSTANCE_CACHE_REF_QUEUE,
				BeansWrapperFactory.INSTANCE);
	}

}
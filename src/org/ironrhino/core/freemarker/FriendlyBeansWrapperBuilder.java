package org.ironrhino.core.freemarker;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.BeansWrapperConfiguration;
import freemarker.ext.beans._BeansAPI;
import freemarker.template.Version;

public class FriendlyBeansWrapperBuilder extends BeansWrapperConfiguration {

	private final static Map<ClassLoader, Map<BeansWrapperConfiguration, WeakReference<BeansWrapper>>> INSTANCE_CACHE = new WeakHashMap<ClassLoader, Map<BeansWrapperConfiguration, WeakReference<BeansWrapper>>>();
	private final static ReferenceQueue<BeansWrapper> INSTANCE_CACHE_REF_QUEUE = new ReferenceQueue<BeansWrapper>();

	private static class BeansWrapperFactory
			implements _BeansAPI._BeansWrapperSubclassFactory<BeansWrapper, BeansWrapperConfiguration> {

		private static final BeansWrapperFactory INSTANCE = new BeansWrapperFactory();

		@Override
		public BeansWrapper create(BeansWrapperConfiguration bwConf) {
			return new FriendlyBeansWrapper(bwConf, true);
		}

	}

	public FriendlyBeansWrapperBuilder(Version incompatibleImprovements) {
		super(incompatibleImprovements);
	}

	/** For unit testing only */
	static void clearInstanceCache() {
		synchronized (INSTANCE_CACHE) {
			INSTANCE_CACHE.clear();
		}
	}

	/** For unit testing only */
	static Map<ClassLoader, Map<BeansWrapperConfiguration, WeakReference<BeansWrapper>>> getInstanceCache() {
		return INSTANCE_CACHE;
	}

	public BeansWrapper build() {
		return _BeansAPI.getBeansWrapperSubclassSingleton(this, INSTANCE_CACHE, INSTANCE_CACHE_REF_QUEUE,
				BeansWrapperFactory.INSTANCE);
	}

}
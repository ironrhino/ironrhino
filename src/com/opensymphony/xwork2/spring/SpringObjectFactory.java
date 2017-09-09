/*
 * Copyright 2002-2006,2009 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.opensymphony.xwork2.spring;

import com.opensymphony.xwork2.ObjectFactory;
import com.opensymphony.xwork2.util.logging.Logger;
import com.opensymphony.xwork2.util.logging.LoggerFactory;

import org.ironrhino.core.util.ReflectionUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Map;

/**
 * Simple implementation of the ObjectFactory that makes use of Spring's application context if one has been configured,
 * before falling back on the default mechanism of instantiating a new class using the class name. <p/> In order to use
 * this class in your application, you will need to instantiate a copy of this class and set it as XWork's ObjectFactory
 * before the xwork.xml file is parsed. In a servlet environment, this could be done using a ServletContextListener.
 *
 * @author Simon Stewart (sms@lateral.net)
 */
@SuppressWarnings({"serial", "rawtypes"})
public class SpringObjectFactory extends ObjectFactory implements ApplicationContextAware {
    private static final Logger LOG = LoggerFactory.getLogger(SpringObjectFactory.class);

    protected ApplicationContext appContext;
    protected AutowireCapableBeanFactory autoWiringFactory;
    protected int autowireStrategy = AutowireCapableBeanFactory.AUTOWIRE_BY_NAME;

    /**
     * Set the Spring ApplicationContext that should be used to look beans up with.
     *
     * @param appContext The Spring ApplicationContext that should be used to look beans up with.
     */
    public void setApplicationContext(ApplicationContext appContext)
            throws BeansException {
        this.appContext = appContext;
        autoWiringFactory = findAutoWiringBeanFactory(this.appContext);
    }

    /**
     * Sets the autowiring strategy
     *
     * @param autowireStrategy
     */
	public void setAutowireStrategy(int autowireStrategy) {
        switch (autowireStrategy) {
            case AutowireCapableBeanFactory.AUTOWIRE_BY_NAME:
                if (LOG.isInfoEnabled()) {
                    LOG.info("Setting autowire strategy to name");
                }
                this.autowireStrategy = autowireStrategy;
                break;
            case AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE:
                if (LOG.isInfoEnabled()) {
                    LOG.info("Setting autowire strategy to type");
                }
                this.autowireStrategy = autowireStrategy;
                break;
            case AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR:
                if (LOG.isInfoEnabled()) {
                    LOG.info("Setting autowire strategy to constructor");
                }
                this.autowireStrategy = autowireStrategy;
                break;
            case AutowireCapableBeanFactory.AUTOWIRE_NO:
                if (LOG.isInfoEnabled()) {
                    LOG.info("Setting autowire strategy to none");
                }
                this.autowireStrategy = autowireStrategy;
                break;
            default:
                throw new IllegalStateException("Invalid autowire type set");
        }
    }

    public int getAutowireStrategy() {
        return autowireStrategy;
    }


    /**
     * If the given context is assignable to AutowireCapbleBeanFactory or contains a parent or a factory that is, then
     * set the autoWiringFactory appropriately.
     *
     * @param context
     */
    protected AutowireCapableBeanFactory findAutoWiringBeanFactory(ApplicationContext context) {
        if (context instanceof AutowireCapableBeanFactory) {
            // Check the context
            return (AutowireCapableBeanFactory) context;
        } else if (context instanceof ConfigurableApplicationContext) {
            // Try and grab the beanFactory
            return ((ConfigurableApplicationContext) context).getBeanFactory();
        } else if (context.getParent() != null) {
            // And if all else fails, try again with the parent context
            return findAutoWiringBeanFactory(context.getParent());
        }
        return null;
    }

    /**
     * Looks up beans using Spring's application context before falling back to the method defined in the {@link
     * ObjectFactory}.
     *
     * @param beanName     The name of the bean to look up in the application context
     * @param extraContext
     * @return A bean from Spring or the result of calling the overridden
     *         method.
     * @throws Exception
     */
    @Override
    public Object buildBean(String beanName, Map<String, Object> extraContext, boolean injectInternal) throws Exception {
        Object o;
        if (appContext.containsBean(beanName)) {
            o = appContext.getBean(beanName);
            if (injectInternal) {
                injectInternalBeans(o);
            }
        } else {
            Class beanClazz = getClassInstance(beanName);
            o = buildBean(beanClazz, extraContext);
        }
        return o;
    }

    /**
     * @param clazz
     * @param extraContext
     * @throws Exception
     */
    @Override
    public Object buildBean(Class clazz, Map<String, Object> extraContext) throws Exception {
    	if(clazz.isArray())
    		return null;
        try {
            Object bean = autoWiringFactory.createBean(clazz, autowireStrategy, false);
            injectInternalBeans(bean);
            return bean;
        } catch (UnsatisfiedDependencyException e) {
            if (LOG.isErrorEnabled())
                LOG.error("Error building bean", e);
            // Fall back
            return autoWireBean(super.buildBean(clazz, extraContext), autoWiringFactory);
        }
    }

    public Object autoWireBean(Object bean) {
        return autoWireBean(bean, autoWiringFactory);
    }

    /**
     * @param bean
     * @param autoWiringFactory
     */
    public Object autoWireBean(Object bean, AutowireCapableBeanFactory autoWiringFactory) {
        if (autoWiringFactory != null) {
            autoWiringFactory.autowireBeanProperties(bean,
                    autowireStrategy, false);
        }
        injectApplicationContext(bean);

        injectInternalBeans(bean);

        return bean;
    }

    @Override
    protected Object injectInternalBeans(Object obj){
    		super.injectInternalBeans(ReflectionUtils.getTargetObject(obj));
    		return obj;
    }

    private void injectApplicationContext(Object bean) {
        if (bean instanceof ApplicationContextAware) {
            ((ApplicationContextAware) bean).setApplicationContext(appContext);
        }
    }

    public Class getClassInstance(String className) throws ClassNotFoundException {
        Class clazz = null;
        if (appContext.containsBean(className)) {
            clazz = appContext.getBean(className).getClass();
        } else {
            clazz = super.getClassInstance(className);
        }
        return clazz;
    }

    /**
     * This method sets the ObjectFactory used by XWork to this object. It's best used as the "init-method" of a Spring
     * bean definition in order to hook Spring and XWork together properly (as an alternative to the
     * org.apache.struts2.spring.lifecycle.SpringObjectFactoryListener)
     * @deprecated Since 2.1 as it isn't necessary
     */
    @Deprecated public void initObjectFactory() {
        // not necessary anymore
    }

    /**
     * Allows for ObjectFactory implementations that support
     * Actions without no-arg constructors.
     *
     * @return false
     */
    @Override
    public boolean isNoArgConstructorRequired() {
        return false;
    }

}

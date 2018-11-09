package org.ironrhino.core.struts;

import java.beans.PropertyDescriptor;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.persistence.EntityManagerFactory;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.apache.struts2.views.freemarker.FreemarkerManager;
import org.hibernate.SessionFactory;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.Subselect;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.query.Query;
import org.ironrhino.core.hibernate.CriteriaState;
import org.ironrhino.core.hibernate.CriterionUtils;
import org.ironrhino.core.metadata.AppendOnly;
import org.ironrhino.core.metadata.Authorize;
import org.ironrhino.core.metadata.CaseInsensitive;
import org.ironrhino.core.metadata.DoubleChecker;
import org.ironrhino.core.metadata.JsonConfig;
import org.ironrhino.core.metadata.Owner;
import org.ironrhino.core.metadata.Readonly;
import org.ironrhino.core.metadata.Richtable;
import org.ironrhino.core.model.Attachmentable;
import org.ironrhino.core.model.BaseTreeableEntity;
import org.ironrhino.core.model.Enableable;
import org.ironrhino.core.model.Ordered;
import org.ironrhino.core.model.Persistable;
import org.ironrhino.core.model.ResultPage;
import org.ironrhino.core.model.Tuple;
import org.ironrhino.core.search.SearchCriteria;
import org.ironrhino.core.search.SearchService;
import org.ironrhino.core.search.elasticsearch.annotations.Searchable;
import org.ironrhino.core.security.role.UserRole;
import org.ironrhino.core.service.BaseManager;
import org.ironrhino.core.service.BaseTreeControl;
import org.ironrhino.core.struts.AnnotationShadows.HiddenImpl;
import org.ironrhino.core.struts.AnnotationShadows.ReadonlyImpl;
import org.ironrhino.core.struts.AnnotationShadows.RichtableImpl;
import org.ironrhino.core.struts.AnnotationShadows.UiConfigImpl;
import org.ironrhino.core.util.AnnotationUtils;
import org.ironrhino.core.util.ApplicationContextUtils;
import org.ironrhino.core.util.AuthzUtils;
import org.ironrhino.core.util.BeanUtils;
import org.ironrhino.core.util.CompareUtils;
import org.ironrhino.core.util.DateUtils;
import org.ironrhino.core.util.JsonUtils;
import org.ironrhino.core.util.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.InvalidPropertyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.ConversionService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.ClassUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionProxy;
import com.opensymphony.xwork2.inject.Inject;
import com.opensymphony.xwork2.interceptor.annotations.InputConfig;
import com.opensymphony.xwork2.util.ValueStack;
import com.opensymphony.xwork2.util.ValueStackFactory;
import com.opensymphony.xwork2.util.reflection.ReflectionContextState;

import freemarker.template.Template;
import lombok.Getter;
import lombok.Setter;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class EntityAction<EN extends Persistable<?>> extends BaseAction {

	private static final long serialVersionUID = -8442983706126047413L;

	protected static Logger logger = LoggerFactory.getLogger(EntityAction.class);

	private ReadonlyImpl _readonly;

	private RichtableImpl _richtableConfig;

	private Map<String, UiConfigImpl> _uiConfigs;

	private volatile EN _entity;

	private String _entityName;

	private Map<String, NaturalId> _naturalIds;

	@Getter
	@Setter
	protected ResultPage<EN> resultPage;

	@Getter
	@Setter
	protected Long tree;

	@Getter
	@Setter
	protected Long parent;

	@Getter
	protected BaseTreeableEntity parentEntity;

	@Autowired
	protected SessionFactory sessionFactory;

	@Autowired(required = false)
	protected SearchService<EN> searchService;

	@Autowired(required = false)
	protected ConversionService conversionService;

	@Value("${csv.defaultEncoding:GBK}")
	private String csvDefaultEncoding = "GBK";

	@Value("${csv.maxRows:0}")
	private int csvMaxRows;

	public int getCsvMaxRows() {
		return csvMaxRows > 0 ? csvMaxRows : 1000 * ResultPage.DEFAULT_PAGE_SIZE;
	}

	public boolean isSearchable() {
		if (getEntityClass().getAnnotation(Searchable.class) != null)
			return true;
		RichtableImpl rc = getRichtableConfig();
		boolean searchable = rc.isSearchable();
		if (searchable)
			return true;
		else
			for (Map.Entry<String, UiConfigImpl> entry : getUiConfigs().entrySet())
				if (entry.getValue().isSearchable())
					return true;
		return false;
	}

	public boolean isFulltextSearchable() {
		return (getEntityClass().getAnnotation(Searchable.class) != null) && searchService != null;
	}

	public boolean isDoubleCheck() {
		return !AnnotationUtils.getAnnotatedPropertyNameAndAnnotations(getEntityClass(), DoubleChecker.class).isEmpty();
	}

	public boolean isEnableable() {
		return Enableable.class.isAssignableFrom(getEntityClass());
	}

	public boolean isTreeable() {
		return BaseTreeableEntity.class.isAssignableFrom(getEntityClass());
	}

	public boolean isAttachmentable() {
		return Attachmentable.class.isAssignableFrom(getEntityClass())
				&& ClassUtils.isPresent("org.ironrhino.common.action.UploadAction", getClass().getClassLoader());
	}

	public boolean isIdAssigned() {
		return EntityClassHelper.isIdAssigned(getEntityClass());
	}

	public EN getEntity() {
		return _entity;
	}

	public RichtableImpl getRichtableConfig() {
		if (_richtableConfig == null) {
			Richtable rc = getClass().getAnnotation(Richtable.class);
			if (rc == null)
				rc = getEntityClass().getAnnotation(Richtable.class);
			_richtableConfig = new RichtableImpl(rc);
		}
		return _richtableConfig;
	}

	public ReadonlyImpl getReadonly() {
		if (_readonly == null) {
			if (getEntityClass().isAnnotationPresent(Immutable.class)) {
				_readonly = new ReadonlyImpl();
				_readonly.setValue(true);
				return _readonly;
			}
			if (getEntityClass().isAnnotationPresent(Subselect.class)) {
				_readonly = new ReadonlyImpl();
				_readonly.setValue(true);
				return _readonly;
			}
			Richtable rconfig = getClass().getAnnotation(Richtable.class);
			if (rconfig == null)
				rconfig = getEntityClass().getAnnotation(Richtable.class);
			Readonly rc = null;
			if (rconfig != null)
				rc = rconfig.readonly();
			if (isAppendOnly() && rc == null) {
				_readonly = new ReadonlyImpl();
				_readonly.setValue(false);
				_readonly.setExpression("!entity.new");
				_readonly.setDeletable(false);
				return _readonly;
			}
			Tuple<Owner, Class<?>> ownerProperty = getOwnerProperty();
			if (ownerProperty != null) {
				Owner owner = ownerProperty.getKey();
				if (!owner.isolate() && owner.readonlyForOther() && !(StringUtils.isNotBlank(owner.supervisorRole())
						&& AuthzUtils.authorize(null, owner.supervisorRole(), null))) {
					_readonly = new ReadonlyImpl();
					_readonly.setValue(false);
					StringBuilder sb = new StringBuilder("!entity.");
					sb.append(ownerProperty.getKey().propertyName()).append("?? || entity.")
							.append(ownerProperty.getKey().propertyName()).append("!=authentication('principal')");
					if (ownerProperty.getValue() == String.class)
						sb.append(".username");
					if (rc != null && !rc.value() && StringUtils.isNotBlank(rc.expression()))
						sb.append(" || ").append(rc.expression());
					String expression = sb.toString();
					_readonly.setExpression(expression);
					_readonly.setDeletable(false);
				}
			}
			if (_readonly == null)
				_readonly = new ReadonlyImpl(rc);
		}
		return _readonly;
	}

	public boolean isAppendOnly() {
		return getEntityClass().getAnnotation(AppendOnly.class) != null;
	}

	public String getEntityName() {
		if (_entityName == null)
			_entityName = ActionContext.getContext().getActionInvocation().getProxy().getActionName();
		return _entityName;
	}

	public Map<String, NaturalId> getNaturalIds() {
		if (_naturalIds == null)
			_naturalIds = AnnotationUtils.getAnnotatedPropertyNameAndAnnotations(getEntityClass(), NaturalId.class);
		return _naturalIds;
	}

	public String getVersionPropertyName() {
		ClassMetadata cm = (ClassMetadata) ((MetamodelImplementor) ((EntityManagerFactory) sessionFactory)
				.getMetamodel()).entityPersister(getEntityClass().getName());
		return cm.isVersioned() ? cm.getPropertyNames()[cm.getVersionProperty()] : null;
	}

	public boolean isNaturalIdMutable() {
		return getNaturalIds().size() > 0 && getNaturalIds().values().iterator().next().mutable();
	}

	public Map<String, UiConfigImpl> getUiConfigs() {
		if (_uiConfigs == null)
			_uiConfigs = EntityClassHelper.getUiConfigs(getEntityClass());
		return _uiConfigs;
	}

	protected <T extends Persistable<?>> BaseManager<T> getEntityManager(Class<T> entityClass) {
		return ApplicationContextUtils.getEntityManager(entityClass);
	}

	protected void setEntity(EN entity) {
		this._entity = entity;
	}

	protected void tryFindEntity() {
		BaseManager<EN> entityManager = getEntityManager(getEntityClass());
		try {
			BeanWrapperImpl bw = new BeanWrapperImpl(getEntityClass().getConstructor().newInstance());
			bw.setConversionService(conversionService);
			Set<String> naturalIds = getNaturalIds().keySet();
			if (StringUtils.isNotBlank(getUid())) {
				String uid = getUid();
				if (uid.indexOf('.') > 0) {
					bw.setPropertyValue("id", uid.substring(0, uid.indexOf('.')));
					_entity = entityManager.get((Serializable) bw.getPropertyValue("id"));
					if (_entity == null && naturalIds.size() == 1) {
						String naturalIdName = naturalIds.iterator().next();
						bw.setPropertyValue(naturalIdName, uid.substring(0, uid.indexOf('.')));
						_entity = entityManager.findByNaturalId((Serializable) bw.getPropertyValue(naturalIdName));
					}
				}
				if (_entity == null) {
					bw.setPropertyValue("id", uid);
					_entity = entityManager.get((Serializable) bw.getPropertyValue("id"));
					if (_entity == null && naturalIds.size() == 1) {
						String naturalIdName = naturalIds.iterator().next();
						bw.setPropertyValue(naturalIdName, uid);
						_entity = entityManager.findByNaturalId((Serializable) bw.getPropertyValue(naturalIdName));
					}
				}

			}
			if (_entity == null && naturalIds.size() > 0) {
				Serializable[] paramters = new Serializable[naturalIds.size() * 2];
				int i = 0;
				boolean satisfied = true;
				for (String naturalId : naturalIds) {
					paramters[i] = naturalId;
					i++;
					bw.setPropertyValue(naturalId, ServletActionContext.getRequest().getParameter(naturalId));
					Serializable value = (Serializable) bw.getPropertyValue(naturalId);
					if (value != null)
						paramters[i] = value;
					else {
						satisfied = false;
						break;
					}
					i++;
				}
				if (satisfied)
					_entity = entityManager.findByNaturalId(paramters);
			}

		} catch (ConversionNotSupportedException e) {
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	@Override
	public String execute() throws Exception {
		BeanWrapperImpl bw = new BeanWrapperImpl(getEntityClass().getConstructor().newInstance());
		bw.setConversionService(conversionService);
		Richtable richtableConfig = getClass().getAnnotation(Richtable.class);
		if (richtableConfig == null)
			richtableConfig = getEntityClass().getAnnotation(Richtable.class);
		final BaseManager entityManager = getEntityManager(getEntityClass());
		Tuple<Owner, Class<?>> ownerProperty = getOwnerProperty();
		if (ownerProperty != null && ownerProperty.getKey().isolate() || !isFulltextSearchable()
				|| StringUtils.isBlank(keyword)) {
			boolean resetPageSize;
			if (resultPage == null) {
				resultPage = new ResultPage();
				resetPageSize = richtableConfig != null;
			} else {
				resetPageSize = richtableConfig != null && richtableConfig.fixPageSize();
			}
			if (resetPageSize && resultPage.getPageSize() != richtableConfig.defaultPageSize())
				resultPage.setPageSize(richtableConfig.defaultPageSize());
			if (richtableConfig != null && resultPage.getPaginating() == null)
				resultPage.setPaginating(richtableConfig.paginating());
			resultPage
					.setCriteria(doPrepareCriteria(entityManager, bw, richtableConfig, isSearchable(), ownerProperty));
			resultPage = entityManager.findByResultPage(resultPage);
		} else {
			Set<String> searchableProperties = new HashSet<>();
			for (Map.Entry<String, UiConfigImpl> entry : getUiConfigs().entrySet()) {
				if (entry.getValue().isSearchable())
					searchableProperties.add(entry.getKey());
			}
			String query = keyword.trim();
			SearchCriteria criteria = new SearchCriteria();
			criteria.setQuery(query);
			String indexType = getEntityClass().getAnnotation(Searchable.class).type();
			if (StringUtils.isBlank(indexType))
				indexType = getEntityName();
			criteria.setTypes(new String[] { indexType });
			if (richtableConfig != null && StringUtils.isNotBlank(richtableConfig.order())) {
				String[] ar = richtableConfig.order().split("\\s*,\\s*");
				for (String s : ar) {
					String[] arr = s.split("\\s+", 2);
					String propertyName = arr[0];
					if (searchableProperties.contains(propertyName)) {
						if (arr.length == 2 && arr[1].equalsIgnoreCase("asc"))
							criteria.addSort(propertyName, false);
						else if (arr.length == 2 && arr[1].equalsIgnoreCase("desc"))
							criteria.addSort(propertyName, true);
						else
							criteria.addSort(propertyName, false);
					}
				}
			} else if (Ordered.class.isAssignableFrom(getEntityClass())
					&& searchableProperties.contains("displayOrder"))
				criteria.addSort("displayOrder", false);
			boolean resetPageSize;
			if (resultPage == null) {
				resultPage = new ResultPage();
				resetPageSize = richtableConfig != null;
			} else {
				resetPageSize = richtableConfig != null && richtableConfig.fixPageSize();
			}
			if (resetPageSize && resultPage.getPageSize() != richtableConfig.defaultPageSize())
				resultPage.setPageSize(richtableConfig.defaultPageSize());
			if (richtableConfig != null)
				resultPage.setPaginating(richtableConfig.paginating());
			prepare(criteria);
			resultPage.setCriteria(criteria);
			resultPage = searchService.search(resultPage, source -> (EN) entityManager.get(source.getId()));
		}
		return LIST;
	}

	protected DetachedCriteria detachedCriteria() throws Exception {
		BaseManager entityManager = getEntityManager(getEntityClass());
		BeanWrapperImpl bw = new BeanWrapperImpl(getEntityClass().getConstructor().newInstance());
		Richtable richtableConfig = getClass().getAnnotation(Richtable.class);
		if (richtableConfig == null)
			richtableConfig = getEntityClass().getAnnotation(Richtable.class);
		boolean searchable = isSearchable();
		Tuple<Owner, Class<?>> ownerProperty = getOwnerProperty();
		return doPrepareCriteria(entityManager, bw, richtableConfig, searchable, ownerProperty);
	}

	private DetachedCriteria doPrepareCriteria(BaseManager entityManager, BeanWrapperImpl bw, Richtable richtableConfig,
			boolean searchable, Tuple<Owner, Class<?>> ownerProperty) {
		DetachedCriteria dc = entityManager.detachedCriteria();
		if (ownerProperty != null) {
			Owner owner = ownerProperty.getKey();
			if (!(StringUtils.isNotBlank(owner.supervisorRole())
					&& AuthzUtils.authorize(null, owner.supervisorRole(), null)) && owner.isolate()) {
				if (ownerProperty.getValue() == String.class) {
					dc.add(Restrictions.eq(owner.propertyName(), AuthzUtils.getUsername()));
				} else {
					UserDetails ud = AuthzUtils.getUserDetails((Class<? extends UserDetails>) ownerProperty.getValue());
					dc.add(Restrictions.eq(owner.propertyName(), ud));
				}
			}
		}
		CriteriaState criteriaState = CriterionUtils.filter(dc, getEntityClass(), getUiConfigs());
		prepare(dc, criteriaState);
		if (isTreeable()) {
			if (parent == null || parent < 1) {
				if (tree != null && tree > 0)
					dc.add(Restrictions.eq("id", tree));
				else
					dc.add(Restrictions.isNull("parent"));
			} else {
				parentEntity = (BaseTreeableEntity) entityManager.get(parent);
				String alias = criteriaState.getAliases().get("parent");
				if (alias == null) {
					alias = "parent_";
					while (criteriaState.getAliases().containsValue(alias))
						alias += "_";
					dc.createAlias("parent", alias);
					criteriaState.getAliases().put("parent", alias);
				}
				dc.add(Restrictions.eq(alias + ".id", parent));
			}
		}
		if (searchable && StringUtils.isNotBlank(keyword)) {
			if (StringUtils.isNumeric(keyword)) {
				try {
					bw.setPropertyValue("id", keyword);
					Serializable idvalue = (Serializable) bw.getPropertyValue("id");
					if (idvalue instanceof Number) {
						dc.add(Restrictions.idEq(idvalue));
						return dc;
					}
				} catch (Exception e) {

				}
			}
			if (StringUtils.isAlphanumeric(keyword) && (keyword.length() == 32 || keyword.length() == 22)) {
				dc.add(Restrictions.idEq(keyword));
				return dc;
			}
			Map<String, MatchMode> propertyNamesInLike = new LinkedHashMap<>();
			for (Map.Entry<String, UiConfigImpl> entry : getUiConfigs().entrySet()) {
				if (entry.getValue().isSearchable() && !entry.getValue().isExcludedFromLike()) {
					if (String.class.equals(entry.getValue().getPropertyType())) {
						propertyNamesInLike.put(entry.getKey(), entry.getValue().isExactMatch() ? MatchMode.EXACT
								: entry.getValue().getQueryMatchMode());
					} else if (String.class.equals(entry.getValue().getElementType())) {
						propertyNamesInLike.put(entry.getKey(), null);
						// null marks as tag
					} else if (Persistable.class.isAssignableFrom(entry.getValue().getPropertyType())) {
						Set<String> nestSearchableProperties = entry.getValue().getNestSearchableProperties();
						if (nestSearchableProperties != null && nestSearchableProperties.size() > 0) {
							String alias = criteriaState.getAliases().get(entry.getKey());
							if (alias == null) {
								alias = entry.getKey() + "_";
								while (criteriaState.getAliases().containsValue(alias))
									alias += "_";
								dc.createAlias(entry.getKey(), alias);
								criteriaState.getAliases().put(entry.getKey(), alias);
							}
							for (String s : nestSearchableProperties) {
								UiConfigImpl nestUci = EntityClassHelper
										.getUiConfigs(entry.getValue().getPropertyType()).get(s);
								if (nestUci == null)
									continue;
								propertyNamesInLike.put(new StringBuilder(alias).append(".").append(s).toString(),
										nestUci.isExactMatch() ? MatchMode.EXACT : MatchMode.ANYWHERE);
							}
						}
					} else if (entry.getValue().getEmbeddedUiConfigs() != null) {
						Set<String> nestSearchableProperties = entry.getValue().getNestSearchableProperties();
						if (nestSearchableProperties != null && nestSearchableProperties.size() > 0) {
							for (String s : nestSearchableProperties) {
								UiConfigImpl nestUci = entry.getValue().getEmbeddedUiConfigs().get(s);
								if (nestUci == null)
									continue;
								propertyNamesInLike.put(
										new StringBuilder(entry.getKey()).append(".").append(s).toString(),
										nestUci.isExactMatch() ? MatchMode.EXACT : MatchMode.ANYWHERE);
							}
						}
					}

				}
			}
			if (propertyNamesInLike.size() > 0)
				dc.add(CriterionUtils.like(keyword, propertyNamesInLike));
			else
				dc.add(Restrictions.like("id", keyword, MatchMode.EXACT));
		}
		if (criteriaState.getOrderings().isEmpty()) {
			if (richtableConfig != null && StringUtils.isNotBlank(richtableConfig.order())) {
				String[] ar = richtableConfig.order().split("\\s*,\\s*");
				for (String s : ar) {
					String[] arr = s.trim().split("\\s+", 2);
					String propertyName = arr[0];
					if (propertyName.indexOf(".") > 0) {
						String p1 = propertyName.substring(0, propertyName.indexOf("."));
						String p2 = propertyName.substring(propertyName.indexOf(".") + 1);
						Class type = bw.getPropertyType(p1);
						if (Persistable.class.isAssignableFrom(type)) {
							String alias = criteriaState.getAliases().get(p1);
							if (alias == null) {
								alias = p1 + "_";
								while (criteriaState.getAliases().containsValue(alias))
									alias += "_";
								dc.createAlias(p1, alias);
								criteriaState.getAliases().put(p1, alias);
							}
							propertyName = alias + '.' + p2;
						}
					}
					if (arr.length == 2 && arr[1].equalsIgnoreCase("asc"))
						dc.addOrder(Order.asc(propertyName));
					else if (arr.length == 2 && arr[1].equalsIgnoreCase("desc"))
						dc.addOrder(Order.desc(propertyName));
					else
						dc.addOrder(Order.asc(propertyName));
				}
			} else if (Ordered.class.isAssignableFrom(getEntityClass()))
				dc.addOrder(Order.asc("displayOrder"));
		}
		return dc;

	}

	protected void prepare(DetachedCriteria dc, CriteriaState criteriaState) {

	}

	protected void prepare(SearchCriteria esc) {

	}

	@Override
	public String input() throws Exception {
		if (getReadonly().isValue()) {
			addActionError(getText("access.denied"));
			return ACCESSDENIED;
		}
		return doInput();
	}

	protected String doInput() throws Exception {
		tryFindEntity();
		if (_entity != null && !_entity.isNew()) {
			Tuple<Owner, Class<?>> ownerProperty = getOwnerProperty();
			if (ownerProperty != null) {
				Owner owner = ownerProperty.getKey();
				if (!(StringUtils.isNotBlank(owner.supervisorRole())
						&& AuthzUtils.authorize(null, owner.supervisorRole(), null))
						&& (owner.isolate() || owner.readonlyForOther())) {
					if (!hasOwnership(_entity)) {
						addActionError(getText("access.denied"));
						return ACCESSDENIED;
					}
				}
			}
			if (checkEntityReadonly(getReadonly().getExpression(), _entity)) {
				addActionError(getText("access.denied"));
				return ACCESSDENIED;
			}
		}
		BeanWrapperImpl bw;
		if (_entity == null) {
			_entity = getEntityClass().getConstructor().newInstance();
			bw = new BeanWrapperImpl(_entity);
			String versionPropertyName = getVersionPropertyName();
			if (versionPropertyName != null) {
				Object version = bw.getPropertyValue(versionPropertyName);
				if (version instanceof Number && ((Number) version).intValue() == 0) {
					bw.setPropertyValue(versionPropertyName, -1);
				}
			}
		} else {
			bw = new BeanWrapperImpl(_entity);
		}
		bw.setConversionService(conversionService);
		if (_entity != null && _entity.isNew()) {
			Set<String> naturalIds = getNaturalIds().keySet();
			if (getUid() != null && naturalIds.size() == 1) {
				bw.setPropertyValue(naturalIds.iterator().next(), getUid());
			}
		}
		Set<String> editablePropertyNames = getUiConfigs().keySet();
		for (String parameterName : ServletActionContext.getRequest().getParameterMap().keySet()) {
			String propertyName = parameterName;
			if (propertyName.startsWith(getEntityName() + "."))
				propertyName = propertyName.substring(propertyName.indexOf('.') + 1);
			if (propertyName.indexOf('.') > 0) {
				String subPropertyName = propertyName.substring(propertyName.indexOf('.') + 1);
				propertyName = propertyName.substring(0, propertyName.indexOf('.'));
				if (editablePropertyNames.contains(propertyName)) {
					Class type = bw.getPropertyType(propertyName);
					if (Persistable.class.isAssignableFrom(type)) {
						String parameterValue = ServletActionContext.getRequest().getParameter(parameterName);
						BaseManager em = getEntityManager(type);
						Persistable value = null;
						BeanWrapperImpl bw2 = new BeanWrapperImpl(type);
						bw2.setConversionService(conversionService);
						if (subPropertyName.equals("id")) {
							bw2.setPropertyValue("id", parameterValue);
							value = em.get((Serializable) bw2.getPropertyValue("id"));
						} else {
							try {
								bw2.setPropertyValue(subPropertyName, parameterValue);
								value = em.findOne(subPropertyName,
										(Serializable) bw2.getPropertyValue(subPropertyName));
							} catch (InvalidPropertyException e) {
								continue;
							}
						}
						bw.setPropertyValue(propertyName, value);
					}
				}
			} else if (editablePropertyNames.contains(propertyName)) {
				String parameterValue = ServletActionContext.getRequest().getParameter(parameterName);
				Class type = bw.getPropertyType(propertyName);
				Object value = null;
				if (Persistable.class.isAssignableFrom(type)) {
					BaseManager em = getEntityManager(type);
					try {
						BeanWrapperImpl bwt = new BeanWrapperImpl(type.getConstructor().newInstance());
						bwt.setPropertyValue("id", parameterValue);
						value = em.get((Serializable) bwt.getPropertyValue("id"));
						if (value == null)
							value = em.findOne(parameterValue);
					} catch (Exception e) {

					}
					bw.setPropertyValue(propertyName, value);
				} else {
					bw.setPropertyValue(propertyName, parameterValue);
				}
			}

		}
		putEntityToValueStack(_entity);
		return INPUT;
	}

	@Override
	public String save() throws Exception {
		if (getReadonly().isValue()) {
			addActionError(getText("access.denied"));
			return ACCESSDENIED;
		}
		return doSave();
	}

	protected String doSave() throws Exception {
		if (!makeEntityValid())
			return INPUT;
		BeanWrapperImpl bwp = new BeanWrapperImpl(_entity);
		bwp.setConversionService(conversionService);
		Tuple<Owner, Class<?>> ownerProperty = getOwnerProperty();
		boolean isnew = _entity.isNew();
		boolean idAssigned = isIdAssigned();
		if (idAssigned)
			isnew = "true".equals(ServletActionContext.getRequest().getParameter("_isnew"));
		if (!isnew) {
			if (ownerProperty != null) {
				Owner owner = ownerProperty.getKey();
				if (!(StringUtils.isNotBlank(owner.supervisorRole())
						&& AuthzUtils.authorize(null, owner.supervisorRole(), null))
						&& (ownerProperty.getKey().isolate() || ownerProperty.getKey().readonlyForOther())) {
					if (!hasOwnership(_entity)) {
						addActionError(getText("access.denied"));
						return ACCESSDENIED;
					}
				}
			}
			if (checkEntityReadonly(getReadonly().getExpression(), _entity)) {
				addActionError(getText("access.denied"));
				return ACCESSDENIED;
			}
		} else {
			if (ownerProperty != null) {
				if (ownerProperty.getValue() == String.class) {
					String username = AuthzUtils.getUsername();
					if (username == null) {
						addActionError(getText("access.denied"));
						return ACCESSDENIED;
					}
					bwp.setPropertyValue(ownerProperty.getKey().propertyName(), username);
				} else {
					UserDetails ud = AuthzUtils.getUserDetails((Class<? extends UserDetails>) ownerProperty.getValue());
					if (ud == null) {
						addActionError(getText("access.denied"));
						return ACCESSDENIED;
					}
					bwp.setPropertyValue(ownerProperty.getKey().propertyName(), ud);
				}
			}
			if (isTreeable())
				((BaseTreeableEntity) _entity)
						.setParent((BaseTreeableEntity) getEntityManager(getEntityClass()).get(parent));
		}

		for (Map.Entry<String, UiConfigImpl> entry : getUiConfigs().entrySet()) {
			String name = entry.getKey();
			UiConfigImpl uiconfig = entry.getValue();
			Object value = bwp.getPropertyValue(name);
			String expression = uiconfig.getHiddenInInput().getExpression();
			if (uiconfig.isRequired()
					&& !(uiconfig.getHiddenInInput().isValue()
							|| StringUtils.isNotBlank(expression) && evalBoolean(expression, _entity, value))
					&& (value == null || value instanceof String && StringUtils.isBlank(value.toString()))) {
				addFieldError(getEntityName() + '.' + name, getText("validation.required"));
				return INPUT;
			}
		}
		BaseManager<EN> entityManager = getEntityManager(getEntityClass());
		String versionPropertyName = getVersionPropertyName();
		Object previousVersion = null;
		if (versionPropertyName != null) {
			previousVersion = bwp.getPropertyValue(versionPropertyName);
		}
		beforeSave(_entity);
		entityManager.save(_entity);
		afterSave(_entity);
		if (isnew && !idAssigned)
			ServletActionContext.getResponse().addHeader("X-Postback", getEntityName() + ".id=" + _entity.getId());
		if (versionPropertyName != null) {
			Object currentVersion = bwp.getPropertyValue(versionPropertyName);
			if (!Objects.equals(previousVersion, currentVersion))
				ServletActionContext.getResponse().addHeader("X-Postback",
						getEntityName() + "." + getVersionPropertyName() + "=" + currentVersion);
		}
		notify("save.success");
		return SUCCESS;
	}

	public String checkavailable() {
		makeEntityValid();
		return JSON;
	}

	protected boolean makeEntityValid() {
		HttpServletRequest request = ServletActionContext.getRequest();
		String targetField = request.getHeader("X-Target-Field");
		boolean idAssigned = isIdAssigned();
		boolean fromList = "cell".equalsIgnoreCase(request.getHeader("X-Edit"));
		Map<String, UiConfigImpl> uiConfigs = getUiConfigs();
		BaseManager<EN> entityManager = getEntityManager(getEntityClass());
		_entity = constructEntity();
		BeanWrapperImpl bw = new BeanWrapperImpl(_entity);
		bw.setConversionService(conversionService);
		for (Map.Entry<String, UiConfigImpl> entry : uiConfigs.entrySet()) {
			Object value = bw.getPropertyValue(entry.getKey());
			if (value instanceof String) {
				String str = (String) value;
				if (str != null && entry.getValue().isTrim() && ("input".equals(entry.getValue().getType())
						|| "textarea".equals(entry.getValue().getType()))) {
					str = str.trim();
					if (str.isEmpty())
						str = null;
					if (bw.getPropertyDescriptor(entry.getKey()).getWriteMethod() != null)
						bw.setPropertyValue(entry.getKey(), str);
				}
				if (StringUtils.isNotBlank(str)) {
					int maxlength = entry.getValue().getMaxlength();
					if (maxlength == 0)
						maxlength = 255;
					if (maxlength > 0 && str.length() > maxlength) {
						addFieldError(getEntityName() + '.' + entry.getKey(),
								getText("validation.maxlength.violation", new String[] { String.valueOf(maxlength) }));
						return false;
					}
					String regex = entry.getValue().getRegex();
					if (StringUtils.isNotBlank(regex) && !str.matches(regex)) {
						addFieldError(getEntityName() + '.' + entry.getKey(), getText("validation.invalid"));
						return false;
					}
				}
			}
		}
		Map<String, NaturalId> naturalIds = getNaturalIds();
		boolean naturalIdMutable = isNaturalIdMutable();
		boolean caseInsensitive = AnnotationUtils
				.getAnnotatedPropertyNameAndAnnotations(getEntityClass(), CaseInsensitive.class).size() > 0;
		boolean isnew = _entity.isNew();
		if (idAssigned)
			isnew = "true".equals(ServletActionContext.getRequest().getParameter("_isnew"));
		if (isnew) {
			if (idAssigned) {
				UiConfigImpl uci = uiConfigs.get("id");
				if (uci != null && uci.getReadonly().isValue()) {
					if (_entity.getId() != null) {
						addFieldError("id", getText("validation.required"));
						return false;
					}
				}
				if (entityManager.exists(_entity.getId())) {
					addFieldError(getEntityName() + ".id", getText("validation.already.exists"));
					return false;
				}
			}
			if (naturalIds.size() > 0) {
				Serializable[] args = new Serializable[naturalIds.size() * 2];
				Iterator<String> it = naturalIds.keySet().iterator();
				int i = 0;
				try {
					while (it.hasNext()) {
						String name = it.next();
						args[i] = name;
						i++;
						args[i] = (Serializable) bw.getPropertyValue(name);
						i++;
					}
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
				}
				if (entityManager.existsOne(caseInsensitive, args)) {
					it = naturalIds.keySet().iterator();
					while (it.hasNext()) {
						String fieldName = getEntityName() + '.' + it.next();
						if (StringUtils.isBlank(targetField) || targetField.equals(fieldName)
								|| targetField.equals(fieldName + ".id"))
							addFieldError(targetField == null ? fieldName : targetField,
									getText("validation.already.exists"));
					}
					return false;
				}
			}
			for (Map.Entry<String, UiConfigImpl> entry : uiConfigs.entrySet()) {
				if (entry.getValue().isUnique() && StringUtils.isNotBlank(
						ServletActionContext.getRequest().getParameter(getEntityName() + '.' + entry.getKey()))) {
					if (entityManager.existsOne(entry.getKey(), (Serializable) bw.getPropertyValue(entry.getKey()))) {
						addFieldError(getEntityName() + '.' + entry.getKey(), getText("validation.already.exists"));
						return false;
					}
				}
			}
			try {
				Set<String> editedPropertyNames = new HashSet<>();
				for (String parameterName : ActionContext.getContext().getParameters().keySet()) {
					String propertyName = parameterName;
					if (propertyName.startsWith("__checkbox_" + getEntityName() + '.')
							|| propertyName.startsWith("__multiselect_" + getEntityName() + '.')
							|| propertyName.startsWith("__datagrid_" + getEntityName() + '.'))
						propertyName = propertyName.substring(propertyName.indexOf('.') + 1);
					if (propertyName.startsWith(getEntityName() + '.'))
						propertyName = propertyName.substring(propertyName.indexOf('.') + 1);
					if (propertyName.indexOf('.') > 0)
						propertyName = propertyName.substring(0, propertyName.indexOf('.'));
					if (propertyName.indexOf('[') > 0)
						propertyName = propertyName.substring(0, propertyName.indexOf('['));
					UiConfigImpl uiConfig = uiConfigs.get(propertyName);
					if ((propertyName.endsWith("FileName") || propertyName.endsWith("ContentType"))
							&& bw.isWritableProperty(propertyName)
							&& ServletActionContext.getRequest().getParameter(parameterName) == null) {
						editedPropertyNames.add(propertyName);
						continue;
					}
					if (uiConfig == null || uiConfig.getReadonly().isValue()
							|| fromList && uiConfig.getHiddenInList().isValue()
							|| !fromList && uiConfig.getHiddenInInput().isValue() || Persistable.class
									.isAssignableFrom(bw.getPropertyDescriptor(propertyName).getPropertyType()))
						continue;
					if (StringUtils.isNotBlank(uiConfig.getReadonly().getExpression()) && evalBoolean(
							uiConfig.getReadonly().getExpression(), _entity, bw.getPropertyValue(propertyName)))
						continue;
					if (fromList) {
						if (StringUtils.isNotBlank(uiConfig.getHiddenInList().getExpression()) && evalBoolean(
								uiConfig.getHiddenInList().getExpression(), _entity, bw.getPropertyValue(propertyName)))
							continue;
					} else {
						if (StringUtils.isNotBlank(uiConfig.getHiddenInInput().getExpression())
								&& evalBoolean(uiConfig.getHiddenInInput().getExpression(), _entity,
										bw.getPropertyValue(propertyName)))
							continue;
					}
					editedPropertyNames.add(propertyName);
					if (idAssigned && isnew)
						editedPropertyNames.add("id");
					if (isAttachmentable())
						editedPropertyNames.add("attachments");
				}
				_entity = getEntityClass().getConstructor().newInstance();
				BeanWrapperImpl bwp = new BeanWrapperImpl(_entity);
				bwp.setConversionService(conversionService);
				String versionPropertyName = getVersionPropertyName();
				if (versionPropertyName != null)
					editedPropertyNames.add(versionPropertyName);
				for (String name : editedPropertyNames)
					bwp.setPropertyValue(name, bw.getPropertyValue(name));
				bw = bwp;
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		} else {
			EN persisted = entityManager.get((Serializable) bw.getPropertyValue("id"));
			if (persisted == null) {
				addFieldError("id", getText("validation.not.exists"));
				return false;
			}
			BeanWrapperImpl bwp = new BeanWrapperImpl(persisted);
			bwp.setConversionService(conversionService);
			if (naturalIdMutable && naturalIds.size() > 0) {
				Serializable[] args = new Serializable[naturalIds.size() * 2];
				Iterator<String> it = naturalIds.keySet().iterator();
				boolean changed = false;
				int i = 0;
				while (it.hasNext()) {
					String name = it.next();
					args[i] = name;
					i++;
					args[i] = (Serializable) bw.getPropertyValue(name);
					Serializable oldValue = (Serializable) bwp.getPropertyValue(name);
					if (args[i] != null && !args[i].equals(oldValue))
						changed = true;
					i++;
				}
				if (changed && entityManager.existsOne(caseInsensitive, args)) {
					it = naturalIds.keySet().iterator();
					while (it.hasNext()) {
						String fieldName = getEntityName() + '.' + it.next();
						if (StringUtils.isBlank(targetField) || targetField.equals(fieldName)
								|| targetField.equals(fieldName + ".id"))
							addFieldError(targetField == null ? fieldName : targetField,
									getText("validation.already.exists"));
					}
					return false;
				}
			}
			for (Map.Entry<String, UiConfigImpl> entry : uiConfigs.entrySet()) {
				if (entry.getValue().isUnique() && StringUtils.isNotBlank(
						ServletActionContext.getRequest().getParameter(getEntityName() + '.' + entry.getKey()))) {
					Serializable newValue = (Serializable) bw.getPropertyValue(entry.getKey());
					Serializable oldValue = (Serializable) bwp.getPropertyValue(entry.getKey());
					boolean changed = (newValue != null && !newValue.equals(oldValue));
					if (changed && entityManager.existsOne(entry.getKey(), newValue)) {
						addFieldError(getEntityName() + '.' + entry.getKey(), getText("validation.already.exists"));
						return false;
					}
				}
			}
			try {
				String versionPropertyName = getVersionPropertyName();
				boolean forceOverride = false;
				if (versionPropertyName != null) {
					Object versionInDb = bwp.getPropertyValue(versionPropertyName);
					Object versionInUi = bw.getPropertyValue(versionPropertyName);
					forceOverride = versionInUi == null
							|| versionInUi instanceof Number && ((Number) versionInUi).intValue() < 0;
					if (!forceOverride && !Objects.equals(versionInDb, versionInUi)) {
						addActionError(getText("validation.version.conflict"));
						return false;
					}
				}

				Set<String> editedPropertyNames = new HashSet<>();
				for (String parameterName : ActionContext.getContext().getParameters().keySet()) {
					String propertyName = parameterName;
					if (propertyName.startsWith("__checkbox_" + getEntityName() + '.')
							|| propertyName.startsWith("__multiselect_" + getEntityName() + '.')
							|| propertyName.startsWith("__datagrid_" + getEntityName() + '.'))
						propertyName = propertyName.substring(propertyName.indexOf('.') + 1);
					if (propertyName.startsWith(getEntityName() + '.'))
						propertyName = propertyName.substring(propertyName.indexOf('.') + 1);
					if (propertyName.indexOf('.') > 0)
						propertyName = propertyName.substring(0, propertyName.indexOf('.'));
					if (propertyName.indexOf('[') > 0)
						propertyName = propertyName.substring(0, propertyName.indexOf('['));
					if (propertyName.equals(versionPropertyName)) {
						if (!forceOverride)
							editedPropertyNames.add(propertyName);
						continue;
					}
					UiConfigImpl uiConfig = uiConfigs.get(propertyName);
					if ((propertyName.endsWith("FileName") || propertyName.endsWith("ContentType"))
							&& bwp.isWritableProperty(propertyName)
							&& ServletActionContext.getRequest().getParameter(parameterName) == null) {
						editedPropertyNames.add(propertyName);
						continue;
					}
					if (uiConfig == null || uiConfig.getReadonly().isValue()
							|| fromList && uiConfig.getHiddenInList().isValue()
							|| !fromList && uiConfig.getHiddenInInput().isValue()
							|| !naturalIdMutable && naturalIds.keySet().contains(propertyName) || Persistable.class
									.isAssignableFrom(bwp.getPropertyDescriptor(propertyName).getPropertyType()))
						continue;
					if (StringUtils.isNotBlank(uiConfig.getReadonly().getExpression()) && evalBoolean(
							uiConfig.getReadonly().getExpression(), persisted, bwp.getPropertyValue(propertyName)))
						continue;
					if (fromList) {
						if (StringUtils.isNotBlank(uiConfig.getHiddenInList().getExpression()) && evalBoolean(
								uiConfig.getHiddenInList().getExpression(), _entity, bw.getPropertyValue(propertyName)))
							continue;
					} else {
						if (StringUtils.isNotBlank(uiConfig.getHiddenInInput().getExpression())
								&& evalBoolean(uiConfig.getHiddenInInput().getExpression(), _entity,
										bw.getPropertyValue(propertyName)))
							continue;
					}
					editedPropertyNames.add(propertyName);
				}
				if (isAttachmentable())
					editedPropertyNames.add("attachments");
				for (String name : editedPropertyNames) {
					Object oldValue = bwp.getPropertyValue(name);
					Object newValue = bw.getPropertyValue(name);
					if (CompareUtils.equals(oldValue, newValue))
						continue;
					bwp.setPropertyValue(name, newValue);
				}
				bw = bwp;
				_entity = persisted;
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
		if (isTreeable()) {
			Collection siblings = null;
			BaseTreeableEntity treeEntity = (BaseTreeableEntity) _entity;
			BaseTreeableEntity parentEntity = (BaseTreeableEntity) entityManager.get(parent);
			if (parentEntity == null) {
				DetachedCriteria dc = entityManager.detachedCriteria();
				dc.add(Restrictions.isNull("parent"));
				dc.addOrder(Order.asc("displayOrder"));
				dc.addOrder(Order.asc("name"));
				siblings = entityManager.findListByCriteria(dc);
			} else {
				siblings = parentEntity.getChildren();
			}
			for (Object o : siblings) {
				BaseTreeableEntity sibling = (BaseTreeableEntity) o;
				if (!treeEntity.isNew() && sibling.getId().equals(treeEntity.getId()))
					continue;
				entityManager.evict((EN) sibling);
				String name = sibling.getName();
				if (name.equals(treeEntity.getName()) || AnnotationUtils
						.getAnnotatedPropertyNameAndAnnotations(getEntityClass(), CaseInsensitive.class)
						.containsKey("name") && name.equalsIgnoreCase(treeEntity.getName())) {
					addFieldError(getEntityName() + ".name", getText("validation.already.exists"));
					return false;
				}
			}
		}
		try {
			for (String propertyName : getUiConfigs().keySet()) {
				UiConfigImpl uiConfig = getUiConfigs().get(propertyName);
				Class type = bw.getPropertyDescriptor(propertyName).getPropertyType();
				if (uiConfig.getReadonly().isValue()
						|| !naturalIdMutable && naturalIds.keySet().contains(propertyName) && !isnew)
					continue;
				if (uiConfig.getType().equals("collection") && uiConfig.getElementType() != null) {
					// remove empty element from collection
					Collection collection = (Collection) bw.getPropertyValue(propertyName);
					if (collection != null) {
						Iterator it = collection.iterator();
						while (it.hasNext()) {
							Object element = it.next();
							BeanWrapperImpl bwe = new BeanWrapperImpl(element);
							for (PropertyDescriptor pd : bwe.getPropertyDescriptors()) {
								// remove Persistable
								if (pd.getReadMethod() != null && pd.getWriteMethod() != null
										&& Persistable.class.isAssignableFrom(pd.getPropertyType())) {
									Persistable p = (Persistable) bwe.getPropertyValue(pd.getName());
									if (p != null && p.isNew())
										bwe.setPropertyValue(pd.getName(), null);
								}
							}
							if (BeanUtils.isEmpty(element))
								it.remove();
						}
					}
				}
				if (!Persistable.class.isAssignableFrom(type)
						|| StringUtils.isNotBlank(uiConfig.getReadonly().getExpression()) && evalBoolean(
								uiConfig.getReadonly().getExpression(), _entity, bw.getPropertyValue(propertyName)))
					continue;
				String parameterValue = ServletActionContext.getRequest()
						.getParameter(getEntityName() + '.' + propertyName);
				if (parameterValue == null)
					parameterValue = ServletActionContext.getRequest()
							.getParameter(getEntityName() + '.' + propertyName + ".id");
				if (parameterValue == null)
					parameterValue = ServletActionContext.getRequest().getParameter(propertyName + "Id");
				if (parameterValue == null) {
					continue;
				} else if (StringUtils.isBlank(parameterValue)) {
					bw.setPropertyValue(propertyName, null);
				} else {
					String listKey = uiConfig.getListKey();
					if (StringUtils.isBlank(listKey))
						listKey = "id";
					BeanWrapperImpl temp = new BeanWrapperImpl(type.getConstructor().newInstance());
					temp.setConversionService(conversionService);
					temp.setPropertyValue(listKey, parameterValue);
					BaseManager em = getEntityManager(type);
					Persistable obj;
					if (listKey.equals("id"))
						obj = em.get((Serializable) temp.getPropertyValue(listKey));
					else
						obj = em.findOne(listKey, (Serializable) temp.getPropertyValue(listKey));
					em.evict(obj);
					bw.setPropertyValue(propertyName, obj);
					em = getEntityManager(getEntityClass());
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return true;
	}

	protected void beforeSave(EN en) {

	}

	protected void afterSave(EN en) {

	}

	private boolean checkEntityReadonly(String expression, EN entity) throws Exception {
		if (StringUtils.isNotBlank(expression)) {
			Template template = new Template(null, "${(" + expression + ")?string!}", freemarkerManager.getConfig());
			StringWriter sw = new StringWriter();
			Map<String, Object> rootMap = new HashMap<>(2, 1);
			rootMap.put("entity", entity);
			template.process(rootMap, sw);
			return sw.toString().equals("true");
		}
		return false;
	}

	private boolean evalBoolean(String expression, EN entity, Object value) {
		if (StringUtils.isNotBlank(expression)) {
			try {
				Template template = new Template(null, "${(" + expression + ")?string!}",
						freemarkerManager.getConfig());
				StringWriter sw = new StringWriter();
				Map<String, Object> rootMap = new HashMap<>(4, 1);
				rootMap.put("entity", entity);
				rootMap.put("value", value);
				template.process(rootMap, sw);
				return sw.toString().equals("true");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	private Tuple<Owner, Class<?>> getOwnerProperty() {
		Owner owner = getEntityClass().getAnnotation(Owner.class);
		if (owner == null)
			return null;
		String propertyName = owner.propertyName();
		if (StringUtils.isBlank(propertyName))
			return null;
		BeanWrapperImpl bw = new BeanWrapperImpl(getEntityClass());
		bw.setConversionService(conversionService);
		Class type = bw.getPropertyType(propertyName);
		if (type == null)
			throw new IllegalArgumentException("No Such property " + propertyName + " of " + getEntityClass());
		if (!UserDetails.class.isAssignableFrom(type) && String.class != type)
			throw new IllegalArgumentException("property " + propertyName + " of " + getEntityClass()
					+ " is not String or instanceof " + UserDetails.class);
		return Tuple.of(owner, type);
	}

	@Override
	public String view() throws Exception {
		tryFindEntity();
		if (_entity == null)
			return NOTFOUND;
		Tuple<Owner, Class<?>> ownerProperty = getOwnerProperty();
		if (ownerProperty != null) {
			Owner owner = ownerProperty.getKey();
			if (!(StringUtils.isNotBlank(owner.supervisorRole())
					&& AuthzUtils.authorize(null, owner.supervisorRole(), null)) && owner.isolate()) {
				if (!hasOwnership(_entity)) {
					addActionError(getText("access.denied"));
					return ACCESSDENIED;
				}
			}
		}
		putEntityToValueStack(_entity);
		return VIEW;
	}

	public String export() throws Exception {
		if (!getRichtableConfig().isExportable())
			return NOTFOUND;
		tryFindEntity();
		if (_entity == null)
			return NOTFOUND;
		BeanWrapperImpl bwi = new BeanWrapperImpl(_entity);
		Tuple<Owner, Class<?>> ownerProperty = getOwnerProperty();
		if (ownerProperty != null) {
			Owner owner = ownerProperty.getKey();
			if (!(StringUtils.isNotBlank(owner.supervisorRole())
					&& AuthzUtils.authorize(null, owner.supervisorRole(), null)) && owner.isolate()) {
				if (!hasOwnership(_entity)) {
					addActionError(getText("access.denied"));
					return ACCESSDENIED;
				}
			}
		}
		Map<String, Object> map = new LinkedHashMap<>();
		Set<String> jsonIgnores = AnnotationUtils.getAnnotatedPropertyNames(getEntityClass(), JsonIgnore.class);
		for (Map.Entry<String, UiConfigImpl> entry : getUiConfigs().entrySet()) {
			if (jsonIgnores.contains(entry.getKey()))
				continue;
			Object value = bwi.getPropertyValue(entry.getKey());
			if (value == null)
				continue;
			HiddenImpl hidden = entry.getValue().getHiddenInView();
			if (hidden.isValue() || StringUtils.isNotBlank(hidden.getExpression())
					&& evalBoolean(hidden.getExpression(), _entity, value))
				continue;
			if (value instanceof Persistable)
				value = String.valueOf(value);
			map.put(entry.getKey(), value);
		}
		String json = JsonUtils.toJson(map);
		HttpServletResponse response = ServletActionContext.getResponse();
		response.setHeader("Content-type", "application/json");
		String filename = String.valueOf(_entity.getId());
		if (bwi.isReadableProperty("name"))
			filename = String.valueOf(bwi.getPropertyValue("name"));
		filename = URLEncoder.encode(filename, "UTF-8");
		response.setHeader("Content-disposition", "attachment;filename=" + filename + ".json");
		PrintWriter out = response.getWriter();
		out.print(json);
		out.flush();
		out.close();
		return NONE;
	}

	@Override
	public String delete() throws Exception {
		if (getReadonly().isValue() && !getReadonly().isDeletable()) {
			addActionError(getText("access.denied"));
			return ACCESSDENIED;
		}
		return doDelete();
	}

	protected String doDelete() throws Exception {
		BaseManager<EN> entityManager = getEntityManager(getEntityClass());
		String[] arr = getId();
		Serializable[] id = (arr != null) ? new Serializable[arr.length] : new Serializable[0];
		try {
			BeanWrapperImpl bw = new BeanWrapperImpl(getEntityClass().getConstructor().newInstance());
			bw.setConversionService(conversionService);
			for (int i = 0; i < id.length; i++) {
				bw.setPropertyValue("id", arr[i]);
				id[i] = (Serializable) bw.getPropertyValue("id");
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		if (id.length > 0) {
			Tuple<Owner, Class<?>> ownerProperty = getOwnerProperty();

			if (ownerProperty != null) {
				Owner owner = ownerProperty.getKey();
				if (!(StringUtils.isNotBlank(owner.supervisorRole())
						&& AuthzUtils.authorize(null, owner.supervisorRole(), null))) {
					if (ownerProperty.getValue() == String.class && AuthzUtils.getUsername() == null
							|| ownerProperty.getValue() != String.class && AuthzUtils
									.getUserDetails((Class<? extends UserDetails>) ownerProperty.getValue()) == null) {
						addActionError(getText("access.denied"));
						return ACCESSDENIED;
					}
				}
			}
			boolean deletable = true;
			String expression = getReadonly().getExpression();
			if (ownerProperty != null || StringUtils.isNotBlank(expression)) {
				for (Serializable uid : id) {
					EN en = entityManager.get(uid);
					if (en == null)
						continue;
					if (ownerProperty != null) {
						Owner owner = ownerProperty.getKey();
						if (!(StringUtils.isNotBlank(owner.supervisorRole())
								&& AuthzUtils.authorize(null, owner.supervisorRole(), null))
								&& (owner.isolate() || owner.readonlyForOther())) {
							BeanWrapperImpl bwi = new BeanWrapperImpl(en);
							bwi.setConversionService(conversionService);
							Object value = bwi.getPropertyValue(owner.propertyName());
							if (ownerProperty.getValue() == String.class) {
								if (value == null || !AuthzUtils.getUsername().equals(value)) {
									addActionError(getText("delete.forbidden", new String[] { en.toString() }));
									deletable = false;
									break;
								}
							} else {
								if (value == null || !AuthzUtils
										.getUserDetails((Class<? extends UserDetails>) ownerProperty.getValue())
										.equals(value)) {
									addActionError(getText("delete.forbidden", new String[] { en.toString() }));
									deletable = false;
									break;
								}
							}
						}
					}
					if (StringUtils.isNotBlank(expression) && checkEntityReadonly(expression, en)
							&& !getReadonly().isDeletable()) {
						addActionError(getText("delete.forbidden", new String[] { en.toString() }));
						deletable = false;
						break;
					}
				}
			}
			if (deletable) {
				List<EN> list = entityManager.delete(id);
				if (list.size() > 0)
					notify("delete.success");
			}
		}
		return SUCCESS;
	}

	public String csv() throws Exception {
		HttpServletRequest request = ServletActionContext.getRequest();
		HttpServletResponse response = ServletActionContext.getResponse();
		String columns = request.getParameter("columns");
		List<String> columnsList = null;
		final List<String> exportColumnsList = new ArrayList<>();
		final Map<String, Number> sumColumns = new HashMap<>();
		final Map<String, Template> csvTemplates = new HashMap<>();
		if (StringUtils.isNotBlank(columns))
			columnsList = Arrays.asList(columns.split("\\s*,\\s*"));
		for (Map.Entry<String, UiConfigImpl> entry : getUiConfigs().entrySet()) {
			String name = entry.getKey();
			if (columnsList != null && !columnsList.contains(name))
				continue;
			UiConfigImpl uc = entry.getValue();
			HiddenImpl hidden = uc.getHiddenInList();
			if (hidden.isValue())
				continue;
			exportColumnsList.add(name);
			if (uc.isShowSum())
				sumColumns.put(name, null);
			if (StringUtils.isNotBlank(uc.getCsvTemplate()))
				csvTemplates.put(name, new Template(null, uc.getCsvTemplate(), freemarkerManager.getConfig()));
		}

		DetachedCriteria dc = detachedCriteria();
		BaseManager entityManager = getEntityManager(getEntityClass());
		long count = entityManager.countByCriteria(dc);
		int maxRows = getCsvMaxRows();
		if (count == 0) {
			addActionError(getText("query.result.empty"));
			return ERROR;
		} else if (count > maxRows) {
			addActionError(getText("query.result.number.exceed", new String[] { String.valueOf(maxRows) }));
			return ERROR;
		}
		response.setCharacterEncoding(csvDefaultEncoding);
		response.setHeader("Content-type", "text/csv");
		response.setHeader("Content-disposition", "attachment;filename=data.csv");
		final String columnSeperator = ",";
		final String lineSeperator = "\r\n";
		final PrintWriter writer = response.getWriter();
		for (int i = 0; i < exportColumnsList.size(); i++) {
			String label = exportColumnsList.get(i);
			UiConfigImpl uic = getUiConfigs().get(label);
			if (uic != null && StringUtils.isNotBlank(uic.getAlias()))
				label = uic.getAlias();
			writer.print(getText(label));
			writer.print(i == exportColumnsList.size() - 1 ? lineSeperator : columnSeperator);
		}
		entityManager.iterate(10, (entityArray, session) -> {
			for (Object en : entityArray) {
				BeanWrapperImpl bw = new BeanWrapperImpl(en);
				for (int i = 0; i < exportColumnsList.size(); i++) {
					String name = exportColumnsList.get(i);
					Object value = bw.getPropertyValue(name);
					if (value != null && sumColumns.containsKey(name)) {
						Class<?> clazz = value.getClass();
						Object sum;
						if (clazz == float.class || clazz == Float.class) {
							BigDecimal old = (BigDecimal) sumColumns.get(name);
							if (old == null)
								old = BigDecimal.ZERO;
							sum = old.add(new BigDecimal((Float) value));
						} else if (clazz == double.class || clazz == Double.class) {
							BigDecimal old = (BigDecimal) sumColumns.get(name);
							if (old == null)
								old = BigDecimal.ZERO;
							sum = old.add(new BigDecimal((Double) value));
						} else if (clazz == BigDecimal.class) {
							BigDecimal old = (BigDecimal) sumColumns.get(name);
							if (old == null)
								old = BigDecimal.ZERO;
							sum = old.add((BigDecimal) value);
						} else if (clazz == short.class || clazz == Short.class) {
							Long old = (Long) sumColumns.get(name);
							if (old == null)
								old = 0L;
							sum = old + (Short) value;
						} else if (clazz == int.class || clazz == Integer.class) {
							Long old = (Long) sumColumns.get(name);
							if (old == null)
								old = 0L;
							sum = old + (Integer) value;
						} else if (clazz == long.class || clazz == Long.class) {
							Long old = (Long) sumColumns.get(name);
							if (old == null)
								old = 0L;
							sum = old + (Long) value;
						} else {
							throw new IllegalArgumentException("Unsupported type: " + clazz.getName());
						}
						sumColumns.put(name, (Number) sum);
					}
					String text;
					Template csvTemplate = csvTemplates.get(name);
					if (csvTemplate != null) {
						StringWriter sw = new StringWriter();
						Map<String, Object> rootMap = new HashMap<>(4, 1);
						rootMap.put("entity", en);
						rootMap.put("value", value);
						try {
							csvTemplate.process(rootMap, sw);
							text = sw.toString();
						} catch (Exception e) {
							text = e.getMessage();
						}
					} else {
						if (value == null) {
							text = "";
						} else if (value instanceof Collection) {
							text = StringUtils.join((Collection) value, ",");
						} else if (value.getClass().isArray()) {
							text = StringUtils.join((Object[]) value, ",");
						} else if (value instanceof Boolean) {
							text = getText(value.toString());
						} else if (value instanceof Date) {
							if (value instanceof Time) {
								text = DateUtils.format((Date) value, "HH:mm:ss");
							} else if (value instanceof java.sql.Date) {
								text = DateUtils.formatDate8((Date) value);
							} else {
								text = DateUtils.formatDatetime((Date) value);
							}
						} else {
							text = String.valueOf(value);
						}
					}
					if (text.contains(String.valueOf(columnSeperator)) || text.contains("\"") || text.contains("\n")) {
						if (text.contains("\""))
							text = text.replaceAll("\"", "\"\"");
						text = new StringBuilder(text.length() + 2).append("\"").append(text).append("\"").toString();
					}
					writer.print(text);
					writer.print(i == exportColumnsList.size() - 1 ? lineSeperator : columnSeperator);
				}

			}
			writer.flush();
		}, dc);
		if (!sumColumns.isEmpty()) {
			for (int i = 0; i < exportColumnsList.size(); i++) {
				String name = exportColumnsList.get(i);
				if (sumColumns.containsKey(name)) {
					Object value = sumColumns.get(name);
					String text = value.toString();
					Template csvTemplate = csvTemplates.get(name);
					if (csvTemplate != null) {
						StringWriter sw = new StringWriter();
						Map<String, Object> rootMap = new HashMap<>(4, 1);
						rootMap.put("value", value);
						try {
							csvTemplate.process(rootMap, sw);
							text = sw.toString();
						} catch (Exception e) {
							text = e.getMessage();
						}
					}
					if (text.contains(String.valueOf(columnSeperator)))
						text = new StringBuilder(text.length() + 2).append("\"").append(text).append("\"").toString();
					writer.print(text);
				}
				writer.print(i == exportColumnsList.size() - 1 ? lineSeperator : columnSeperator);
			}
		}
		return NONE;
	}

	public String enable() throws Exception {
		if (!isEnableable() || getReadonly().isValue())
			return ACCESSDENIED;
		return updateEnabled(true);
	}

	public String disable() throws Exception {
		if (!isEnableable() || getReadonly().isValue())
			return ACCESSDENIED;
		return updateEnabled(false);
	}

	protected String updateEnabled(boolean enabled) throws Exception {
		BaseManager<EN> em = getEntityManager(getEntityClass());
		String[] arr = getId();
		Serializable[] id = (arr != null) ? new Serializable[arr.length] : new Serializable[0];
		try {
			BeanWrapperImpl bw = new BeanWrapperImpl(getEntityClass().getConstructor().newInstance());
			bw.setConversionService(conversionService);
			for (int i = 0; i < id.length; i++) {
				bw.setPropertyValue("id", arr[i]);
				id[i] = (Serializable) bw.getPropertyValue("id");
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		if (id.length > 0) {
			Tuple<Owner, Class<?>> ownerProperty = getOwnerProperty();
			if (ownerProperty != null) {
				Owner owner = ownerProperty.getKey();
				if (!(StringUtils.isNotBlank(owner.supervisorRole())
						&& AuthzUtils.authorize(null, owner.supervisorRole(), null))) {
					if (ownerProperty.getValue() == String.class && AuthzUtils.getUsername() == null
							|| ownerProperty.getValue() != String.class && AuthzUtils
									.getUserDetails((Class<? extends UserDetails>) ownerProperty.getValue()) == null) {
						addActionError(getText("access.denied"));
						return ACCESSDENIED;
					}
				}
			}
			for (Serializable s : id) {
				Enableable en = (Enableable) em.get(s);
				if (en == null || en.isEnabled() == enabled)
					continue;
				if (ownerProperty != null) {
					Owner owner = ownerProperty.getKey();
					if (!(StringUtils.isNotBlank(owner.supervisorRole())
							&& AuthzUtils.authorize(null, owner.supervisorRole(), null))
							&& (owner.isolate() || owner.readonlyForOther())) {
						BeanWrapperImpl bwi = new BeanWrapperImpl(en);
						bwi.setConversionService(conversionService);
						Object value = bwi.getPropertyValue(owner.propertyName());
						if (ownerProperty.getValue() == String.class) {
							if (value == null || !AuthzUtils.getUsername().equals(value))
								continue;
						} else {
							if (value == null || !AuthzUtils
									.getUserDetails((Class<? extends UserDetails>) ownerProperty.getValue())
									.equals(value))
								continue;
						}
					}
				}
				String expression = getReadonly().getExpression();
				if (StringUtils.isNotBlank(expression) && checkEntityReadonly(expression, (EN) en))
					continue;
				en.setEnabled(enabled);
				em.save((EN) en);
			}
			notify("operate.success");
		}
		return SUCCESS;
	}

	@InputConfig(resultName = "move")
	public String move() throws Exception {
		if (!isTreeable())
			return NOTFOUND;
		String id = getUid();
		BaseTreeableEntity entity = null;
		BaseTreeableEntity parentEntity = null;
		BaseManager em = getEntityManager(getEntityClass());
		if (StringUtils.isNumeric(id))
			entity = (BaseTreeableEntity) em.get(Long.valueOf(id));
		if (entity == null) {
			addActionError(getText("validation.required"));
			return SUCCESS;
		}
		if (parent != null && parent > 0)
			parentEntity = (BaseTreeableEntity) em.get(Long.valueOf(parent));
		if (parentEntity != null && parentEntity.getFullId().startsWith(entity.getFullId())
				|| entity.getParent() == null && parentEntity == null || entity.getParent() != null
						&& parentEntity != null && entity.getParent().getId().equals(parentEntity.getId())) {
			addActionError(getText("validation.invalid"));
			return SUCCESS;
		}
		entity.setParent(parentEntity);
		em.save(entity);
		notify("operate.success");
		return SUCCESS;
	}

	@JsonConfig(root = "children")
	@Authorize(ifNotGranted = UserRole.ROLE_BUILTIN_ANONYMOUS)
	public String children() {
		if (!isTreeable())
			return NOTFOUND;
		BaseTreeableEntity parentEntity;
		BaseTreeControl baseTreeControl = null;
		Collection<BaseTreeControl> baseTreeControls = ApplicationContextUtils.getBeansOfType(BaseTreeControl.class)
				.values();
		for (BaseTreeControl btc : baseTreeControls) {
			if (ReflectionUtils.getGenericClass(btc.getClass()) == getEntityClass()) {
				baseTreeControl = btc;
				break;
			}
		}
		if (baseTreeControl != null) {
			if (parent == null || parent < 1) {
				if (tree != null && tree > 0) {
					children = new ArrayList<>();
					children.add((EN) baseTreeControl.getTree().getDescendantOrSelfById(tree));
					return JSON;
				} else {
					parentEntity = baseTreeControl.getTree();
				}
			} else {
				parentEntity = baseTreeControl.getTree().getDescendantOrSelfById(parent);
			}
			if (parentEntity != null)
				children = parentEntity.getChildren();
		} else {
			BaseManager entityManager = getEntityManager(getEntityClass());
			if (parent == null || parent < 1) {
				if (tree != null && tree > 0) {
					children = new ArrayList<>();
					children.add((EN) entityManager.get(tree));
				} else {
					DetachedCriteria dc = entityManager.detachedCriteria();
					dc.add(Restrictions.isNull("parent")).addOrder(Order.asc("displayOrder"))
							.addOrder(Order.asc("name"));
					children = entityManager.findListByCriteria(dc);
				}
			} else {
				parentEntity = (BaseTreeableEntity) entityManager.get(parent);
				if (parentEntity != null)
					children = parentEntity.getChildren();
			}
		}
		children = CriterionUtils.filter(children);
		return JSON;
	}

	public String treeview() throws Exception {
		if (!isTreeable())
			return NOTFOUND;
		if (parent != null && parent > 0) {
			_entity = getEntityManager(getEntityClass()).get(parent);
			if (_entity == null)
				return NOTFOUND;
			putEntityToValueStack(_entity);
		}
		return "treeview";
	}

	private Collection<EN> children;

	public Collection<EN> getChildren() {
		return children;
	}

	@JsonConfig(root = "suggestions")
	@Authorize(ifNotGranted = UserRole.ROLE_BUILTIN_ANONYMOUS)
	public String suggestion() {
		final String propertyName = getUid();
		if (StringUtils.isBlank(propertyName) || StringUtils.isBlank(keyword))
			return NONE;
		UiConfigImpl uic = getUiConfigs().get(propertyName);
		if (uic == null
				|| !uic.isUnique() && !(getNaturalIds().size() == 1 && getNaturalIds().containsKey(propertyName)))
			return NONE;
		suggestions = getEntityManager(getEntityClass()).executeFind(session -> {
			StringBuilder hql = new StringBuilder("select ").append(propertyName).append(" from ")
					.append(getEntityClass().getSimpleName()).append(" where ").append(propertyName)
					.append(" like :keyword");
			Query<String> q = session.createQuery(hql.toString(), String.class);
			q.setParameter("keyword", keyword + "%");
			q.setMaxResults(20);
			return q.list();
		});
		return JSON;
	}

	protected List suggestions;

	public List getSuggestions() {
		return suggestions;
	}

	@Override
	protected Authorize findAuthorize() {
		Authorize authorize = super.findAuthorize();
		if (authorize == null) {
			Class<?> c = getEntityClass();
			authorize = c.getAnnotation(Authorize.class);
		}
		return authorize;
	}

	@Override
	protected DoubleChecker findDoubleChecker() {
		DoubleChecker doubleCheck = super.findDoubleChecker();
		if (doubleCheck == null
				&& ActionContext.getContext().getActionInvocation().getProxy().getMethod().equals("save")) {
			Map<String, DoubleChecker> map = AnnotationUtils.getAnnotatedPropertyNameAndAnnotations(getEntityClass(),
					DoubleChecker.class);
			doubleCheck = map.size() > 0 ? map.values().iterator().next() : null;
		}
		return doubleCheck;
	}

	// need call once before view
	protected Class<EN> getEntityClass() {
		if (entityClass == null)
			entityClass = (Class<EN>) ReflectionUtils.getGenericClass(getClass());
		if (entityClass == null) {
			ActionProxy proxy = ActionContext.getContext().getActionInvocation().getProxy();
			String actionName = getEntityName();
			String namespace = proxy.getNamespace();
			entityClass = (Class<EN>) AutoConfigPackageProvider.getEntityClass(namespace, actionName);
		}
		return entityClass;
	}

	private Class<EN> entityClass;

	private void putEntityToValueStack(Persistable entity) {
		ValueStack vs = ActionContext.getContext().getValueStack();
		if (entity != null)
			vs.set(getEntityName(), entity);
	}

	protected EN constructEntity() {
		Persistable entity = null;
		try {
			entity = getEntityClass().getConstructor().newInstance();
			ValueStack temp = valueStackFactory.createValueStack();
			temp.set(getEntityName(), entity);
			Map<String, Object> context = temp.getContext();
			Map<String, Object> parameters = ActionContext.getContext().getParameters();
			try {
				ReflectionContextState.setCreatingNullObjects(context, true);
				ReflectionContextState.setDenyMethodExecution(context, true);
				for (Map.Entry<String, Object> entry : parameters.entrySet()) {
					String name = entry.getKey();
					Object value = entry.getValue();
					if (value instanceof String[]) {
						String[] arr = (String[]) entry.getValue();
						if (name.startsWith(getEntityName() + ".")) {
							if (name.split("\\.").length > 2) {
								if (arr.length == 1 && StringUtils.isEmpty(arr[0])) {
									arr = null;
								}
							}
						}
						value = arr;
					}
					temp.setParameter(name, value);
				}
			} finally {
				ReflectionContextState.setCreatingNullObjects(context, false);
				ReflectionContextState.setDenyMethodExecution(context, false);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return (EN) entity;
	}

	private boolean hasOwnership(Persistable<?> entity) {
		BeanWrapperImpl bw = new BeanWrapperImpl(entity);
		bw.setConversionService(conversionService);
		Tuple<Owner, Class<?>> ownerProperty = getOwnerProperty();
		if (ownerProperty != null) {
			Object value = bw.getPropertyValue(ownerProperty.getKey().propertyName());
			if (ownerProperty.getValue() == String.class) {
				String username = AuthzUtils.getUsername();
				if (username == null || value == null || !username.equals(value)) {
					return false;
				}
			} else {
				UserDetails ud = AuthzUtils.getUserDetails((Class<? extends UserDetails>) ownerProperty.getValue());
				if (ud == null || value == null || !ud.equals(value)) {
					return false;
				}
			}
		}
		return true;
	}

	@Inject
	private ValueStackFactory valueStackFactory;

	@Inject
	private FreemarkerManager freemarkerManager;

}
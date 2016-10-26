package org.ironrhino.core.struts;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
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
import java.util.Set;

import javax.persistence.Version;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.apache.struts2.views.freemarker.FreemarkerManager;
import org.hibernate.Query;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.NaturalId;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.ironrhino.core.hibernate.CriteriaState;
import org.ironrhino.core.hibernate.CriterionUtils;
import org.ironrhino.core.metadata.AppendOnly;
import org.ironrhino.core.metadata.Authorize;
import org.ironrhino.core.metadata.CaseInsensitive;
import org.ironrhino.core.metadata.JsonConfig;
import org.ironrhino.core.metadata.Owner;
import org.ironrhino.core.metadata.Readonly;
import org.ironrhino.core.metadata.Richtable;
import org.ironrhino.core.metadata.UiConfig;
import org.ironrhino.core.model.Attachmentable;
import org.ironrhino.core.model.BaseTreeableEntity;
import org.ironrhino.core.model.Enableable;
import org.ironrhino.core.model.Ordered;
import org.ironrhino.core.model.Persistable;
import org.ironrhino.core.model.ResultPage;
import org.ironrhino.core.model.Tuple;
import org.ironrhino.core.search.elasticsearch.ElasticSearchCriteria;
import org.ironrhino.core.search.elasticsearch.ElasticSearchService;
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

@SuppressWarnings({ "unchecked", "rawtypes" })
public class EntityAction<EN extends Persistable<?>> extends BaseAction {

	private static final long serialVersionUID = -8442983706126047413L;

	protected static Logger logger = LoggerFactory.getLogger(EntityAction.class);

	private ReadonlyImpl _readonly;

	private RichtableImpl _richtableConfig;

	private Map<String, UiConfigImpl> _uiConfigs;

	private Persistable _entity;

	private String _entityName;

	private Map<String, NaturalId> _naturalIds;

	protected ResultPage<EN> resultPage;

	protected Long tree;

	protected Long parent;

	protected BaseTreeableEntity parentEntity;

	@Autowired(required = false)
	protected transient ElasticSearchService<EN> elasticSearchService;

	@Autowired(required = false)
	protected transient ConversionService conversionService;

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

	public Persistable getEntity() {
		return _entity;
	}

	public ResultPage getResultPage() {
		return resultPage;
	}

	public void setResultPage(ResultPage resultPage) {
		this.resultPage = resultPage;
	}

	public Long getTree() {
		return tree;
	}

	public void setTree(Long tree) {
		this.tree = tree;
	}

	public Long getParent() {
		return parent;
	}

	public void setParent(Long parent) {
		this.parent = parent;
	}

	public BaseTreeableEntity getParentEntity() {
		return parentEntity;
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
			Immutable immutable = getEntityClass().getAnnotation(Immutable.class);
			if (immutable != null) {
				_readonly = new ReadonlyImpl();
				_readonly.setValue(true);
				return _readonly;
			}
			AppendOnly appendOnly = getEntityClass().getAnnotation(AppendOnly.class);
			if (appendOnly != null) {
				_readonly = new ReadonlyImpl();
				_readonly.setValue(false);
				_readonly.setExpression("!entity.new");
				_readonly.setDeletable(false);
				return _readonly;
			}
			Richtable rconfig = getClass().getAnnotation(Richtable.class);
			if (rconfig == null)
				rconfig = getEntityClass().getAnnotation(Richtable.class);
			Readonly rc = null;
			if (rconfig != null)
				rc = rconfig.readonly();
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
		Set<String> names = AnnotationUtils.getAnnotatedPropertyNames(getEntityClass(), Version.class);
		return names.isEmpty() ? null : names.iterator().next();
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

	protected void setEntity(Persistable<?> entity) {
		this._entity = entity;
	}

	protected void tryFindEntity() {
		BaseManager entityManager = getEntityManager(getEntityClass());
		try {
			BeanWrapperImpl bw = new BeanWrapperImpl(getEntityClass().newInstance());
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
		BeanWrapperImpl bw = new BeanWrapperImpl(getEntityClass().newInstance());
		bw.setConversionService(conversionService);
		Richtable richtableConfig = getClass().getAnnotation(Richtable.class);
		if (richtableConfig == null)
			richtableConfig = getEntityClass().getAnnotation(Richtable.class);
		final BaseManager entityManager = getEntityManager(getEntityClass());
		boolean searchable = isSearchable();
		Tuple<Owner, Class<?>> ownerProperty = getOwnerProperty();
		if (ownerProperty != null && ownerProperty.getKey().isolate()
				|| (!searchable || StringUtils.isBlank(keyword) || (searchable && elasticSearchService == null))) {
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
			resultPage.setCriteria(doPrepareCriteria(entityManager, bw, richtableConfig, searchable, ownerProperty));
			resultPage = entityManager.findByResultPage(resultPage);
		} else {
			Set<String> searchableProperties = new HashSet<>();
			for (Map.Entry<String, UiConfigImpl> entry : getUiConfigs().entrySet()) {
				if (entry.getValue().isSearchable())
					searchableProperties.add(entry.getKey());
			}
			String query = keyword.trim();
			ElasticSearchCriteria criteria = new ElasticSearchCriteria();
			criteria.setQuery(query);
			criteria.setTypes(new String[] { getEntityName() });
			if (richtableConfig != null && StringUtils.isNotBlank(richtableConfig.order())) {
				String[] ar = richtableConfig.order().split(",");
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
			resultPage = elasticSearchService.search(resultPage, source -> (EN) entityManager.get(source.getId()));
		}
		return LIST;
	}

	protected DetachedCriteria detachedCriteria() throws Exception {
		BaseManager entityManager = getEntityManager(getEntityClass());
		BeanWrapperImpl bw = new BeanWrapperImpl(getEntityClass().newInstance());
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
						propertyNamesInLike.put(entry.getKey(),
								entry.getValue().isExactMatch() ? MatchMode.EXACT : MatchMode.ANYWHERE);
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
				String[] ar = richtableConfig.order().split(",");
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

	protected void prepare(ElasticSearchCriteria esc) {

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
		if (_entity == null)
			_entity = getEntityClass().newInstance();
		BeanWrapperImpl bw = new BeanWrapperImpl(_entity);
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
						BeanWrapperImpl bwt = new BeanWrapperImpl(type.newInstance());
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
		if (isIdAssigned())
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
		BaseManager<Persistable<?>> entityManager = getEntityManager(getEntityClass());
		beforeSave((EN) _entity);
		entityManager.save(_entity);
		afterSave((EN) _entity);
		addActionMessage(getText("save.success"));
		return SUCCESS;
	}

	public String checkavailable() {
		return makeEntityValid() ? NONE : INPUT;
	}

	protected boolean makeEntityValid() {
		HttpServletRequest request = ServletActionContext.getRequest();
		String targetField = request.getHeader("X-Target-Field");
		boolean idAssigned = isIdAssigned();
		boolean fromList = "cell".equalsIgnoreCase(request.getHeader("X-Edit"));
		Map<String, UiConfigImpl> uiConfigs = getUiConfigs();
		BaseManager<Persistable<?>> entityManager = getEntityManager(getEntityClass());
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
		Persistable persisted = null;
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
						addActionError(getText("try.again.later"));
						return false;
					}
				}
				persisted = entityManager.get(_entity.getId());
				if (persisted != null) {
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
				persisted = entityManager.findOne(caseInsensitive, args);
				entityManager.evict(persisted);
				if (persisted != null) {
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
				for (Map.Entry<String, UiConfigImpl> entry : uiConfigs.entrySet())
					if (entry.getValue().isUnique() && StringUtils.isNotBlank(
							ServletActionContext.getRequest().getParameter(getEntityName() + '.' + entry.getKey()))) {
						persisted = entityManager.findOne(entry.getKey(),
								(Serializable) bw.getPropertyValue(entry.getKey()));
						if (persisted != null) {
							addFieldError(getEntityName() + '.' + entry.getKey(), getText("validation.already.exists"));
							return false;
						}
					}
			}
			try {
				Persistable temp = _entity;
				_entity = getEntityClass().newInstance();
				bw = new BeanWrapperImpl(temp);
				bw.setConversionService(conversionService);
				BeanWrapperImpl bwp = new BeanWrapperImpl(_entity);
				bwp.setConversionService(conversionService);
				Set<String> editedPropertyNames = new HashSet<>();
				for (String propertyName : ServletActionContext.getRequest().getParameterMap().keySet()) {
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
					if (uiConfig == null || uiConfig.getReadonly().isValue()
							|| fromList && uiConfig.getHiddenInList().isValue()
							|| !fromList && uiConfig.getHiddenInInput().isValue() || Persistable.class
									.isAssignableFrom(bwp.getPropertyDescriptor(propertyName).getPropertyType()))
						continue;
					if (StringUtils.isNotBlank(uiConfig.getReadonly().getExpression()) && evalBoolean(
							uiConfig.getReadonly().getExpression(), _entity, bwp.getPropertyValue(propertyName)))
						continue;
					if (fromList) {
						if (StringUtils.isNotBlank(uiConfig.getHiddenInList().getExpression())
								&& evalBoolean(uiConfig.getHiddenInList().getExpression(), _entity,
										bwp.getPropertyValue(propertyName)))
							continue;
					} else {
						if (StringUtils.isNotBlank(uiConfig.getHiddenInInput().getExpression())
								&& evalBoolean(uiConfig.getHiddenInInput().getExpression(), _entity,
										bwp.getPropertyValue(propertyName)))
							continue;
					}
					editedPropertyNames.add(propertyName);
					if (idAssigned && isnew)
						editedPropertyNames.add("id");
					if (isAttachmentable())
						editedPropertyNames.add("attachments");
				}
				for (String name : editedPropertyNames)
					bwp.setPropertyValue(name, bw.getPropertyValue(name));
				bw = bwp;
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		} else {
			if (naturalIdMutable && naturalIds.size() > 0) {
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
				persisted = entityManager.findOne(caseInsensitive, args);
				entityManager.evict(persisted);
				if (persisted != null && !persisted.getId().equals(_entity.getId())) {
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

				for (Map.Entry<String, UiConfigImpl> entry : uiConfigs.entrySet())
					if (entry.getValue().isUnique() && StringUtils.isNotBlank(
							ServletActionContext.getRequest().getParameter(getEntityName() + '.' + entry.getKey()))) {
						persisted = entityManager.findOne(entry.getKey(),
								(Serializable) bw.getPropertyValue(entry.getKey()));
						entityManager.evict(persisted);
						if (persisted != null && !persisted.getId().equals(_entity.getId())) {
							addFieldError(getEntityName() + '.' + entry.getKey(), getText("validation.already.exists"));
							return false;
						}
					}

				if (persisted != null && !persisted.getId().equals(_entity.getId())) {
					persisted = null;
				}
			}
			try {
				if (persisted == null) {
					persisted = entityManager.get((Serializable) bw.getPropertyValue("id"));
					if (persisted == null) {
						addActionError(getText("try.again.later"));
						return false;
					}
					entityManager.evict(persisted);
				}
				BeanWrapperImpl bwp = new BeanWrapperImpl(persisted);
				bwp.setConversionService(conversionService);
				String versionPropertyName = getVersionPropertyName();
				if (versionPropertyName != null) {
					int versionInDb = (Integer) bwp.getPropertyValue(versionPropertyName);
					int versionInUi = (Integer) bw.getPropertyValue(versionPropertyName);
					if (versionInUi > -1 && versionInUi < versionInDb) {
						addActionError(getText("validation.version.conflict"));
						return false;
					}
				}

				Set<String> editedPropertyNames = new HashSet<>();
				for (String propertyName : ServletActionContext.getRequest().getParameterMap().keySet()) {
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
						editedPropertyNames.add(propertyName);
						continue;
					}
					UiConfigImpl uiConfig = uiConfigs.get(propertyName);
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
						if (StringUtils.isNotBlank(uiConfig.getHiddenInList().getExpression())
								&& evalBoolean(uiConfig.getHiddenInList().getExpression(), _entity,
										bwp.getPropertyValue(propertyName)))
							continue;
					} else {
						if (StringUtils.isNotBlank(uiConfig.getHiddenInInput().getExpression())
								&& evalBoolean(uiConfig.getHiddenInInput().getExpression(), _entity,
										bwp.getPropertyValue(propertyName)))
							continue;
					}
					editedPropertyNames.add(propertyName);
				}
				if (isAttachmentable())
					editedPropertyNames.add("attachments");
				for (String name : editedPropertyNames)
					bwp.setPropertyValue(name, bw.getPropertyValue(name));
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
				entityManager.evict(sibling);
				if (!treeEntity.isNew() && sibling.getId().equals(treeEntity.getId()))
					continue;
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
						|| !naturalIdMutable && naturalIds.keySet().contains(propertyName) && !isnew
						|| !Persistable.class.isAssignableFrom(type))
					continue;
				if (StringUtils.isNotBlank(uiConfig.getReadonly().getExpression()) && evalBoolean(
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
					BeanWrapperImpl temp = new BeanWrapperImpl(type.newInstance());
					temp.setConversionService(conversionService);
					temp.setPropertyValue(listKey, parameterValue);
					BaseManager em = getEntityManager(type);
					Persistable obj;
					if (listKey.equals(UiConfig.DEFAULT_LIST_KEY))
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

	private boolean checkEntityReadonly(String expression, Persistable<?> entity) throws Exception {
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

	private boolean evalBoolean(String expression, Persistable<?> entity, Object value) {
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
		return new Tuple<>(owner, type);
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
		if (bwi.getPropertyValue("name") != null)
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
		BaseManager<Persistable<?>> entityManager = getEntityManager(getEntityClass());
		String[] arr = getId();
		Serializable[] id = (arr != null) ? new Serializable[arr.length] : new Serializable[0];
		try {
			BeanWrapperImpl bw = new BeanWrapperImpl(getEntityClass().newInstance());
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
					Persistable<?> en = entityManager.get(uid);
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
				List<Persistable<?>> list = entityManager.delete(id);
				if (list.size() > 0)
					addActionMessage(getText("delete.success"));
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
		final Map<String, Template> csvTemplates = new HashMap<>();
		if (StringUtils.isNotBlank(columns))
			columnsList = Arrays.asList(columns.split(","));
		for (Map.Entry<String, UiConfigImpl> entry : getUiConfigs().entrySet()) {
			String name = entry.getKey();
			if (columnsList != null && !columnsList.contains(name))
				continue;
			UiConfigImpl uc = entry.getValue();
			HiddenImpl hidden = uc.getHiddenInList();
			if (hidden.isValue())
				continue;
			exportColumnsList.add(name);
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
					Object value = bw.getPropertyValue(exportColumnsList.get(i));
					String text;
					Template csvTemplate = csvTemplates.get(exportColumnsList.get(i));
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
		BaseManager<Persistable<?>> em = getEntityManager(getEntityClass());
		String[] arr = getId();
		Serializable[] id = (arr != null) ? new Serializable[arr.length] : new Serializable[0];
		try {
			BeanWrapperImpl bw = new BeanWrapperImpl(getEntityClass().newInstance());
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
				if (StringUtils.isNotBlank(expression) && checkEntityReadonly(expression, (Persistable<?>) en))
					continue;
				en.setEnabled(enabled);
				em.save((Persistable) en);
			}
			addActionMessage(getText("operate.success"));
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
		addActionMessage(getText("operate.success"));
		return SUCCESS;
	}

	@JsonConfig(root = "children")
	@Authorize(ifAnyGranted = UserRole.ROLE_BUILTIN_USER)
	public String children() {
		if (!isTreeable())
			return NOTFOUND;
		BaseTreeableEntity parentEntity;
		BaseTreeControl baseTreeControl = null;
		Collection<BaseTreeControl> baseTreeControls = ApplicationContextUtils.getBeansOfType(BaseTreeControl.class)
				.values();
		for (BaseTreeControl btc : baseTreeControls)
			if (btc.getTree().getClass().equals(getEntityClass())) {
				baseTreeControl = btc;
				break;
			}
		if (baseTreeControl != null) {
			if (parent == null || parent < 1) {
				if (tree != null && tree > 0) {
					children = new ArrayList<>();
					children.add(baseTreeControl.getTree().getDescendantOrSelfById(tree));
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
					children.add(entityManager.get(tree));
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

	private Collection<Persistable> children;

	public Collection<Persistable> getChildren() {
		return children;
	}

	@JsonConfig(root = "suggestions")
	@Authorize(ifAnyGranted = UserRole.ROLE_BUILTIN_USER)
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
			Query q = session.createQuery(hql.toString());
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
			return c.getAnnotation(Authorize.class);
		}
		return authorize;
	}

	// need call once before view
	protected Class<Persistable<?>> getEntityClass() {
		if (entityClass == null)
			entityClass = (Class<Persistable<?>>) ReflectionUtils.getGenericClass(getClass());
		if (entityClass == null) {
			ActionProxy proxy = ActionContext.getContext().getActionInvocation().getProxy();
			String actionName = getEntityName();
			String namespace = proxy.getNamespace();
			entityClass = (Class<Persistable<?>>) AutoConfigPackageProvider.getEntityClass(namespace, actionName);
		}
		return entityClass;
	}

	private Class<Persistable<?>> entityClass;

	private void putEntityToValueStack(Persistable entity) {
		ValueStack vs = ActionContext.getContext().getValueStack();
		if (entity != null)
			vs.set(getEntityName(), entity);
	}

	protected Persistable constructEntity() {
		Persistable entity = null;
		try {
			entity = getEntityClass().newInstance();
			ValueStack temp = valueStackFactory.createValueStack();
			temp.set(getEntityName(), entity);
			Map<String, Object> context = temp.getContext();
			Map<String, Object> parameters = ActionContext.getContext().getParameters();
			try {
				ReflectionContextState.setCreatingNullObjects(context, true);
				ReflectionContextState.setDenyMethodExecution(context, true);
				for (Map.Entry<String, Object> entry : parameters.entrySet()) {
					String name = entry.getKey();
					String[] value = (String[]) entry.getValue();
					if (name.startsWith(getEntityName() + ".")) {
						if (name.split("\\.").length > 2) {
							if (value.length == 1 && StringUtils.isEmpty(value[0])) {
								value = null;
							}
						}
						temp.setParameter(name, value);
					}
				}
			} finally {
				ReflectionContextState.setCreatingNullObjects(context, false);
				ReflectionContextState.setDenyMethodExecution(context, false);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return entity;
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
	private transient ValueStackFactory valueStackFactory;

	@Inject
	private transient FreemarkerManager freemarkerManager;

}
package org.ironrhino.core.search.elasticsearch;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.search.SearchHit;
import org.hibernate.Session;
import org.ironrhino.core.metadata.NotInJson;
import org.ironrhino.core.metadata.Trigger;
import org.ironrhino.core.model.Persistable;
import org.ironrhino.core.search.elasticsearch.annotations.Index;
import org.ironrhino.core.search.elasticsearch.annotations.Searchable;
import org.ironrhino.core.search.elasticsearch.annotations.SearchableComponent;
import org.ironrhino.core.search.elasticsearch.annotations.SearchableId;
import org.ironrhino.core.search.elasticsearch.annotations.SearchableProperty;
import org.ironrhino.core.search.elasticsearch.annotations.Store;
import org.ironrhino.core.service.BaseManager.IterateCallback;
import org.ironrhino.core.service.EntityManager;
import org.ironrhino.core.util.AnnotationUtils;
import org.ironrhino.core.util.ClassScaner;
import org.ironrhino.core.util.DateUtils;
import org.ironrhino.core.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;

@SuppressWarnings(value = { "unchecked", "rawtypes" })
public class IndexManagerImpl implements IndexManager {

	private Logger logger = LoggerFactory.getLogger(getClass());

	@Value("${indexManager.indexName:ironrhino}")
	private String indexName;

	private Map<String, Class> typeClassMapping;

	private Map<Class, Map<String, Object>> schemaMapping;

	@Autowired
	private Client client;

	@Resource
	private EntityManager entityManager;

	private ObjectMapper objectMapper;

	@Override
	public String getIndexName() {
		return indexName;
	}

	public void setIndexName(String indexName) {
		this.indexName = indexName;
	}

	@PostConstruct
	public void init() {
		objectMapper = JsonUtils.createNewObjectMapper();
		objectMapper
				.setDateFormat(new SimpleDateFormat(DateUtils.DATETIME_ISO));
		objectMapper
				.setAnnotationIntrospector(new JacksonAnnotationIntrospector() {

					private static final long serialVersionUID = -2795053276465297328L;

					@Override
					protected boolean _isIgnorable(Annotated a) {
						if (a instanceof SearchableId
								|| a instanceof SearchableProperty
								|| a instanceof SearchableComponent)
							return false;
						boolean b = super._isIgnorable(a);
						if (b)
							return b;
						NotInJson ann = a.getAnnotation(NotInJson.class);
						return ann != null;
					}

				});
		Set<Class<?>> set = ClassScaner.scanAnnotated(
				ClassScaner.getAppPackages(), Searchable.class);
		typeClassMapping = new HashMap<String, Class>(set.size());
		schemaMapping = new HashMap<Class, Map<String, Object>>(set.size());
		for (Class c : set) {
			Searchable searchable = (Searchable) c
					.getAnnotation(Searchable.class);
			if (!searchable.root() || c.getSimpleName().contains("$"))
				continue;
			typeClassMapping.put(classToType(c), c);
			schemaMapping.put(c, getSchemaMapping(c, false));
		}
		initialize();
		if (client instanceof NodeClient) {
			NodeClient nc = (NodeClient) client;
			if ("memory".equals(nc.settings().get("index.store.type")))
				new Thread(new Runnable() {
					@Override
					public void run() {
						rebuild();
					}
				}).start();
		}
	}

	private static Map<String, Object> getSchemaMapping(Class c,
			boolean component) {
		Map<String, Object> mapping = new HashMap<String, Object>();
		Map<String, Object> properties = new HashMap<String, Object>();
		if (component)
			mapping.put("type", "object");
		mapping.put("properties", properties);
		BeanWrapperImpl bw = new BeanWrapperImpl(c);
		PropertyDescriptor[] pds = bw.getPropertyDescriptors();
		for (PropertyDescriptor pd : pds) {
			String name = pd.getName();
			// if (name.equals("path"))
			// continue;
			Method m = pd.getReadMethod();
			Class propertyType = pd.getPropertyType();
			if (propertyType == null)
				continue;
			Class componentType = propertyType;
			if (propertyType.isArray()) {
				componentType = propertyType.getComponentType();
				if (componentType.isInterface())
					continue;
			} else if (Collection.class.isAssignableFrom(propertyType)) {
				Type type = m.getGenericReturnType();
				if (type instanceof ParameterizedType) {
					ParameterizedType ptype = (ParameterizedType) type;
					Type temp = ptype.getActualTypeArguments()[0];
					if (temp instanceof Class)
						componentType = (Class) temp;
				}
				if (componentType.isInterface())
					continue;
			}

			SearchableId searchableId = null;
			SearchableProperty searchableProperty = null;
			SearchableComponent searchableComponent = null;
			if (m != null) {
				searchableId = m.getAnnotation(SearchableId.class);
				searchableProperty = m.getAnnotation(SearchableProperty.class);
				searchableComponent = m
						.getAnnotation(SearchableComponent.class);
			}
			try {
				Field f = pd.getReadMethod().getDeclaringClass()
						.getDeclaredField(name);
				if (f != null) {
					if (searchableId == null)
						searchableId = f.getAnnotation(SearchableId.class);
					if (searchableProperty == null)
						searchableProperty = f
								.getAnnotation(SearchableProperty.class);
					if (searchableComponent == null)
						searchableComponent = f
								.getAnnotation(SearchableComponent.class);
				}
			} catch (Exception e) {
			}
			if (searchableId != null) {
				properties.put(name, new PropertyMapping(componentType,
						searchableId));
			} else if (searchableProperty != null) {
				properties.put(name, new PropertyMapping(componentType,
						searchableProperty));
			} else if (searchableComponent != null) {
				properties.put(name, getSchemaMapping(componentType, true));
			}
		}
		return mapping;
	}

	public static class PropertyMapping {
		private String type = "string";
		private String index_name;
		private String format;
		private Float boost;
		private String index;
		private String store;
		private String analyzer;
		private String index_analyzer;
		private String search_analyzer;
		private Boolean include_in_all;
		private String null_value;
		private String term_vector;
		private Boolean omit_norms;
		private Boolean omit_term_freq_and_positions;
		private Boolean ignore_malformed;

		public PropertyMapping() {

		}

		public PropertyMapping(Class propertyClass,
				SearchableId searchableProperty) {
			this.type = searchableProperty.type();
			if (StringUtils.isBlank(type)) {
				if (propertyClass.isPrimitive())
					this.type = propertyClass.toString();
				else if (propertyClass.isEnum())
					this.type = "string";
				else
					this.type = propertyClass.getSimpleName().toLowerCase();
			}
			this.type = translateType(this.type);
			if (StringUtils.isNotBlank(searchableProperty.index_name()))
				this.index_name = searchableProperty.index_name();
			if (StringUtils.isNotBlank(searchableProperty.format()))
				this.format = searchableProperty.format();
			Index index = searchableProperty.index();
			if (index == Index.NO || index == Index.ANALYZED
					|| index == Index.NOT_ANALYZED)
				this.index = index.name().toLowerCase();
			if ("string".equals(this.type)
					&& searchableProperty.index() != Index.NOT_ANALYZED
					&& searchableProperty.index() != Index.NO
					&& !searchableProperty.omit_norms()
					&& searchableProperty.boost() != 1.0f)
				this.boost = searchableProperty.boost();
			Store store = searchableProperty.store();
			if (store != Store.NA)
				this.store = store.name().toLowerCase();
			if (StringUtils.isNotBlank(searchableProperty.analyzer()))
				this.analyzer = searchableProperty.analyzer();
			if (StringUtils.isNotBlank(searchableProperty.index_analyzer()))
				this.index_analyzer = searchableProperty.index_analyzer();
			if (StringUtils.isNotBlank(searchableProperty.search_analyzer()))
				this.search_analyzer = searchableProperty.search_analyzer();
			if (!searchableProperty.include_in_all())
				this.include_in_all = false;
			if (StringUtils.isNotBlank(searchableProperty.null_value()))
				this.null_value = searchableProperty.null_value();
			if (searchableProperty.omit_norms())
				this.omit_norms = searchableProperty.omit_norms();
			if (searchableProperty.omit_term_freq_and_positions())
				this.omit_term_freq_and_positions = searchableProperty
						.omit_term_freq_and_positions();
			if ("date".equals(this.type) || StringUtils.isNotBlank(this.format))
				this.ignore_malformed = searchableProperty.ignore_malformed();

		}

		public PropertyMapping(Class propertyClass,
				SearchableProperty searchableProperty) {
			this.type = searchableProperty.type();
			if (StringUtils.isBlank(type)) {
				if (propertyClass.isPrimitive())
					this.type = propertyClass.toString();
				else if (propertyClass.isEnum())
					this.type = "string";
				else
					this.type = propertyClass.getSimpleName().toLowerCase();
			}
			this.type = translateType(this.type);
			if (StringUtils.isNotBlank(searchableProperty.index_name()))
				this.index_name = searchableProperty.index_name();
			if (StringUtils.isNotBlank(searchableProperty.format()))
				this.format = searchableProperty.format();
			Index index = searchableProperty.index();
			if (index == Index.NO || index == Index.ANALYZED
					|| index == Index.NOT_ANALYZED)
				this.index = index.name().toLowerCase();
			if ("string".equals(this.type)
					&& searchableProperty.index() != Index.NOT_ANALYZED
					&& searchableProperty.index() != Index.NO
					&& !searchableProperty.omit_norms()
					&& searchableProperty.boost() != 1.0f)
				this.boost = searchableProperty.boost();
			Store store = searchableProperty.store();
			if (store != Store.NA)
				this.store = store.name().toLowerCase();
			if (StringUtils.isNotBlank(searchableProperty.analyzer()))
				this.analyzer = searchableProperty.analyzer();
			if (StringUtils.isNotBlank(searchableProperty.index_analyzer()))
				this.index_analyzer = searchableProperty.index_analyzer();
			if (StringUtils.isNotBlank(searchableProperty.search_analyzer()))
				this.search_analyzer = searchableProperty.search_analyzer();
			if (!searchableProperty.include_in_all())
				this.include_in_all = false;
			if (StringUtils.isNotBlank(searchableProperty.null_value()))
				this.null_value = searchableProperty.null_value();
			if (searchableProperty.omit_norms())
				this.omit_norms = searchableProperty.omit_norms();
			if (searchableProperty.omit_term_freq_and_positions())
				this.omit_term_freq_and_positions = searchableProperty
						.omit_term_freq_and_positions();
			if ("date".equals(this.type) || StringUtils.isNotBlank(this.format))
				this.ignore_malformed = searchableProperty.ignore_malformed();
		}

		private static String translateType(String input) {
			if (input.equals("int"))
				return "integer";
			if (input.equals("bigdecimal"))
				return "double";
			return input;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public String getFormat() {
			return format;
		}

		public void setFormat(String format) {
			this.format = format;
		}

		public Float getBoost() {
			return boost;
		}

		public void setBoost(Float boost) {
			this.boost = boost;
		}

		public String getIndex() {
			return index;
		}

		public void setIndex(String index) {
			this.index = index;
		}

		public String getStore() {
			return store;
		}

		public void setStore(String store) {
			this.store = store;
		}

		public String getAnalyzer() {
			return analyzer;
		}

		public void setAnalyzer(String analyzer) {
			this.analyzer = analyzer;
		}

		public Boolean getInclude_in_all() {
			return include_in_all;
		}

		public void setInclude_in_all(Boolean include_in_all) {
			this.include_in_all = include_in_all;
		}

		public String getIndex_name() {
			return index_name;
		}

		public void setIndex_name(String index_name) {
			this.index_name = index_name;
		}

		public String getIndex_analyzer() {
			return index_analyzer;
		}

		public void setIndex_analyzer(String index_analyzer) {
			this.index_analyzer = index_analyzer;
		}

		public String getSearch_analyzer() {
			return search_analyzer;
		}

		public void setSearch_analyzer(String search_analyzer) {
			this.search_analyzer = search_analyzer;
		}

		public String getNull_value() {
			return null_value;
		}

		public void setNull_value(String null_value) {
			this.null_value = null_value;
		}

		public String getTerm_vector() {
			return term_vector;
		}

		public void setTerm_vector(String term_vector) {
			this.term_vector = term_vector;
		}

		public Boolean getOmit_norms() {
			return omit_norms;
		}

		public void setOmit_norms(Boolean omit_norms) {
			this.omit_norms = omit_norms;
		}

		public Boolean getOmit_term_freq_and_positions() {
			return omit_term_freq_and_positions;
		}

		public void setOmit_term_freq_and_positions(
				Boolean omit_term_freq_and_positions) {
			this.omit_term_freq_and_positions = omit_term_freq_and_positions;
		}

		public Boolean getIgnore_malformed() {
			return ignore_malformed;
		}

		public void setIgnore_malformed(Boolean ignore_malformed) {
			this.ignore_malformed = ignore_malformed;
		}

	}

	private String entityToDocument(Persistable entity) {
		Map<String, Object> map = AnnotationUtils
				.getAnnotatedPropertyNameAndValues(entity, SearchableId.class,
						SearchableProperty.class, SearchableComponent.class);
		Iterator<Map.Entry<String, Object>> it = map.entrySet().iterator();
		while (it.hasNext()) {
			Object value = it.next().getValue();
			if (value == null || value instanceof String
					&& StringUtils.isBlank((String) value)
					|| value instanceof Collection
					&& ((Collection) value).isEmpty()
					|| value.getClass().isArray()
					&& ((Object[]) value).length == 0)
				it.remove();
		}
		if (map.isEmpty())
			logger.warn("{} is empty", entity);
		try {
			return objectMapper.writeValueAsString(map);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return null;
		}
	}

	private static String classToType(Class clazz) {
		String type = StringUtils.uncapitalize(clazz.getSimpleName());
		Searchable s = (Searchable) clazz.getAnnotation(Searchable.class);
		if (s != null && StringUtils.isNotBlank(s.type()))
			type = s.type();
		return type;
	}

	private Class typeToClass(String type) {
		return typeClassMapping.get(type);
	}

	@Override
	public Object searchHitToEntity(SearchHit sh) throws Exception {
		return objectMapper.readValue(sh.sourceAsString(),
				typeToClass(sh.getType()));
	}

	@Override
	public ListenableActionFuture<IndexResponse> index(Persistable entity) {
		return client
				.prepareIndex(getIndexName(), classToType(entity.getClass()),
						String.valueOf(entity.getId()))
				.setSource(entityToDocument(entity)).execute();
	}

	@Override
	public ListenableActionFuture<DeleteResponse> delete(Persistable entity) {
		return client.prepareDelete(getIndexName(),
				classToType(entity.getClass()), String.valueOf(entity.getId()))
				.execute();
	}

	private void initialize() {
		IndicesAdminClient adminClient = client.admin().indices();
		try {
			IndicesExistsResponse ies = adminClient.exists(
					new IndicesExistsRequest(getIndexName())).get();
			if (!ies.isExists())
				adminClient.create(new CreateIndexRequest(getIndexName()))
						.get();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		for (Map.Entry<Class, Map<String, Object>> entry : schemaMapping
				.entrySet()) {
			HashMap<String, Map<String, Object>> map = new HashMap<String, Map<String, Object>>();
			map.put(classToType(entry.getKey()), entry.getValue());
			String mapping = JsonUtils.toJson(map);
			if (logger.isDebugEnabled())
				logger.debug("Mapping {} : {}", entry.getKey(), mapping);
			try {
				adminClient.preparePutMapping(getIndexName())
						.setType(classToType(entry.getKey()))
						.setSource(mapping).execute().get();
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
	}

	@Override
	@Trigger
	public void rebuild() {
		IndicesAdminClient adminClient = client.admin().indices();
		try {
			IndicesExistsResponse ies = adminClient.exists(
					new IndicesExistsRequest(getIndexName())).get();
			if (ies.isExists())
				adminClient.delete(new DeleteIndexRequest(getIndexName()))
						.get();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		initialize();
		for (Class c : schemaMapping.keySet())
			indexAll(classToType(c));
		logger.info("rebuild completed");
	}

	@Override
	public void indexAll(String type) {
		Class clz = typeToClass(type);
		entityManager.setEntityClass(clz);
		final AtomicLong indexed = new AtomicLong();
		entityManager.iterate(20, new IterateCallback() {
			@Override
			public void process(Object[] entityArray, Session session) {
				BulkRequestBuilder bulkRequest = client.prepareBulk();
				indexed.addAndGet(entityArray.length);
				for (Object obj : entityArray) {
					Persistable p = (Persistable) obj;
					bulkRequest.add(client.prepareIndex(getIndexName(),
							classToType(p.getClass()),
							String.valueOf(p.getId())).setSource(
							entityToDocument(p)));
				}
				try {
					if (bulkRequest.numberOfActions() > 0) {
						ListenableActionFuture<BulkResponse> br = bulkRequest
								.execute();
						br.addListener(bulkResponseActionListener);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		logger.info("indexed {} for {}", indexed.get(), type);
	}

	private ActionListener<BulkResponse> bulkResponseActionListener = new ActionListener<BulkResponse>() {
		@Override
		public void onResponse(BulkResponse br) {
			if (br.hasFailures())
				logger.error(br.buildFailureMessage());
		}

		@Override
		public void onFailure(Throwable e) {
			logger.error(e.getMessage(), e);
		}
	};

}

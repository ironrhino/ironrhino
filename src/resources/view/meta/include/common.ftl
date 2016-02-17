<#macro stageConditional value negated=false>
	<#if statics['org.ironrhino.core.spring.configuration.StageCondition'].matches(value,negated)>
		<#nested>
	</#if>
</#macro>

<#macro runLevelConditional value negated=false>
	<#if statics['org.ironrhino.core.spring.configuration.RunLevelCondition'].matches(value,negated)>
		<#nested>
	</#if>
</#macro>

<#macro classPresentConditional value negated=false>
	<#if !value?is_indexable>
		<#local value=[value]/>
	</#if>
	<#if statics['org.ironrhino.core.spring.configuration.ClassPresentCondition'].matches(value,negated)>
		<#nested>
	</#if>
</#macro>

<#macro resourcePresentConditional value negated=false>
	<#if !value?is_indexable>
		<#local value=[value]/>
	</#if>
	<#if statics['org.ironrhino.core.spring.configuration.ResourcePresentCondition'].matches(value,negated)>
		<#nested>
	</#if>
</#macro>

<#function authentication property>
  <#return statics['org.ironrhino.core.util.AuthzUtils'].authentication(property)>
</#function>

<#function hasPermission secured>
  <#return statics['org.ironrhino.core.util.AuthzUtils'].hasPermission(secured)>
</#function>

<#function hasRole role>
  <#return statics['org.ironrhino.core.util.AuthzUtils'].hasRole(role)>
</#function>

<#macro authorize ifAllGranted="" ifAnyGranted="" ifNotGranted="" authorizer="" resource="">
	<#if !ifAllGranted?is_indexable>
		<#if ifAllGranted?has_content>
			<#local ifAllGranted=[ifAllGranted]/>
		<#else>
			<#local ifAllGranted=[]/>
		</#if>
	</#if>
	<#if !ifAnyGranted?is_indexable>
		<#if ifAnyGranted?has_content>
			<#local ifAnyGranted=[ifAnyGranted]/>
		<#else>
			<#local ifAnyGranted=[]/>
		</#if>
	</#if>
	<#if !ifNotGranted?is_indexable>
		<#if ifNotGranted?has_content>
			<#local ifNotGranted=[ifNotGranted]/>
		<#else>
			<#local ifNotGranted=[]/>
		</#if>
	</#if>
	<#if statics['org.ironrhino.core.util.AuthzUtils'].authorizeArray(ifAllGranted,ifAnyGranted,ifNotGranted) || (authorizer?has_content && resource?has_content && beans['dynamicAuthorizerManager'].authorize(authorizer,authentication("principal"),resource))>
		<#nested>
	</#if>
</#macro>

<#macro cache key scope="application" timeToIdle="-1" timeToLive="3600">
<#local keyExists=statics['org.ironrhino.core.cache.CacheContext'].eval(key)??>
<#local content=statics['org.ironrhino.core.cache.CacheContext'].getPageFragment(key,scope)!>
<#if keyExists&&content??&&content?length gt 0>${content}<#else>
<#local content><#nested/></#local>  
${content}
${statics['org.ironrhino.core.cache.CacheContext'].putPageFragment(key,content,scope,timeToIdle,timeToLive)}
</#if>
</#macro>

<#macro captcha theme="">
<#if captchaRequired!>
	<@s.textfield label="%{getText('captcha')}" name="captcha" class="required captcha" style="width:60px;" data\-captcha="${base}/captcha.jpg?token=${session.id}"/>
</#if>
</#macro>

<#function getUrl value includeContextPath=true includeQueryString=false secure="">
<#if value?starts_with('/assets/') && includeContextPath>
	<#if assetsBase??>
		<#local value=assetsBase+value>
	<#else>
		<#local value=base+value>
	</#if>
	<#return value>
<#elseif value?starts_with('/')>
	<#if request??>
		<#if !request.isSecure() && secure=="true">
			<#local value=statics['org.ironrhino.core.util.RequestUtils'].getBaseUrl(request,true,includeContextPath)+value>
		<#elseif request.isSecure() && secure=="false">
			<#local value=statics['org.ironrhino.core.util.RequestUtils'].getBaseUrl(request,false,includeContextPath)+value>
		<#elseif includeContextPath>
			<#local value=base+value>
		</#if>
	<#else>
		<#if value?starts_with('http://') && secure=="true">
			<#local value=value?replace('http://','https://')?replace('8080','8443')>
		<#elseif value?starts_with('https://') && secure=="false">
			<#local value=value?replace('https://','http://')?replace('8443','8080')>
		</#if>
	</#if>
</#if>
<#local value=response.encodeURL(value)/>
<#if includeQueryString && request?? && request.queryString?has_content && !request.queryString?matches('^_=\\d+$')>
	<#local value+=(value?index_of('?') gt 0)?then('&','?')/>
	<#if request.queryString?index_of('&_=') gt 0>
		<#local value+=request.queryString?keep_before('&_=')/>
	<#else>
		<#local value+=request.queryString/>
	</#if>
</#if>
<#return value>
</#function>

<#macro url value includeContextPath=true includeQueryString=false secure="">
${getUrl(value,includeContextPath,includeQueryString,secure)}<#t>
</#macro>
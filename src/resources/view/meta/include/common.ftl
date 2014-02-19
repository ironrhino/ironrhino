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
	<#if statics['org.ironrhino.core.spring.configuration.ClassPresentCondition'].matches(value,negated)>
		<#nested>
	</#if>
</#macro>

<#macro resourcePresentConditional value negated=false>
	<#if statics['org.ironrhino.core.spring.configuration.ResourcePresentCondition'].matches(value,negated)>
		<#nested>
	</#if>
</#macro>

<#function authentication property>
  <#return statics['org.ironrhino.core.util.AuthzUtils'].authentication(property)>
</#function>

<#macro authorize ifAllGranted="" ifAnyGranted="" ifNotGranted="" authorizer="" resource="">
	<#if statics['org.ironrhino.core.util.AuthzUtils'].authorize(ifAllGranted,ifAnyGranted,ifNotGranted) || (authorizer!="" &&  statics['org.ironrhino.core.util.ApplicationContextUtils'].getBean('dynamicAuthorizerManager').authorize(authorizer,authentication("principal"),resource))>
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
	<@s.textfield label="%{getText('captcha')}" name="captcha" cssClass="required captcha input-small" id="${base}/captcha.jpg?token=${session.id}"/>
</#if>
</#macro>

<#function getUrl value includeContextPath=true includeQueryString=false secure=false>
<#if value?starts_with('/assets/') && includeContextPath>
	<#if assetsBase??>
		<#local value=assetsBase+value>
	<#else>
		<#local value=base+value>
	</#if>
	<#return value>
<#elseif value?starts_with('/')>
	<#if request??>
		<#if !request.isSecure() && secure>
			<#local value=statics['org.ironrhino.core.util.RequestUtils'].getBaseUrl(request,true,includeContextPath)+value>
		<#elseif request.isSecure() && !secure>
			<#local value=statics['org.ironrhino.core.util.RequestUtils'].getBaseUrl(request,false,includeContextPath)+value>
		<#elseif includeContextPath>
			<#local value=base+value>
		</#if>
	<#else>
		<#if value?starts_with('http://') && secure>
			<#local value=value?replace('http://','https://')?replace('8080','8443')>
		<#elseif value?starts_with('https://') && !secure>
			<#local value=value?replace('https://','http://')?replace('8443','8080')>
		</#if>
	</#if>
</#if>
<#local value=response.encodeURL(value)/>
<#if includeQueryString && request?? && request.queryString?has_content && !request.queryString?matches('^_=\\d+$')>
	<#local value=value+(value?index_of('?') gt 0)?string('&','?')/>
	<#if request.queryString?index_of('&_=') gt 0>
		<#local value=value+request.queryString?substring(0,request.queryString?index_of('&_='))/>
	<#else>
		<#local value=value+request.queryString/>
	</#if>
</#if>
<#return value>
</#function>

<#macro url value includeContextPath=true includeQueryString=false secure=false>
${getUrl(value,includeContextPath,includeQueryString,secure)}<#t>
</#macro>
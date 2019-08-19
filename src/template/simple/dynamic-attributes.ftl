<#if parameters.dynamicAttributes?has_content><#rt/>
<#list parameters.dynamicAttributes as aKey,keyValue><#rt/>
<#if !aKey?starts_with('_internal_')>
  <#if keyValue?is_string>
	  <#assign value = struts.translateVariables(keyValue)!keyValue/>
  <#else>
	  <#assign value = keyValue?string/>
  </#if>
 ${aKey}="${value?html}"<#rt/>
</#if>
</#list><#rt/>
</#if><#rt/>
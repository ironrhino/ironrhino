<#if parameters.dynamicAttributes?has_content><#rt/>
<#assign aKeys = parameters.dynamicAttributes.keySet()><#rt/>
<#list aKeys as aKey><#rt/>
<#if !aKey?starts_with('_internal_')>
  <#assign keyValue = parameters.dynamicAttributes.get(aKey)/>
  <#if keyValue?is_string>
      <#assign value = struts.translateVariables(keyValue)!keyValue/>
  <#else>
      <#assign value = keyValue?string/>
  </#if>
 ${aKey}="${value?html}"<#rt/>
</#if>
</#list><#rt/>
</#if><#rt/>
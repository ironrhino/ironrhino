<#if parameters.dynamicAttributes?has_content><#rt/>
<#list parameters.dynamicAttributes as key,value><#rt/>
<#if !key?starts_with('_internal_')>
 ${key}="${value?html}"<#rt/>
</#if>
</#list><#rt/>
</#if><#rt/>
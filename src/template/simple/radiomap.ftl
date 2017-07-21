<@s.iterator value="parameters.list">
    <#if parameters.listKey?has_content>
        <#assign itemKey = stack.findValue(parameters.listKey)/>
    <#else>
        <#assign itemKey = stack.findValue('top')/>
    </#if>
    <#assign itemKeyStr = itemKey.toString() />
    <#if parameters.listValue?has_content>
        <#assign itemValue = stack.findString(parameters.listValue)/>
    <#else>
        <#assign itemValue = stack.findString('top')/>
    </#if>
<label for="${parameters.id?html}-${itemKeyStr?html}" class="radio inline"><#rt/>    
<input type="radio"<#rt/>
<#if parameters.name?has_content>
 name="${parameters.name?html}"<#rt/>
</#if>
 id="${parameters.id?html}-${itemKeyStr?html}"<#rt/>
<#if tag.contains(parameters.nameValue!'', itemKeyStr) || tag.contains(parameters.nameValue!'', itemValue)>
 checked="checked"<#rt/>
</#if>
<#if itemKey?has_content>
 value="${itemKeyStr?html}"<#rt/>
</#if>
<#if parameters.readonly!false>
 readonly="readonly"<#rt/>
</#if>
<#if parameters.disabled!false>
 disabled="disabled"<#rt/>
</#if>
<#include "/${parameters.templateDir}/${parameters.expandTheme}/css.ftl" />
<#include "/${parameters.templateDir}/${parameters.expandTheme}/dynamic-attributes.ftl" />
/>${itemValue}<#t/>
</label>
</@s.iterator>

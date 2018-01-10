<#assign itemCount = 0/>
<#if parameters.list?has_content>
<@s.iterator value="parameters.list">
    <#assign itemCount = itemCount + 1/>
    <#if parameters.listKey?has_content>
        <#assign itemKey = stack.findValue(parameters.listKey)/>
        <#else>
            <#assign itemKey = stack.findValue('top')/>
    </#if>
    <#if parameters.listValue?has_content>
        <#assign itemValue = stack.findString(parameters.listValue)!""/>
        <#else>
            <#assign itemValue = stack.findString('top')/>
    </#if>
    <#assign itemKeyStr=itemKey.toString() />
<label for="${parameters.id?html}-${itemCount}" class="checkbox inline"><#rt/>
<input type="checkbox" name="${parameters.name?html}" value="${itemKeyStr?html}"<#rt/>
 id="${parameters.id?html}-${itemCount}"<#rt/>
    <#if tag.contains(parameters.nameValue, itemKey) || tag.contains(parameters.nameValue, itemValue)>
 checked="checked"<#rt/>
    </#if>
    <#if parameters.readonly!false>
 readonly="readonly"<#rt/>
    </#if>
    <#if parameters.disabled!false>
 disabled="disabled"<#rt/>
    </#if>
    <#include "/${parameters.templateDir}/${parameters.expandTheme}/css.ftl" />
	<#include "/${parameters.templateDir}/${parameters.expandTheme}/dynamic-attributes.ftl" />
/>${itemValue?html}</label>
</@s.iterator>
</#if>
<#if !(parameters.disabled!false) && parameters.name?has_content>
<input type="hidden" name="__multiselect_${parameters.name?html}"/>
</#if>
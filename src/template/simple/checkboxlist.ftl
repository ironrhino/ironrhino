<#assign itemCount = 0/>
<@s.iterator value="parameters.list">
    <#assign itemCount = itemCount + 1/>
    <#assign itemKey = stack.findValue(parameters.listKey)/>
    <#assign itemKeyStr = itemKey?string />
    <#assign itemValue = stack.findString(parameters.listValue)!""/>
<label for="${parameters.name?html}-${itemCount}" class="checkbox inline"><#rt/>
<input type="checkbox"<#rt/>
 id="${parameters.name?html}-${itemCount}"<#rt/>
<#if parameters.name?has_content>
 name="${parameters.name?html}"<#rt/>
</#if>
<#if itemKey?has_content>
 value="${itemKeyStr?html}"<#rt/>
</#if>
<#if tag.contains(parameters.nameValue!"", itemKeyStr) || tag.contains(parameters.nameValue!"", itemValue)>
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
<#if !(parameters.disabled!false) && parameters.name?has_content>
<input type="hidden" name="__multiselect_${parameters.name?html}"/>
</#if>
<#setting number_format="#.#####">
<select<#rt/>
<#include "/${parameters.templateDir}/${parameters.expandTheme}/basic-attributes.ftl" />
<#if parameters.multiple!false>
 multiple="multiple"<#rt/>
</#if>
<#include "/${parameters.templateDir}/${parameters.expandTheme}/dynamic-attributes.ftl" />
>
<#if parameters.headerKey?? && parameters.headerValue??>
<option value="${parameters.headerKey?html}"<#if tag.contains(parameters.nameValue, parameters.headerKey) || tag.contains(parameters.nameValue, parameters.headerValue)> selected="selected"<#rt/></#if>>${parameters.headerValue?html}</option>
</#if>
<#if parameters.emptyOption!false>
<option value=""></option>
</#if>
<@s.iterator value="parameters.list">
		<#if stack.findValue(parameters.listKey)?has_content>
			<#assign itemKey = stack.findValue(parameters.listKey)/>
			<#assign itemKeyStr = stack.findString(parameters.listKey)/>
		<#else>
			<#assign itemKey = ''/>
			<#assign itemKeyStr = ''/>
		</#if>
		<#if stack.findString(parameters.listValue)?has_content>
			<#assign itemValue = stack.findString(parameters.listValue)/>
		<#else>
			<#assign itemValue = ''/>
		</#if>
<option value="${itemKeyStr?html}"<#if tag.contains(parameters.nameValue, itemKey) || tag.contains(parameters.nameValue, itemValue)> selected="selected"</#if>>${itemValue?html}</option>
</@s.iterator>
<#include "/${parameters.templateDir}/${parameters.expandTheme}/optgroup.ftl" />
</select>
<#if (parameters.multiple!false) && !(parameters.disabled!false) && parameters.name?has_content>
<input type="hidden" name="__multiselect_${parameters.name?html}"/>
</#if>

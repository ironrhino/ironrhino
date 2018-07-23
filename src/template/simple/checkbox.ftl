<input type="checkbox"<#rt/>
<#include "/${parameters.templateDir}/${parameters.expandTheme}/basic-attributes.ftl" />
 value="${parameters.fieldValue?html}"<#rt/>
<#if parameters.nameValue?has_content && (parameters.nameValue?is_boolean && parameters.nameValue || parameters.nameValue=='true')>
 checked="checked"<#rt/>
</#if>
<#include "/${parameters.templateDir}/${parameters.expandTheme}/dynamic-attributes.ftl" />
/>
<#if !(parameters.disabled!false) && parameters.name?has_content>
<input type="hidden" name="__checkbox_${parameters.name?html}"/>
</#if>

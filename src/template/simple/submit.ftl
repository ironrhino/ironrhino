<button type="submit"<#rt/>
<#include "/${parameters.templateDir}/${parameters.expandTheme}/basic-attributes.ftl" />
<#if parameters.nameValue?has_content>
 value="${parameters.nameValue?html}"<#rt/>
</#if>
<#include "/${parameters.templateDir}/${parameters.expandTheme}/dynamic-attributes.ftl" />
>
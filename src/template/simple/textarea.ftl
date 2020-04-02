<textarea<#rt/>
<#include "/${parameters.templateDir}/${parameters.expandTheme}/basic-attributes.ftl" />
<#include "/${parameters.templateDir}/${parameters.expandTheme}/dynamic-attributes.ftl" />
><#rt/>
<#if parameters.nameValue?has_content>
${parameters.nameValue?html}<#rt/>
</#if>
</textarea>

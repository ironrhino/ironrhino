<#if (parameters.type!"")=="button">
<button type="reset"<#rt/>
<#include "/${parameters.templateDir}/${parameters.expandTheme}/basic-attributes.ftl" />
<#include "/${parameters.templateDir}/${parameters.expandTheme}/dynamic-attributes.ftl" />
><#if parameters.src?has_content>
<img<#rt/>
<#if parameters.label?has_content>
 alt="${parameters.label?html}"<#rt/>
</#if>
<#if parameters.src?has_content>
 src="${parameters.src?html}"<#rt/>
</#if>
/><#else><#if parameters.label?has_content>${parameters.label?html}<#rt/></#if></#if></button>
<#else>
<input type="reset"<#rt/>
<#include "/${parameters.templateDir}/${parameters.expandTheme}/basic-attributes.ftl" />
<#include "/${parameters.templateDir}/${parameters.expandTheme}/dynamic-attributes.ftl" />
/>
</#if>
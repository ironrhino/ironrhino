<button type="submit"<#rt/>
<#if parameters.id?has_content>
 id="${parameters.id?html}"<#rt/>
</#if>
<#if parameters.name?has_content>
 name="${parameters.name?html}"<#rt/>
</#if>
<#if parameters.disabled!false>
 disabled="disabled"<#rt/>
</#if>
 class="btn<#if parameters.cssClass?has_content> ${parameters.cssClass?html}</#if>"<#rt/>
<#if parameters.cssStyle?has_content>
 style="${parameters.cssStyle?html}"<#rt/>
</#if>
<#if parameters.title?has_content>
 title="${parameters.title?html}"<#rt/>
</#if>
<#if parameters.tabindex?has_content>
 tabindex="${parameters.tabindex?html}"<#rt/>
</#if>
<#include "/${parameters.templateDir}/simple/scripting-events.ftl"/>
<#include "/${parameters.templateDir}/simple/common-attributes.ftl" />
<#include "/${parameters.templateDir}/simple/dynamic-attributes.ftl" />
>
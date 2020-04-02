<#include "/${parameters.templateDir}/${parameters.expandTheme}/form-common.ftl" />
>
<#if action.csrfRequired!false>
<input type="hidden" name="csrf" value="${action.csrf}"/><#rt/>
</#if>
<#include "/${parameters.templateDir}/simple/form-common.ftl" />>
<fieldset>
<#if action.csrfRequired!false>
<input type="hidden" name="csrf" value="${action.csrf}"/><#rt/>
</#if>
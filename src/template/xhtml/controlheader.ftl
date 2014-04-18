<div class="control-group<#if parameters.name?? && fieldErrors?? && fieldErrors[parameters.name]??> error</#if>"<#if parameters.dynamicAttributes['group']?has_content> data-group="${parameters.dynamicAttributes['group']}"</#if>>
<#if parameters.label??>
<label class="control-label"<#if parameters.id??> for="${parameters.id?html}"</#if>>${parameters.label?html}</label>
</#if>
<div class="controls">
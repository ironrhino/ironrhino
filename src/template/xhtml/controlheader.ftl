<div<#if parameters.id??> id="control-group-${parameters.id?html}"</#if> class="control-group<#if parameters.name?? && fieldErrors?? && fieldErrors[parameters.name]??> error</#if>"<#if parameters.dynamicAttributes['_internal_group']?has_content> data-group="${parameters.dynamicAttributes['_internal_group']}"</#if>>
<#if parameters.label??>
<label class="control-label"<#if parameters.id??> for="${parameters.id?html}"</#if>><#if parameters.dynamicAttributes['_internal_description']?has_content><span data-content="${parameters.dynamicAttributes['_internal_description']}" class="poped glyphicon glyphicon-question-sign"></span> </#if>${parameters.label?html}</label>
</#if>
<div class="controls">

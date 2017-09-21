<div<#if parameters.id??> id="control-group-${parameters.id?html}"</#if> class="control-group<#if parameters.name?? && fieldErrors?? && fieldErrors[parameters.name]??> error</#if>"<#if parameters.dynamicAttributes['_internal_group']?has_content> data-group="${parameters.dynamicAttributes['_internal_group']}"</#if>>
<#assign _label=''>
<#if parameters.label??>
<#assign _label=parameters.label>
<#elseif parameters.name?has_content>
<#assign _label=parameters.name>
<#if _label?index_of('.') gt 0><#assign _label=_label?keep_after_last('.')></#if>
<#assign _label=getText(_label)>
</#if>
<#if _label?has_content>
<label class="control-label"<#if parameters.id??> for="${parameters.id?html}"</#if>><#if parameters.dynamicAttributes['_internal_description']?has_content><span data-content="${parameters.dynamicAttributes['_internal_description']}" class="poped glyphicon glyphicon-question-sign"></span> </#if>${_label?html}</label>
</#if>
<div class="controls">

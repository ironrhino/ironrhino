<!DOCTYPE html>
<#escape x as x?html><html>
<head>
<#assign entityDisplayName=action.getText((richtableConfig.alias?has_content)?string(richtableConfig.alias!,entityName))/>
<title>${entityDisplayName}</title>
</head>
<body>
<div class="row<#if fluidLayout>-fluid</#if> reload-container">
	<div class="span2">
		<#assign parentNode=(entityName?eval)!/>
		<div class="treeview reloadable" data-url="${actionBaseUrl}/children<#if tree?? && tree gt 0>?tree=${tree}</#if>" data-head="${entityDisplayName}"<#if parentNode?has_content>data-value="<#if tree?? && tree gt 0>${parentNode.getFullname(tree)}<#else>${parentNode.fullname}</#if>" data-separator="${parentNode.fullnameSeperator}"</#if>>
		<template><a href="${actionBaseUrl}?view=treeview&parent={{id}}<#if tree??>&tree=${tree}</#if>" class="ajax view" data-replacement="${entityName}_list"><span>{{name}}</span></a></template>
		</div>
	</div>
	<div class="span10">
		<div id="${entityName}_list"></div>
	</div>
</div>
</body>
</html></#escape>

<!DOCTYPE html>
<#escape x as x?html><html>
<head>
<title>${action.getText('region')}</title>
</head>
<body>
<div class="row<#if fluidLayout>-fluid</#if> reload-container">
	<div class="span2">
		<div class="treeview reloadable" data-url="/region/children<#if tree?? && tree gt 0>?tree=${tree}</#if>" data-head="${action.getText('region')}"<#if region?has_content>data-value="<#if tree?? && tree gt 0>${region.getFullname(tree)}<#else>${region.fullname}</#if>" data-separator="${region.fullnameSeperator}"</#if>>
		<template><a href="<@url value="/common/region?view=treeview&parent={{id}}"/><#if tree??>&tree=${tree}</#if>" class="ajax view" data-replacement="region_list"><span>{{name}}</span></a></template>
		</div>
	</div>
	<div class="span10">
		<div id="region_list"></div>
	</div>
</div>
</body>
</html></#escape>

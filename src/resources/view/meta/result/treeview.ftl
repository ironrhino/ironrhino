<!DOCTYPE html>
<#escape x as x?html><html>
<head>
<#assign entityDisplayName=action.getText((richtableConfig.alias?has_content)?string(richtableConfig.alias!,entityName))/>
<title>${entityDisplayName}</title>
</head>
<body>
<div class="row reload-container">
	<div class="span2">
		<div class="treeview reloadable" data-url="${actionBaseUrl}/children" data-head="${entityDisplayName}">
		<template><a href="${actionBaseUrl}?view=treeview&parent={{id}}" class="ajax view" data-replacement="${entityName}_list"><span>{{name}}</span></a></template>
		</div>
	</div>
	<div class="span10">
		<div id="${entityName}_list"></div>
	</div>
</div>
</body>
</html></#escape>

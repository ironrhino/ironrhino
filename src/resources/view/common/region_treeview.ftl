<!DOCTYPE html>
<#escape x as x?html><html>
<head>
<title>${action.getText('region')}</title>
</head>
<body>
<div class="row">
	<div class="span2">
		<div class="treeview" data-url="/region/children" data-head="${action.getText('region')}">
		<template><a href="<@url value="/common/region?view=treeview&parent={{id}}"/>" class="ajax view" data-replacement="region_list"><span>{{name}}</span></a></template>
		</div>
	</div>
	<div class="span10">
		<div id="region_list"><div class="ajaxpanel" data-url="<@url value="/common/region?view=treeview"/>"></div></div>
	</div>
</div>
</body>
</html></#escape>

<!DOCTYPE html>
<#escape x as x?html><html>
<head>
<title>${action.getText(name)}</title>
</head>
<body>
<ul class="breadcrumb">
	<li>
    	<a href="<@url value="/"/>">${action.getText('index')}</a> <span class="divider">/</span>
	</li>
	<li class="active">${action.getText(name)}</li>
</ul>
<div class="container-fluid issue ${name}">
<#if page??>
<div>
	<h3 class="title" style="text-align:center;">${page.title!}</h3>
	<div class="date" style="text-align:center;">${page.createDate?date}</div>
	<div class="content">
		<@includePage path="${page.path}"/>
	</div>
</div>
</#if>
</div>
</body>
</html></#escape>

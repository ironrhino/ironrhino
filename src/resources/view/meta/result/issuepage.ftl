<#ftl output_format='HTML'>
<!DOCTYPE html>
<html>
<head>
<title>${getText(name)}</title>
</head>
<body>
<ul class="breadcrumb">
	<li>
		<a class="ajax view" href="<@url value="/"/>">${getText('index')}</a> <span class="divider">/</span>
	</li>
	<li class="active">${getText(name)}</li>
</ul>
<div class="issue ${name}">
<#if page??>
<div>
	<h3 class="title center">${page.title!}</h3>
	<div class="date center">${page.createDate?date}</div>
	<div class="content">
		<@includePage path="${page.path}"/>
	</div>
</div>
</#if>
</div>
</body>
</html>
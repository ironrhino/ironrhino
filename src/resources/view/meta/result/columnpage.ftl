<!DOCTYPE html>
<#escape x as x?html><html>
<head>
<title>${page.title!}</title>
</head>
<body>
<ul class="breadcrumb">
	<li>
    	<a href="<@url value="/"/>">${action.getText('index')}</a> <span class="divider">/</span>
	</li>
<#if !column??>
	<li class="active">${action.getText(name)}</li>
<#else>
	<li>
    	<a href="<@url value="/${name}"/>">${action.getText(name)}</a> <span class="divider">/</span>
	</li>
	<li class="active">${column!}</li>
</#if>
</ul>
<div class="container-fluid column ${name}">
  <div class="row-fluid">
    <div class="span2">
		<ul class="nav nav-list">
			<li class="nav-header">${name}</li>
			<#list columns as var>
			<#assign active=column?? && column==var/>
			<li<#if active> class="active"</#if>><a href="<@url value="/${name}/list/${var?url}"/>" class="ajax view history">${var}</a></li>
			</#list>
		</ul>
    </div>
    <div class="span10">
    <#if page??>
    	<h3 class="title" style="text-align:center;">${page.title!}</h3>
    	<div class="content"><@includePage path="${page.path}"/></div>
	</#if>
    </div>
  </div>
</div>
</body>
</html></#escape>

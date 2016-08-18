<!DOCTYPE html>
<#escape x as x?html><html>
<head>
<title>${page.title!}</title>
</head>
<body>
<ul class="breadcrumb">
	<li>
    	<a class="ajax view" href="<@url value="/"/>">${action.getText('index')}</a> <span class="divider">/</span>
	</li>
<#if !column?has_content>
	<li class="active">${action.getText(name)}</li>
<#else>
	<li>
    	<a class="ajax view" href="<@url value="/${name}"/>">${action.getText(name)}</a> <span class="divider">/</span>
	</li>
	<li class="active">${column!}</li>
</#if>
</ul>
<div class="column ${name}">
  <div class="row<#if fluidLayout>-fluid</#if>">
    <div class="span2">
		<ul class="nav nav-list">
			<li class="nav-header"><a href="<@url value="/${name}"/>" class="ajax view">${name}</a></li>
			<#list columns as var>
			<#assign active=column?has_content && column==var/>
			<li<#if active> class="active"</#if> style="padding-left:10px;"><a href="<@url value="/${name}/list/${var?url}"/>" class="ajax view">${var}</a></li>
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

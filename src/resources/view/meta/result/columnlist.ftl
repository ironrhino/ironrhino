<!DOCTYPE html>
<#escape x as x?html><html>
<head>
<title>${action.getText(name)}</title>
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
  <div class="row">
    <div class="span2">
		<ul class="nav nav-list">
			<li class="nav-header<#if !column?has_content> active</#if>"><a href="<@url value="/${name}"/>" class="ajax view">${name}</a></li>
			<#list columns as var>
			<#assign active=column?has_content && column==var/>
			<li<#if active> class="active"</#if> style="padding-left:10px;"><a href="<@url value="/${name}/list/${var?url}"/>" class="ajax view">${var}</a></li>
			</#list>
		</ul>
    </div>
    <div id="list" class="span10">
		<ul class="unstyled">
		<#list resultPage.result as page>
			<li><a href="<@url value="/${name}/p${page.path}"/><#if column?has_content>?column=${column}</#if>"><#if page.title?has_content><@page.title?interpret/></#if></a></li>
		</#list>
		</ul>
		<@pagination class="ajax view history cache" data\-replacement="list"/>
    </div>
  </div>
</div>
</body>
</html></#escape>

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
<#if !page??>
	<li class="active">${action.getText(name)}</li>
<#else>
	<li>
    	<a href="<@url value="/${name}"/>">${action.getText(name)}</a> <span class="divider">/</span>
	</li>
	<li class="active">${page.title!}</li>
</#if>
</ul>
<div class="container-fluid series ${name}">
  <div class="row-fluid">
    <div class="span2">
		<ul class="nav nav-list">
			<li class="nav-header">${name}</li>
			<#list pages as var>
			<#assign active=page?? && page.path==var.path/>
			<li<#if active> class="active"</#if>><a href="<@url value="/${name}/p${var.path}"/>" class="ajax view history">${var.title}</a></li>
			</#list>
		</ul>
    </div>
    <div class="span10">
		<#if page??>
			<div class="chapter">
				<@includePage path="${page.path}"/>
			</div>
			<#if showPager>
			<div class="pager">
				<#if previousPage??><li><a href="<@url value="/${name}/p${previousPage.path}"/>" class="ajax view history">${action.getText('previouspage')}:${previousPage.title}</a></li></#if>
				<#if nextPage??><li><a href="<@url value="/${name}/p${nextPage.path}"/>" class="ajax view history">${action.getText('nextpage')}:${nextPage.title}</a></li></#if>
			</div>
			</#if>
		</#if>
    </div>
  </div>
</div>
</body>
</html></#escape>

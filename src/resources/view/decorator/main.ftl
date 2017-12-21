<#ftl output_format='HTML'>
<#assign charset=response.characterEncoding!'utf-8'/>
<#assign requestURI=request.requestURI[request.contextPath?length..]/>
<#if fluidLayout>
	<#assign fluidLayout = 'welcome'!= (page.properties["meta.body_class"])!/>
</#if>
<#compress>
<!DOCTYPE html>
<html>
<head>
<title>${title?no_esc}</title>
<meta charset="${charset}">
<#if request.contextPath!=''>
<meta name="context_path" content="${request.contextPath}">
</#if>
<#assign verboseMode = properties['verboseMode']!>
<#if verboseMode?has_content>
<meta name="verbose_mode" content="${verboseMode}">
</#if>
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<link rel="icon" type="image/png" href="<@url value="/assets/images/favicon.png"/>">
<link href="<@url value="/assets/styles/ironrhino${devMode?then('','-min')}.css"/>" media="all" rel="stylesheet" type="text/css">
<script src="<@url value="/assets/scripts/ironrhino${devMode?then('','-min')}.js"/>" type="text/javascript"<#if !head?contains('</script>')> defer</#if>></script>
<#include "include/assets.ftl" ignore_missing=true/>
${head?no_esc}
</head>

<body lang="${.lang}" class="main<#if sidebarLayout> sidebar</#if><#assign body_class=(page.properties["meta.body_class"])!/><#if body_class?has_content> ${body_class}</#if>">
<#include "include/top.ftl" ignore_missing=true/>
<#if 'welcome'!=page.properties["meta.body_class"]!>
<@authorize ifNotGranted="ROLE_BUILTIN_ANONYMOUS">
<header class="navbar navbar-fixed-top">
<div class="navbar-inner">
<div class="container<#if fluidLayout>-fluid</#if>">
	<a class="btn btn-navbar" data-toggle="collapse" data-target=".nav-collapse">
		<span class="icon-bar"></span>
		<span class="icon-bar"></span>
		<span class="icon-bar"></span>
	</a>
	<#include "include/brand.ftl" ignore_missing=true/>
	<div class="btn-group pull-right">
	<#assign user = authentication("principal")>
		<a href="#" class="btn dropdown-toggle" data-toggle="dropdown">
		<i class="glyphicon glyphicon-user"></i> <#if user.name??>${user.name}<#elseif user.username??>${user.username}<#else>${(user?string)!}</#if> <span class="caret"></span>
		</a>
		<#include "include/dropdown.ftl" ignore_missing=true/>
	</div>
	<div class="nav-collapse">
		<#include "include/nav.ftl" ignore_missing=true/>
	</div>
</div>
</div>
</header>
<#include "include/sidebar.ftl" ignore_missing=true/>
</@authorize>
</#if>
<div id="content" class="container<#if fluidLayout>-fluid</#if>">
<#if action.hasActionMessages() || action.hasActionErrors()>
<div id="message">
<@s.actionerror />
<@s.actionmessage />
</div>
</#if>
${body?no_esc}
</div>
<#include "include/bottom.ftl" ignore_missing=true/>
</body>
</html></#compress>
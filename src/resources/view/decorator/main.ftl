<#assign charset=response.characterEncoding!'utf-8'/>
<#assign requestURI=request.requestURI[request.contextPath?length..]/>
<#assign modernBrowser = true/>
<#assign ua = request.getAttribute('userAgent')!/>
<#if ua?? && ua.name=='msie' && ua.majorVersion lt 9>
<#assign modernBrowser = false/>
</#if>
<#if modernBrowser>
<!DOCTYPE html>
<html>
<#else>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
</#if>
<#compress><#escape x as x?html>
<head>
<title><#noescape>${title}</#noescape></title>
<#if modernBrowser>
<meta charset="${charset}">
<#else>
<meta http-equiv="Content-Type" content="text/html; charset=${charset}"/>
</#if>
<#if request.contextPath!=''>
<meta name="context_path" content="${request.contextPath}" />
</#if>
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<link rel="shortcut icon" href="<@url value="/assets/images/favicon.ico"/>" />
<link href="<@url value="/assets/styles/ironrhino${devMode?then('','-min')}.css"/>" media="all" rel="stylesheet" type="text/css" />
<script src="<@url value="/assets/scripts/ironrhino${devMode?then('','-min')}.js"/>" type="text/javascript"<#if modernBrowser&&!head?contains('</script>')> defer</#if>></script>
<#include "include/assets.ftl" ignore_missing=true/>
<#noescape>${head}</#noescape>
</head>

<body class="main ${.lang} ${(page.properties["meta.body_class"])!}<#if modernBrowser> render-location-qrcode</#if>">
<#if !modernBrowser>
<div class="container<#if fluidLayout>-fluid</#if>">
<div class="alert">
<button type="button" class="close" data-dismiss="alert">&times;</button>
<#noescape>
${action.getText('browser.warning')}
</#noescape>
</div>
</div>
</#if>
<#include "include/top.ftl" ignore_missing=true/>
<#if 'welcome'!=page.properties["meta.body_class"]!>
<@authorize ifNotGranted="ROLE_BUILTIN_ANONYMOUS">
<div class="navbar navbar-fixed-top">
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
</div>
</@authorize>
</#if>
<div id="content" class="container<#if fluidLayout>-fluid</#if>">
<#if action.hasActionMessages() || action.hasActionErrors()>
<div id="message">
<@s.actionerror />
<@s.actionmessage />
</div>
</#if>
<#noescape>${body}</#noescape>
</div>
<#include "include/bottom.ftl" ignore_missing=true/>
</body>
</html></#escape></#compress>
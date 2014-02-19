<!DOCTYPE html>
<#escape x as x?html><html>
<head>
<title>
<#if preview>[${action.getText('preview')}]</#if><#if page.title??><@page.title?interpret/></#if></title>
<#noescape>${page.head!}</#noescape>
</head>
<body>
<div class="page content">
	<#assign designMode=(Parameters.designMode!)=='true'&&statics['org.ironrhino.core.util.AuthzUtils'].authorize("ROLE_ADMINISTRATOR","","")>
	<#if designMode>
	<div class="editme" data-url="<@url value="/common/page/editme?id=${page.id}"/>" name="page.content">
	</#if>
	<#if page.content??><@page.content?interpret/></#if>
	<#if designMode>
	</div>
	</#if>
</div>
</body>
</html></#escape>

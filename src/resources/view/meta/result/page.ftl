<#ftl output_format='HTML'>
<!DOCTYPE html>
<html>
<head>
<title>
<#if preview>[${action.getText('preview')}]</#if><#if page.title??><@page.title?interpret/></#if></title>
${(page.head?no_esc)!}
</head>
<body>
<div class="page content">
	<#assign designMode=(Parameters.designMode!)=='true'&&hasRole("ROLE_ADMINISTRATOR")>
	<#if designMode>
	<div class="editme" data-url="<@url value="/common/page/editme?id=${page.id}"/>" name="page.content">
	</#if>
	<#if page.content??><@page.content?interpret/></#if>
	<#if designMode>
	</div>
	</#if>
</div>
</body>
</html>

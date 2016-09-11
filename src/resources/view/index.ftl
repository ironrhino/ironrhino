<#ftl output_format='HTML'>
<!DOCTYPE html>
<html>
<head>
<title>${action.getText('index')}</title>
</head>
<body>
<#if printSetting??>
<div class="portal savable">
	<ul class="portal-column">
		<li id="announcement" class="portlet">
			<div class="portlet-header">${action.getText('announcement')}</div>
			<div class="portlet-content">
			<@authorize ifAnyGranted="ROLE_ADMINISTRATOR">
				<div class="ajaxpanel" data-url="<@url value="/common/setting/input/announcement?view=embedded&class=htmlarea"/>"></div>
			</@authorize>
			<@authorize ifNotGranted="ROLE_ADMINISTRATOR">
				<div style="padding-left:10px;white-space:pre-wrap;"><@printSetting key="announcement"/></div>
			</@authorize>
			</div>
		</li>
	</ul>
</div>
</#if>
</body>
</html>

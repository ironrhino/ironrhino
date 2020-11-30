<#ftl output_format='HTML'>
<!DOCTYPE html>
<html>
<head>
<title>${getText('serverMap')}</title>
</head>
<body>
<iframe src="<@url value="/assets/components/graphviz/viewer.html"/>#${beans['applicationContextInspector'].serverMap.toGraphvizString()?url}" style="width: 100%; height:500px; border: 0px;" scrolling="no"></iframe>
</body>
</html>
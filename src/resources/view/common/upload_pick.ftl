<#ftl output_format='HTML'>
<!DOCTYPE html>
<html>
<head>
<title>${action.getText('upload')}</title>
</head>
<body>
<@s.form id="upload_form" action="${actionBaseUrl}" method="post" class="ajax view form-inline" data\-replacement="files">
	<input type="hidden" name="pick" value="true"/>
	<table id="files" class="table table-striped middle" style="margin-top:50px;">
		<caption style="font-size:120%;font-weight:bold;"><@s.hidden id="folder" name="folder"/>${action.getText('current.location')}:<span id="current_folder" style="margin-left:10px;">${folder}<#if !folder?ends_with('/')>/</#if></span></caption>
		<thead>
		<tr style="font-weight:bold;height:43px;">
			<td style="width:30px" class="radio"></td>
			<td style="width:220px;"><span style="line-height:28px;">${action.getText('name')}</span><input type="search" class="filter input-small pull-right"/></td>
			<td class="center">${action.getText('preview')}</td>
		</tr>
		</thead>
		<tfoot>
		<tr>
			<td colspan="3" class="center">
			<button type="button" class="btn mkdir">${action.getText('create.subfolder')}</button>
			<button type="button" class="btn snapshot">${action.getText('snapshot')}</button>
			<button type="button" class="btn reload">${action.getText('reload')}</button>
			</td>
		</tr>
		</tfoot>
		<tbody>
		<#list files as key,value>
		<tr>
			<td class="radio"><#if value><input type="radio" name="id" value="<@url value="${action.getFileUrl(key?url)}"/>" class="custom"/></#if></td>
			<td><#if value><span class="uploaditem filename" style="cursor:pointer;">${key}</span> <a href="<@url value="${action.getFileUrl(key?url)}"/>" target="_blank" download="${key}"><i class="glyphicon glyphicon-download-alt"></i></a><#else><a style="color:blue;" class="ajax view" data-replacement="files" href="${actionBaseUrl}/pick${folderEncoded}/${key?replace('..','__')?url}">${key}</a></#if></td>
			<td class="center"><#if value && ['jpg','gif','png','bmp']?seq_contains(key?lower_case?split('.')?last)><a href="<@url value="${action.getFileUrl(key?url)}"/>" target="_blank"><img class="uploaditem" src="<@url value="${action.getFileUrl(key?url)}"/>" style="width:50px;height:50px;"/></a></#if></td>
		</tr>
		</#list>
		</tbody>
	</table>
</@s.form>
</body>
</html>



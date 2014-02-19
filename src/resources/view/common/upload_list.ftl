<!DOCTYPE html>
<#escape x as x?html><html>
<head>
<title>${action.getText('upload')}</title>
</head>
<body>
<@s.form id="upload_form" action="upload" method="post" enctype="multipart/form-data" cssClass="form-inline">
	<div class="row">
	<#list 1..Parameters.size?default('4')?number as index>
		<div class="span3"><@s.file theme="simple" name="file" multiple="true"/></div>
	</#list>
	</div>
	<div style="text-align:center;padding-top:30px;">
	<@s.submit theme="simple" value="${action.getText('upload')}"/>
	<label for="autorename">${action.getText('autorename')}:</label><@s.checkbox theme="simple" id="autorename" name="autorename" cssClass="custom"/>
	</div>
	<table id="files" class="checkboxgroup table table-striped middle" style="margin-top:50px;">
		<caption style="font-size:120%;font-weight:bold;"><@s.hidden id="folder" name="folder"/>${action.getText('current.location')}:<span id="current_folder" style="margin-left:10px;">${folder}<#if !folder?ends_with('/')>/</#if></span></caption>
		<thead>
		<tr style="font-weight:bold;height:43px;">
			<td style="width:30px" class="checkbox"><input type="checkbox" class="checkbox custom"/></td>
			<td style="width:300px;"><span style="line-height:28px;">${action.getText('name')}</span><input type="search" class="filter input-small pull-right"/></td>
			<td style="width:150px" class="center;">${action.getText('preview')}</td>
			<td >${action.getText('path')}</td>
		</tr>
		</thead>
		<tfoot>
		<tr>
			<td colspan="4" class="center">
			<button type="button" class="btn delete">${action.getText('delete')}</button>
			<button type="button" class="btn mkdir">${action.getText('create.subfolder')}</button>
			<button type="button" class="btn snapshot">${action.getText('snapshot')}</button>
			<button type="button" class="btn reload">${action.getText('reload')}</button>
			</td>
		</tr>
		</tfoot>
		<tbody>
		<#list files.entrySet() as entry>
		<tr>
			<td class="checkbox"><#if entry.key!='..'><input type="checkbox" name="id" value="${entry.key}" class="custom"/></#if></td>
			<td><#if entry.value><span class="uploaditem filename" style="cursor:pointer;">${entry.key}</span> <a href="<@url value="${fileStoragePath}/upload${folderEncoded}/${entry.key?url}"/>" target="_blank" download="${entry.key}"><i class="glyphicon glyphicon-download-alt"></i></a><#else><a style="color:blue;" class="ajax view history" data-replacement="files" href="<#if entry.key!='..'>${actionBaseUrl}/list${folderEncoded}/${entry.key?url}<#else>${actionBaseUrl}/list${folderEncoded?substring(0,folderEncoded?last_index_of('/'))}</#if>">${entry.key}</a></#if></td>
			<td><#if entry.value && ['jpg','gif','png','bmp']?seq_contains(entry.key?lower_case?split('.')?last)><a href="<@url value="${fileStoragePath}/upload${folderEncoded}/${entry.key?url}"/>" target="_blank"><img class="uploaditem" src="<@url value="${fileStoragePath}/upload${folderEncoded}/${entry.key?url}"/>" style="height:50px;"/></a></#if></td>
			<td><#if entry.value><span><@url value="${fileStoragePath}/upload${folderEncoded}/${entry.key?url}"/></span></#if></td>
		</tr>
		</#list>
		</tbody>
	</table>
</@s.form>
</body>
</html></#escape>



<#ftl output_format='HTML'>
<!DOCTYPE html>
<html>
<head>
<title>${getText('upload')}</title>
</head>
<body>
<@s.form id="upload_form" action="${actionBaseUrl}" method="post" enctype="multipart/form-data" class="ajax view form-horizontal">
	<div class="row<#if fluidLayout>-fluid</#if>">
		<div class="span5"><@s.file name="file" class="required custom input-xlarge" multiple="true"/></div>
		<div class="span4"><@s.checkbox name="autorename" class="switch input-medium"/></div>
		<div class="span3 center"><@s.submit theme="simple" class="btn-primary" label="${getText('upload')}"/></div>
	</div>
	<table id="files" class="checkboxgroup table table-striped middle">
		<caption style="font-size:120%;font-weight:bold;"><@s.hidden id="folder" name="folder"/>${getText('current.location')}:<span id="current_folder" style="margin-left:10px;">${folder}<#if !folder?ends_with('/')>/</#if></span></caption>
		<thead>
		<tr style="font-weight:bold;">
			<td style="width:30px;" class="checkbox"><input type="checkbox" class="checkall"/></td>
			<td style="width:300px;"><span style="line-height:30px;">${getText('name')}</span><input type="search" class="filter input-medium pull-right" form=""/></td>
			<td style="width:150px;">${getText('preview')}</td>
			<td >${getText('path')}</td>
		</tr>
		</thead>
		<tfoot>
		<tr>
			<td colspan="4" class="center">
			<button type="button" class="btn delete" data-shown="selected">${getText('delete')}</button>
			<button type="button" class="btn mkdir">${getText('create.subfolder')}</button>
			<button type="button" class="btn snapshot">${getText('snapshot')}</button>
			<button type="button" class="btn reload">${getText('reload')}</button>
			</td>
		</tr>
		</tfoot>
		<tbody>
		<#list files as key,value>
		<tr>
			<td class="checkbox"><#if key!='..'><input type="checkbox" name="id" value="${key+value?then('','/')}"/></#if></td>
			<td><#if value><span class="uploaditem filename" style="cursor:pointer;">${key}</span> <a href="<@url value="${action.getFileUrl(key?url)}"/>" target="_blank" download="${key}"><i class="glyphicon glyphicon-download-alt clickable"></i></a><#else><a style="color:blue;" class="ajax view history" data-replacement="files" href="<#if key!='..'>${actionBaseUrl}/list${folderEncoded}/${key?url}<#else>${actionBaseUrl}/list${folderEncoded?keep_before_last('/')}</#if>">${key}</a></#if></td>
			<td><#if value && ['jpg','gif','png','webp','bmp']?seq_contains(key?keep_after_last('.')?lower_case)><a href="<@url value="${action.getFileUrl(key?url)}"/>" target="_blank"><img class="uploaditem" src="<@url value="${action.getFileUrl(key?url)}"/>" style="height:50px;"/></a></#if></td>
			<td><#if value><span class="clipboard-content" style="word-break: break-all;"><@url value="${action.getFileUrl(key?url)}"/></span> <i class="glyphicon glyphicon-copy clickable set-clipboard" title="${getText('copy')}"></i></#if></td>
		</tr>
		</#list>
		</tbody>
	</table>
</@s.form>
</body>
</html>



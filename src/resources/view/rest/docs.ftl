<#macro listFields fields>
<table class="table">
<thead>
	<tr>
		<th style="width:150px;">名字</th>
		<th style="width:80px;">类型</th>
		<th style="width:80px;">必填</th>
		<th style="width:80px;">默认值</th>
		<th>备注</th>
	</tr>
</thead>
<tbody>
	<#list fields as field>
	<tr>
		<td>${field.name} <span class="label">${action.getText(field.label!field.name)}</span></td>
		<td>${field.type!} </td>
		<td>${action.getText(field.required?string)} </td>
		<td>${field.defaultValue!} </td>
		<td>${field.description!}
			<#if field.values?has_content>
			枚举值:
			<ul class="unstyled">
				<#list field.values.entrySet() as entry>
				<li>${entry.key} <span class="label">${entry.value}</span></li>
				</#list>
			</ul>
			</#if>
		</td>
	</tr>
	</#list>
</tbody>
</table>
</#macro>
<!DOCTYPE html>
<#escape x as x?html><html>
<head>
<title>${action.getText('docs')}<#if version?has_content> ${version}</#if></title>
</head>
<body>

<div class="container">
  <div class="row">
    <div class="span3">
		<div class="accordion" id="api_module_accordion">
		<#list apiModules as apiModule>
		  <div class="accordion-group">
		    <div class="accordion-heading">
		      <a class="accordion-toggle" data-toggle="collapse" data-parent="#api_module_accordion" href="#module${apiModule_index}"<#if apiModule.description?has_content> title="${apiModule.description}"</#if>>
		        ${apiModule.name}
		      </a>
		    </div>
		    <#assign currentModule = module?has_content && module==apiModule.name>
		    <div id="module${apiModule_index}" class="accordion-body collapse<#if currentModule> in</#if>">
		      <div class="accordion-inner">
		        <ul class="nav nav-list">
					<#list apiModule.apiDocs as apiDoc>
					<li<#if currentModule && api?has_content && api==apiDoc.name> class="active"</#if> style="padding-left:10px;"><a href="<@url value="${actionBaseUrl}?module=${apiModule.name}&api=${apiDoc.name}"/>" class="ajax view">${apiDoc.name}</a></li>
					</#list>
				</ul>
		      </div>
		    </div>
		  </div>
		</#list>
		</div>
    </div>
    <div id="apidoc" class="span9">
		<#if apiDoc??>
			<h4 style="text-align:center;">${apiDoc.name}</h4>
			<#if apiDoc.description?has_content>
			<div class="alert alert-info">
			  <#noescape>${apiDoc.description}</#noescape>
			</div>
			</#if>
			<table class="table">
				<tbody>
					<#if apiDoc.requiredAuthorities?has_content>
					<tr><td style="width:100px;">所需授权</td><td><#list apiDoc.requiredAuthorities as auth><span class="label label-info">${action.getText(auth)}</span> </#list></td></tr>
					</#if>
					<tr><td>请求方法</td><td><#list apiDoc.methods as method><span class="label label-info">${method}</span> </#list></td></tr>
					<tr><td>请求URL</td><td>${apiBaseUrl}${apiDoc.url}</td></tr>
					<#if apiDoc.pathVariables?has_content><tr><td>URL变量</td><td><@listFields fields=apiDoc.pathVariables/></td></tr></#if>
					<#if apiDoc.requestParams?has_content><tr><td>请求参数</td><td><@listFields fields=apiDoc.requestParams/></td></tr></#if>
					<#if apiDoc.requestHeaders?has_content><tr><td>请求头</td><td><@listFields fields=apiDoc.requestHeaders/></td></tr></#if>
					<#if apiDoc.requestBody?has_content><tr><td>请求消息体</td><td><@listFields fields=apiDoc.requestBody/></td></tr></#if>
					<#if apiDoc.sampleRequestBody?has_content><tr><td>请求消息体示例</td><td><code style="word-break: break-all;word-wrap: break-word;white-space: pre;white-space: pre-wrap;"><#noescape>${apiDoc.sampleRequestBody}</#noescape></code></td></tr></#if>
					<#if apiDoc.responseBody?has_content><tr><td>响应消息体</td><td><@listFields fields=apiDoc.responseBody/></td></tr></#if>
					<#if apiDoc.sampleResponseBody?has_content><tr><td>响应消息体示例</td><td><code style="word-break: break-all;word-wrap: break-word;white-space: pre;white-space: pre-wrap;"><#noescape>${apiDoc.sampleResponseBody}</#noescape></code></td></tr></#if>
				</tbody>
			</table>
		</#if>
    </div>
  </div>
</div>
</body>
</html></#escape>

<#macro listFields fields>
<table class="table">
<thead>
	<tr>
		<th style="width:200px;">名字</th>
		<th style="width:60px;">类型</th>
		<th style="width:60px;">必填</th>
		<th style="width:80px;">默认值</th>
		<th>备注</th>
	</tr>
</thead>
<tbody>
	<#list fields as field>
	<tr>
		<td>${field.name} 
		<#assign label=field.label!/>
		<#if !label?has_content>
		<#assign label=''/>
		<#list field.name?split('.') as var>
		<#assign label+=action.getText(var)/>
		</#list>
		</#if>
		<#if label!=field.name> <span class="label pull-right">${label}</span></#if>
		</td>
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
		<div class="accordion" id="api-accordion">
		<@classPresentConditional value="org.ironrhino.security.oauth.server.model.Authorization">
		  <div class="accordion-group">
		    <div class="accordion-heading">
		      <a class="accordion-toggle" data-toggle="collapse" data-parent="#api-accordion" href="#overview">
		     	接入基础
		      </a>
		    </div>
		    <div id="overview" class="accordion-body collapse<#if !module?has_content> in</#if>">
		      <div class="accordion-inner">
		      	<#assign partial=Parameters.partial!/>
		        <ul class="nav nav-list">
					<li<#if partial=='prerequisite'> class="active"</#if>><a href="<@url value="${actionBaseUrl}?partial=prerequisite"/><#if version?has_content>&version=${version}</#if>" class="ajax view">接入准备</a></li>
					<li<#if partial=='oauth2'> class="active"</#if>><a href="<@url value="${actionBaseUrl}?partial=oauth2"/><#if version?has_content>&version=${version}</#if>" class="ajax view">OAuth2</a></li>
					<li<#if partial=='status'> class="active"</#if>><a href="<@url value="${actionBaseUrl}?partial=status"/><#if version?has_content>&version=${version}</#if>" class="ajax view">通用返回状态消息</a></li>
				</ul>
		      </div>
		    </div>
		  </div>
		</@classPresentConditional>
		<#list apiModules.entrySet() as entry>
		  <#assign _category=entry.key>
		  <#if _category?has_content>
		  	<div class="accordion-group">
		    <div class="accordion-heading">
		      <a class="accordion-toggle" data-toggle="collapse" data-parent="#api-accordion" href="#category_${entry?index}">${_category}</a>
		    </div>
		    <div id="category_${entry?index}" class="accordion-body collapse<#if _category==category> in</#if>">
		    <div class="accordion-inner">
		    <div id="category_accordion_${entry?index}" class="accordion">
		  </#if>
		  <#list entry.value as apiModule>
		  <div class="accordion-group">
		    <div class="accordion-heading">
		      <a class="accordion-toggle" data-toggle="collapse" data-parent="#<#if _category?has_content>category_accordion_${entry?index}<#else>api-accordion</#if>" href="#module_${entry?index}_${apiModule?index}"<#if apiModule.description?has_content> title="${apiModule.description}"</#if>>${apiModule.name}</a>
		    </div>
		    <#assign currentModule = (!category?has_content||_category==category)&&module?has_content && module==apiModule.name>
		    <div id="module_${entry?index}_${apiModule?index}" class="accordion-body collapse<#if currentModule> in</#if>">
		      <div class="accordion-inner">
		        <ul class="nav nav-list">
					<#list apiModule.apiDocs as apiDoc>
					<li<#if currentModule && api?has_content && api==apiDoc.name> class="active"</#if>><a href="${actionBaseUrl}?<#if _category?has_content>category=${_category?url}&</#if>module=${apiModule.name?url}&api=${apiDoc.name?url}<#if version?has_content>&version=${version}</#if>" class="ajax view">${apiDoc.name}</a></li>
					</#list>
				</ul>
		      </div>
		    </div>
		  </div>
		  </#list>
		  <#if _category?has_content>
		  </div>
		  </div>
		  </div>
		  </div>
		  </#if>
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
					<#if apiDoc.cookieValues?has_content><tr><td>请求Cookie</td><td><@listFields fields=apiDoc.cookieValues/></td></tr></#if>
					<#if apiDoc.requestBody?has_content><tr><td>请求消息体<#if !apiDoc.requestBodyRequired><br/><span class="label label-info">可选</span></#if><#if apiDoc.requestBodyType?has_content><br/><span class="label label-warning">${action.getText(apiDoc.requestBodyType)}</span></#if></td><td><@listFields fields=apiDoc.requestBody/></td></tr></#if>
					<#if apiDoc.requestBodySample?has_content><tr><td>请求消息体示例</td><td><code style="word-break: break-all;word-wrap: break-word;white-space: pre;white-space: pre-wrap;"><#noescape>${apiDoc.requestBodySample}</#noescape></code></td></tr></#if>
					<#if apiDoc.responseBody?has_content><tr><td>响应消息体<#if apiDoc.responseBodyType?has_content><br/><span class="label label-warning">${action.getText(apiDoc.responseBodyType)}</span></#if></td><td><@listFields fields=apiDoc.responseBody/></td></tr></#if>
					<#if apiDoc.responseBodySample?has_content><tr><td>响应消息体示例</td><td><code style="word-break: break-all;word-wrap: break-word;white-space: pre;white-space: pre-wrap;"><#noescape>${apiDoc.responseBodySample}</#noescape></code></td></tr></#if>
				</tbody>
			</table>
		<#elseif partial?has_content>
			<#include "${partial}.ftl"/>
		</#if>
    </div>
  </div>
</div>
</body>
</html></#escape>

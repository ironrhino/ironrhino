<#ftl output_format='HTML'>
<#assign method=apiDoc.methods[0]>
<#assign url=apiBaseUrl+apiDoc.url>
<#assign url=url?replace('\\{([^{}]+)\\}','<span style="background:yellow;border-bottom:1px solid;padding:2px;" contenteditable="plaintext-only">$1</span>','r')>
<form class="api-playground" method="${method}">
<table class="table">
<tbody>
<@classPresentConditional value="org.ironrhino.security.oauth.server.model.Authorization">
<tr><td>Access Token</td><td><input type="text" name="accessToken" class="input-xlarge" placeholder="不填会默认使用当前用户"> <button type="button" class="btn last-accessToken">最近使用</button> <button type="button" class="btn fetch-accessToken">重新获取</button>
	<#if !base?has_content>
		<#assign base=statics['org.ironrhino.core.util.RequestUtils'].getBaseUrl(request)>
	</#if>
	<div class="fetch-accessToken-form" style="display: none;padding-top: 20px;" data-endpoint="${base}/oauth/oauth2/token">
	<div><select name="grant_type" class="input-xlarge"><option>client_credentials</option><option>password</option></select></div>
	<div style="padding-top: 5px;"><input type="text" name="client_id" class="input-xlarge required" placeholder="client_id"></div>
	<div style="padding-top: 5px;"><input type="text" name="client_secret" class="input-xlarge required" placeholder="client_secret"></div>
	<div class="grant-type-password" style="display:none;">
	<div style="padding-top: 5px;"><input type="text" name="username" class="input-xlarge required" placeholder="username"></div>
	<div style="padding-top: 5px;"><input type="password" name="password" class="input-xlarge required" placeholder="password"></div>
	</div>
	<div style="padding-top: 5px; padding-bottom: 10px;"><button type="button" class="btn btn-primary">发送请求</button></div>
	</div>
</td></tr>
</@classPresentConditional>
<tr><td style="width:100px;"><span class="label label-info">${method}</span></td><td><div class="url">${url?no_esc}</div></td></tr>
<#if apiDoc.requestParams?has_content>
<tr><td>请求参数</td><td class="compact-horizontal">
	<table class="requestParams table datagrid adaptive"><tbody>
	<#list apiDoc.requestParams as param>
	<tr>
	<td><input type="text" value="${param.name}" placeholder="名字" readonly></td><td class="center middle"> = </td>
	<td>
	<#if param.values?has_content>
	<select name="${param.name}"<#if param.required> class="required"</#if><#if param.multiple> multiple</#if>>
		<option value=""></option>
		<#list param.values as key,value>
		<option value="${key}">${value}</option>
		</#list>
	</select>
	<#else>
	<input name="${param.name}"<#if param.type=='file'> type="file" class="custom"<#if param.multiple> multiple</#if><#else> type="text" value="${param.defaultValue!}" placeholder="值"</#if><#if param.required> class="required"</#if>>
	</#if>
	</td>
	</tr>
	</#list>
	</tbody></table>
</td></tr>
</#if>
<#if apiDoc.requestHeaders?has_content>
<tr><td>请求头</td><td class="compact-horizontal">
	<table class="requestHeaders table datagrid adaptive"><tbody>
	<#list apiDoc.requestHeaders as header>
	<tr>
	<td><input type="text" value="${header.name}" placeholder="名字" readonly></td><td class="center middle"> = </td>
	<td>
	<#if param.values?has_content>
	<select name="${param.name}"<#if param.required> class="required"</#if><#if param.multiple> multiple</#if>>
		<option value=""></option>
		<#list param.values as key,value>
		<option value="${key}">${value}</option>
		</#list>
	</select>
	<#else>
	<input type="text" name="${header.name}" value="${header.defaultValue!}" placeholder="值"<#if header.required> class="required"</#if>>
	</#if>
	</td>
	</tr>
	</#list>
	</tbody></table>
</td></tr>
</#if>
<#if apiDoc.requestBodySample?has_content>
<tr><td>请求消息体</td><td>
<code class="requestBody block json" contenteditable>${apiDoc.requestBodySample}</code>
</td></tr>
</#if>
<tr><td>响应状态</td><td>
<div class="responseStatus"></div>
</td></tr>
<tr><td>响应头</td><td>
<p class="responseHeaders"></p>
</td></tr>
<tr><td>响应消息体</td><td>
<code class="responseBody block json" style="display:inline-block;box-sizing:border-box;min-width:100%;min-height:50px;"></code></td></tr>
</tbody>
</table>
<button type="submit" class="btn btn-primary btn-block">${getText('confirm')}</button>
</form>

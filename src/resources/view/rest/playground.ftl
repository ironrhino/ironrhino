<#ftl output_format='HTML'>
<#assign method=apiDoc.methods[0]>
<#assign url=apiBaseUrl+apiDoc.url>
<#assign url=url?replace('\\{(.+)\\}','<span style="background:yellow;" contenteditable>$1</span>','r')>
<form class="api-playground" method="${method}">
<table class="table">
<tbody>
<tr><td style="width:100px;">URL</td><td><div class="url">${url?no_esc}</div></td></tr>
<@classPresentConditional value="org.ironrhino.security.oauth.server.model.Authorization">
<tr><td>Access Token</td><td><input type="text" class="accessToken" placeholder="不填会默认使用当前用户"></td></tr>
</@classPresentConditional>
<tr><td>请求参数</td><td>
	<table class="requestParams table datagrid adaptive"><tbody>
	<#if apiDoc.requestParams?has_content>
	<#list apiDoc.requestParams as param>
	<tr><td><input type="text" value="${param.name}" placeholder="名字"></td><td class="center middle"> = </td><td><input type="text" value="${param.defaultValue!}" placeholder="值"></td><td class="manipulate"></td></tr>
	</#list>
	<#else>
	<tr><td><input type="text" placeholder="名字"></td><td class="center middle"> = </td><td><input type="text" placeholder="值"></td><td class="manipulate"></td></tr>
	</#if>
	</tbody></table>
</td></tr>
<tr><td>请求头</td><td>
	<table class="requestHeaders table datagrid adaptive"><tbody>
	<#if apiDoc.requestHeaders?has_content>
	<#list apiDoc.requestHeaders as header>
	<tr><td><input type="text" value="${header.name}" placeholder="名字"></td><td class="center middle"> = </td><td><input type="text" value="${header.defaultValue!}" placeholder="值"></td><td class="manipulate"></td></tr>
	</#list>
	<#else>
	<tr><td><input type="text" placeholder="名字"></td><td class="center middle"> = </td><td><input type="text" placeholder="值"></td><td class="manipulate"></td></tr>
	</#if>
	</tbody></table>
</td></tr>						
<#if apiDoc.requestBodySample?has_content>
<tr><td>请求消息体</td><td>
<code class="requestBody block" contenteditable>${apiDoc.requestBodySample}</code>
</td></tr>
</#if>
<tr><td>响应状态</td><td>
<div class="responseStatus"></div>
</td></tr>
<tr><td>响应头</td><td>
<p class="responseHeaders"></p>
</td></tr>
<tr><td>响应消息体</td><td>
<code class="responseBody block" style="display:inline-block;min-width:100%;min-height:50px;"></code></td></tr>
</tbody>
</table>
<button type="submit" class="btn btn-primary btn-block">${getText('confirm')}</button>
</form>

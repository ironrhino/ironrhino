<#if !base?has_content>
	<#assign base=statics['org.ironrhino.core.util.RequestUtils'].getBaseUrl(request)>
</#if>
<h4 class="center">OAuth2</h4>
<ol>
<li>获取access_token
	<ul class="unstyled">
	<li>
	<h5><em>client_credentials</em>方式,此方式获取的token代表的用户是client的所有者</h5>
	<ol>
	<li>client访问<br><code>${base}/oauth/oauth2/token?grant_type=client_credentials&client_id={client_id}&client_secret={client_secret}</code></li>
	<li>服务器返回 access_token, 示例:<br><code>{"expires_in":3600,"access_token":"{access_token}","refresh_token":"{refresh_token}"}</code></li>
	</ol>
	</li>
	<li>
	<h5><em>password</em>方式,此方式获取的token代表的用户是username对应的用户</h5>
	<ol>
	<li>client访问<br><code>${base}/oauth/oauth2/token?grant_type=password&client_id={client_id}&client_secret={client_secret}&username={username}&password={password}</code></li>
	<li>服务器返回 access_token, 示例:<br><code>{"expires_in":3600,"access_token":"{access_token}","refresh_token":"{refresh_token}"}</code></li>
	<li>如果是移动App请求, 请带上设备号和设备名称参数<br><code>&device_id={device_id}&device_name={device_name}</code></li>
	<#if (properties['verification.code.enabled']!)=='true'>
	<li>用验证码交换token需要先触发发送 <br><code>${base}/oauth/oauth2/sendVerificationCode?client_id={client_id}&client_secret={client_secret}&username={username}</code>
	<br>
	交换token的时除了带上username和password还需要verificationCode参数, 示例:<br><code>&username={username}&password={password}&verificationCode={verificationCode}</code>
	<br>
	如果不需要password只需要verificationCode参数, 可以把验证码放入password, 示例:<br><code>&username={username}&password={verificationCode}</code>
	</li>
	</#if>
	</ol>
	</li>
	<li>
	<h5><em>authorization_code</em>方式,此方式获取的token代表的用户是授权人</h5>
	<ol>
	<li>用户点击浏览器链接<br><code>${base}/oauth/oauth2/auth?response_type=code&client_id={client_id}&redirect_uri={redirect_uri}</code><br>redirect_uri需要和client时候预留的一致</li>
	<li>浏览器跳转到授权页面让用户输入用户名密码</li>
	<li>浏览器跳转到{redirect_uri},并且带上了请求参数code={code}</li>
	<li>client的后台根据返回的code访问<br><code>${base}/oauth/oauth2/token?grant_type=authorization_code&code={code}&client_id={client_id}&client_secret={client_secret}&redirect_uri={redirect_uri}</code></li>
	<li>服务器返回 access_token, 示例:<br><code>{"expires_in":3600,"access_token":"{access_token}","refresh_token":"{refresh_token}"}</code></li>
	</ol>
	</li>
	</ul>
</li>
<li>把access_token放入请求参数或者请求头里面来调用API
	<div>
	请求参数名为access_token:<br><code>curl ${apiBaseUrl}/user/@self?access_token={access_token}</code>
	</div>
	<div>
	请求头名为Authorization:<br><code>curl -H "Authorization: Bearer {access_token}" ${apiBaseUrl}/user/@self</code>
	</div>
</li>
<li>获取或使用access_token的错误响应
	<#if 'true'==properties['oauth.error.legacy']!>
	<ul>
	<li>
	<div>响应消息体为json格式, 包含code和status字段和message(可选)字段.
	示例: <code>{"code":"7","status":"UNAUTHORIZED","message":"invalid_token"}</code>
	</div>
	</li>
	<li>响应状态码列表
	<ul>
		<li>BAD_REQUEST:		400</li>
		<li>UNAUTHORIZED:	401</li>
	</ul>
	</li>
	</ul>	
	<#else>
	<ul>
	<li>参见: 
	<a href="https://tools.ietf.org/html/rfc6749#section-5.2" target="_blank">https://tools.ietf.org/html/rfc6749#section-5.2</a>  
	<a href="https://tools.ietf.org/html/rfc6750#section-3.1" target="_blank">https://tools.ietf.org/html/rfc6750#section-3.1</a>
	</li>
	<li>
	<div>响应消息体为json格式, 包含error字段和error_message(可选)字段.
	示例: <code>{"error":"invalid_token","error_message":"expired_token"}</code>
	</div>
	</li>
	<li>响应状态码列表
	<ul>
		<li>invalid_request:	400</li>
		<li>invalid_client:	400</li>
		<li>invalid_token:	401</li>
		<li>invalid_grant:	400</li>
		<li>invalid_scope:	400</li>
		<li>insufficient_scope:	403</li>
		<li>unauthorized_client:	401</li>
		<li>unsupported_grant_type:	400</li>
	</ul>
	</li>
	</ul>
	</#if>
</li>
</ol>



其他token相关的API:
<ul>
<li>
刷新token:<br><code> ${base}/oauth/oauth2/token?grant_type=refresh_token&refresh_token={refresh_token}&client_id={client_id}&client_secret={client_secret}</code>
</li>
<li>
回收token:<br><code> ${base}/oauth/oauth2/revoke?access_token={access_token}</code>
</li>
</ul>
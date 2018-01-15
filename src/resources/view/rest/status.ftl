<h4 class="center">通用返回状态消息</h4>
<div class="alert alert-info">
消息格式： <code>{"code":"{code}","status":"{status}"}</code> <br/>
如果有详细的错误信息：<code>{"code":"{code}","status":"{status}","message":"{message}"}</code> <br/>
如果有详细的fieldErrors：<code>{"code":"{code}","status":"{status}","message":"{message}","fieldErrors": {"{field}": ["{error}"]}}</code> <br/>
</div>
<table class="table table-bordered table-striped">
	<thead>
		<tr><th style="width:100px;">code</th><th style="width:200px;">status</th><th>描述</th></tr>
	</thead>
	<tbody>
		<tr><td>0</td><td>OK</td><td>操作成功</td></tr>
		<tr><td>1</td><td>REQUEST_TIMEOUT</td><td>请求超时</td></tr>
		<tr><td>2</td><td>FORBIDDEN</td><td>禁止操作</td></tr>
		<tr><td>3</td><td>UNAUTHORIZED</td><td>没有授权或者权限不够</td></tr>
		<tr><td>4</td><td>NOT_FOUND</td><td>资源不存在</td></tr>
		<tr><td>5</td><td>ALREADY_EXISTS</td><td>资源已经存在,违反唯一性约束</td></tr>
		<tr><td>6</td><td>FIELD_INVALID</td><td>字段不合法,具体见详细错误信息</td></tr>
		<tr><td>7</td><td>BAD_REQUEST</td><td>请求不合法,具体见详细错误信息</td></tr>
		<tr><td>-1</td><td>INTERNAL_SERVER_ERROR</td><td>服务器内部错误,具体见详细错误信息</td></tr>
	</tbody>
</table>
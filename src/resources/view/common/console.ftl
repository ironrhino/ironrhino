<#ftl output_format='HTML'>
<!DOCTYPE html>
<html>
<head>
<title>${getText('console')}</title>
<style>
	li>div{
		line-height: 30px;
	}
	div.key{
		text-align: right;
	}
</style>
<script>
$(function(){
		$('#trigger .btn').click(function(){
			var t = $(this);
			$.alerts({
				type : 'confirm',
				callback : function(b) {
					if (b) {
						ajax({
						type:'POST',
						url:'${actionBaseUrl}/executeJson',
						data:{
							expression : t.data('expression')||t.text(),
							scope: t.data('scope')
						},
						beforeSend:function(){
							t.prop('disabled',true);
						},
						onsuccess:function(data){
							Message.showActionMessage(MessageBundle.get('success'));
						},
						complete:function(){
							t.prop('disabled',false);
						}
						});
					}
				}
			});
		});
		<#if printSetting??>
		$('#switch input:checkbox').change(function(e){
			var t = this;
			var key = t.name;
			var value = t.checked;
			ajax({
			type:'POST',
			url:'${actionBaseUrl}/executeJson',
			data:{
				expression : 'settingControl.setValue("'+key+'","'+value+'")'
			}
			});
		});
		</#if>
});
</script>
</head>
<body>
<@s.form id="form" action=actionBaseUrl method="post" class="form-horizontal ajax focus">
	<div class="row-fluid">
	<div class="span8">
	<@s.textarea name="expression" class="required" style="width:100%;height:100px;"/>
	</div>
	<div class="span4">
	<@s.select name="scope" class="input-medium" list="@org.ironrhino.core.metadata.Scope@values()" listKey="name" listValue="displayName"/>
	</div>
	</div>
	<@s.submit label=getText('submit') class="btn-primary"/>
</@s.form>
<hr/>

<#assign triggers = beans['applicationContextConsole'].triggers>
<#if triggers?size gt 0>
<div id="trigger">
	<ul class="thumbnails">
	<#list triggers as key,value>
	<li class="span4">
	<button type="button" class="btn btn-block" data-scope="${value.name()}" data-expression="${key}">${getText(key)}</button>
	</li>
	</#list>
	</ul>
</div>
<hr/>
</#if>

<#assign lifecycleBeans = beans['applicationContextConsole'].lifecycleBeans>
<#if lifecycleBeans?size gt 0>
<@s.form id="lifecycle-form" action=actionBaseUrl method="post" class="form-horizontal ajax view">
<@s.hidden name="scope" value="APPLICATION"/>
<div>
	<ul class="thumbnails">
	<#list lifecycleBeans as key,value>
	<li class="span4">
	<div class="row-fluid">
	<div class="key span8"><#if key?index_of('.') gt 0>${key?keep_after_last('.')}<#else>${key}</#if></div>
	<div class="span4">
	<button type="submit" class="btn confirm" name="expression" value="${key}.start()"<#if value.running> disabled</#if>>${getText('start')}</button>
	<button type="submit" class="btn confirm" name="expression" value="${key}.stop()"<#if !value.running> disabled</#if>>${getText('stop')}</button>
	</div>
	</div>
	</li>
	</#list>
	</ul>
</div>
</@s.form>
<hr/>
</#if>

<#if printSetting??>
<#assign settings = beans['settingControl'].getAllBooleanSettings()>
<#if settings?size gt 0>
<div id="switch">
	<ul class="thumbnails">
	<#list settings as setting>
	<li class="span4">
	<div class="row-fluid">
	<div class="span6 key"<#if setting.description?has_content> title="${setting.description}"</#if>>${getText(setting.key)}</div>
	<div class="span6"><input type="checkbox" name="${setting.key}"<#if setting.value=='true'> checked="checked"</#if> class="switch switch-round"/></div>
	</div>
	</li>
	</#list>
	</ul>
</div>
<hr/>
</#if>
</#if>
</body>
</html>
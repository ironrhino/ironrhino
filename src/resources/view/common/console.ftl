<!DOCTYPE html>
<#escape x as x?html><html>
<head>
<title>${action.getText('console')}</title>
<script>
$(function(){
		$('#trigger .btn').click(function(){
			var t = $(this);
			$.ajax({
				type:'POST',
				url:'${actionBaseUrl}/executeJson',
				data:{
					expression : $(this).data('expression')||$(this).text(),
					scope: $(this).data('scope')
				},
				beforeSend:function(){
					t.prop('disabled',true);
				},
				success:function(data){
					if(data && data.actionErrors){
						alert(data.actionErrors[0]);
					}else{
						alert(MessageBundle.get('success'));
					}
					t.prop('disabled',false);
				},
				error:function(data){
					alert(MessageBundle.get('error'));
					t.prop('disabled',false);
				}
			});
		});
		<#if printSetting??>
		$('#switch input:checkbox').change(function(e){
			var t = this;
			var key = t.name;
			var value = t.checked;
			$.post('${actionBaseUrl}/executeJson',
								{
								expression : 'settingControl.setValue("'+key+'","'+value+'")'
								}
								,function(data){
									if(data && data.actionErrors){
										$(t).closest('.switch').bootstrapSwitch('toggleState');
										alert(data.actionErrors[0]);
										return;
									}
								});
		});
		</#if>
							
});
</script>
</head>
<body>
<@s.form id="form" action="${actionBaseUrl}" method="post" class="ajax focus form-inline well">
	<div class="row-fluid">
	<div class="span6">
	<span>${action.getText('expression')}: </span><@s.textfield theme="simple" id="expression" name="expression" class="required" style="width:80%;"/>
	</div>
	<div class="span3">
	<span>${action.getText('scope')}: </span><@s.select theme="simple" id="scope" name="scope" class="input-medium" list="@org.ironrhino.core.metadata.Scope@values()" listKey="name" listValue="displayName"/>
	</div>
	<div class="span3">
	<@s.submit id="submit" theme="simple" value="%{getText('confirm')}" />
	</div>
</@s.form>
<hr/>

<#assign triggers = beans['applicationContextConsole'].getTriggers()>
<#if triggers?keys?size gt 0>
<div id="trigger"<#if fluidLayout> class="container"</#if>>
	<ul class="thumbnails">
	<#list triggers.entrySet() as entry>
	<li class="span4">
	<button type="button" class="btn btn-block" data-scope="${entry.value.name()}" data-expression="${entry.key}">${action.getText(entry.key)}</button>
	</li>
	</#list>
	</ul>
</div>
<hr/>
</#if>

<#if printSetting??>
<#assign settings = beans['settingControl'].getAllBooleanSettings()>
<#if settings?size gt 0>
<div id="switch"<#if fluidLayout> class="container"</#if>>
	<style scoped>
	div.key{
		text-align: right;
		line-height: 30px;
		font-weight: bold;
	}
	</style>
	<ul class="thumbnails">
	<#list settings as setting>
	<li class="span4">
	<div class="row-fluid">
	<div class="span6 key"<#if setting.description?has_content> title="${setting.description}"</#if>>${action.getText(setting.key)}</div>
	<div class="span6"><div class="switch" data-on-label="${action.getText('ON')}" data-off-label="${action.getText('OFF')}"><input type="checkbox" name="${setting.key}"<#if setting.value=='true'> checked="checked"</#if>></div></div>
	</div>
	</li>
	</#list>
	</ul>
</div>
<hr/>
</#if>
</#if>
</body>
</html></#escape>
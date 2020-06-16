<#ftl output_format='HTML'>
<!DOCTYPE html>
<html>
<head>
<title>${getText('scheduled')}${getText('console')}</title>
<script>
$(function(){
		$('#scheduled input:checkbox').change(function(e){
			var t = this;
			$.post('${actionBaseUrl}/shortCircuit',
								{
								task : t.name,
								shortCircuit: !t.checked
								}
								,function(data){
									if(data && data.actionErrors){
										$(t).closest('.switch').bootstrapSwitch('toggleState');
										alert(data.actionErrors[0]);
										return;
									}
								});
		});
		$('.btn.all').click(function(){
			var selector = '#scheduled input:checkbox:' + ($(this).hasClass('disable') ? 'checked' : 'not(:checked)');
			$.alerts({
				type : 'confirm',
				callback : function(b) {
					if (b) 
						$(selector).click();
				}
			});
		});
});
</script>
</head>
<body>
<div class="row<#if fluidLayout>-fluid</#if> center">
	<#list ['disable','enable'] as var>
	<button class="btn all ${var}">${getText(var)}${getText('all')}</button>
	</#list>
</div>
<div id="scheduled">
	<style>
	li.task{
		margin-top: 50px;
		line-height: 30px;
	}
	div.taskname{
		text-align: right;
		font-weight: bold;
	}
	</style>
	<ul class="thumbnails">
	<#list tasks as task>
	<li class="span6 task">
	<div class="row-fluid">
		<div class="span5 taskname">${getText(task.name)}</div>
		<div class="span3"><input type="checkbox" name="${task.name}"<#if !circuitBreaker??> disabled</#if><#if !circuitBreaker?? || !circuitBreaker.isShortCircuit(task.name)> checked="checked"</#if> class="switch switch-round input-small"/></div>
		<div class="span4">${task.type}: ${task.description!}</div>
	</div>
	</li>
	</#list>
	</ul>
</div>

</body>
</html>
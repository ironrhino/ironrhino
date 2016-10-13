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
});
</script>
</head>
<body>

<div id="scheduled">
	<style scoped>
	div.task{
		text-align: right;
		line-height: 30px;
		font-weight: bold;
	}
	</style>
	<ul class="thumbnails">
	<#list tasks as task>
	<li class="span6">
	<div class="row-fluid">
	<div class="span7 task">${getText(task)}</div>
	<div class="span5"><div class="switch" data-on-label="${getText('ON')}" data-off-label="${getText('OFF')}"><input type="checkbox" name="${task}"<#if !circuitBreaker??> disabled</#if><#if !circuitBreaker?? || !circuitBreaker.isShortCircuit(task)> checked="checked"</#if>></div></div>
	</div>
	</li>
	</#list>
	</ul>
</div>

</body>
</html>
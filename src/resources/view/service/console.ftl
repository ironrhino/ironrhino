<#ftl output_format='HTML'>
<!DOCTYPE html>
<html>
<head>
<title>${getText('service')}${getText('console')}</title>
<style>
<#assign borderColor='rgba(82, 168, 236, 0.8)'>
.services >.thumbnails > li {
	position: relative;
}
.services >.thumbnails > li > button.open {
	border-left-color: ${borderColor};
	border-top-color: ${borderColor};
	border-right-color: ${borderColor};
	border-radius: 2px 2px 0 0;
}
.services >.thumbnails > li > table {
	position: absolute;
	z-index: 1000;
	background-color: #fff;
	border-color: ${borderColor};
	border-top-width: 0;
	border-radius: 0 0 2px 2px;
}
button.open + table > thead:first-child > tr:first-child > th:first-child {
	border-top-left-radius: 0px;
}
button.open + table > thead:first-child > tr:first-child > th:last-child {
	border-top-right-radius: 0px;
}
button.open + table > thead > tr > th:first-child, button.open + table > tbody > tr > td:first-child {
	border-left-color: ${borderColor};
}
</style>
<script>
$(function(){
$('.service').click(function(e){
	$('button.open').filter(function(){return this != e.target;}).click();
	var t = $(this).toggleClass('open');
	if(!t.hasClass('open')){
		t.next('table.hosts').remove();
	}else{
		var url = '${actionBaseUrl}/hosts/'+t.text();
		$.getJSON(url,function(data){
			var table = $('<table class="table table-bordered hosts"><thead><tr><th style="width:50%;">Exported By:</th></tr></head><tbody></tbody></table>').insertAfter(t);
			$.each(data,function(){
				$('<tr><td class="provider middle">'+this+'</td></tr>').appendTo(table.find('tbody'));
			});
		});
	}
});
$(document).click(function(e){
	var target = $(e.target);
	if(target.closest('button.service,table.hosts').length)
		return;
	$('button.open').click();
});
});
</script>
</head>
<body>

<div class="center">
<strong>${serviceRegistry.localHost}</strong>
</div>
<hr/>
<#list serviceRegistry.getAllAppNames() as appName>
<#assign services = serviceRegistry.getExportedServicesByAppName(appName)>
<#if services?has_content>
<h3 class="center">${appName}</h3>
<div class="services">
	<ul class="thumbnails">
	<#list services as service,description>
	<li class="span6<#if description?has_content> poped</#if>"<#if description?has_content> data-placement="bottom" data-content="${description}"</#if>>
	<button type="button" class="btn btn-block service">
	${service}
	<@stageConditional value="PRODUCTION" negated=true>
	<#if beans['servicePlayground'].services?seq_contains(service)>
	<a href="${actionNamespace}/playground/${service}" target="_blank"><span class="glyphicon glyphicon-align-justify pull-right" style="margin-right:5px;"></span></a>
	</#if>
	</@stageConditional>
	</button>
	</li>
	</#list>
	</ul>
</div>
<hr/>
</#if>
</#list>
</body>
</html>
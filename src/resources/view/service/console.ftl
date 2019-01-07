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
			var table = $('<table class="table table-bordered hosts"><thead><tr><th style="width:50%;">Exported By:</th><th>Imported By:</th></tr></head><tbody></tbody></table>').insertAfter(t);
			$.each(data,function(k,v){
				var row = $('<tr><td class="provider middle"></td><td class="consumers"></td></tr>').appendTo(table.find('tbody'));
				row.find('.provider').html('<a class="host" href="#">'+k+'</a>');
				if(v.length){
					var ul = $('<ul class="unstyled"/>').appendTo(row.find('.consumers'));
					$.each(v,function(i,val){
						$('<li/>').appendTo(ul).html('<a class="host" href="#">'+val+'</a>');
					});
				}
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
$(document).on('click','a.host',function(e){
	$('#imported-services').remove();
	var t = $(this);
	var host = t.text();
	var url = '${actionBaseUrl}/services/'+host;
	$.getJSON(url,function(data){
		$('<div id="imported-services"/>').insertAfter($('hr:last')).prepend('<h4 class="center">Imported by '+host+'</h4>');
		var ul = $('<ul class="thumbnails"/>').appendTo($('#imported-services'));
		$.each(data,function(k,v){
			$('<li/>').addClass('span6').appendTo(ul).html(k+'<a class="host pull-right" href="#">'+v+'</a>');
		});	
		if(!$('#imported-services li').length){
			$('#imported-services h4').text('No services imported by '+host);
		}
		$('html,body').animate({scrollTop : $('#imported-services').offset().top - 50}, 100);
	});
	return false;
});
});
</script>
</head>
<body>

<div class="center">
<a class="host" href="#"><strong>${serviceRegistry.localHost}</strong></a>
</div>
<hr/>
<#list serviceRegistry.getAllAppNames() as appName>
<#assign services = serviceRegistry.getExportedServicesByAppName(appName)>
<#if services?size gt 0>
<h3 class="center">${appName}</h3>
<div class="services">
	<ul class="thumbnails">
	<#list services as service,description>
	<li class="span6<#if description?has_content> poped</#if>"<#if description?has_content> data-placement="bottom" data-content="${description}"</#if>>
	<button type="button" class="btn btn-block service">
	${service}
	<@stageConditional value="DEVELOPMENT">
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
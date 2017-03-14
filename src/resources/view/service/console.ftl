<#ftl output_format='HTML'>
<!DOCTYPE html>
<html>
<head>
<title>${getText('service')}${getText('console')}</title>
<script>
$(function(){
$('.service').click(function(){
	var t = $(this);
	if(t.siblings().length){
		t.siblings().remove();
	}else{
		var url = '${actionBaseUrl}/hosts/'+t.text();
		$.getJSON(url,function(data){
			var div = $('<div class="row-fluid"><div class="exported span6"><h5 style="text-align:center;">Exported By:</h5></div><div class="imported span6"><h5 style="text-align:center;">Imported By:</h5></div></div>').insertAfter(t);
			var ul = $('<ul class="thumbnails"/>').appendTo(div.find('.exported'));
			$.each(data.exported,function(i,v){
				$('<li/>').appendTo(ul).html('<a class="host" href="#">'+v+'</a>');
			});
			ul = $('<ul class="thumbnails"/>').appendTo(div.find('.imported'));
			$.each(data.imported,function(i,v){
				$('<li/>').appendTo(ul).html('<a class="host" href="#">'+v+'</a>');
			});
		});
	}
});
$(document).on('click','a.host',function(e){
	$('#imported-services').remove();
	var t = $(this);
	var host = t.text();
	var url = '${actionBaseUrl}/services/'+host;
	$.getJSON(url,function(data){
		$('<div id="imported-services"/>').insertAfter($('hr:last')).prepend('<h4 style="text-align:center;">Imported by '+host+'</h4>');
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

<div class="row<#if fluidLayout>-fluid</#if>" style="margin-bottom: 20px;">
<div style="text-align:center;">
<a class="host" href="#"><strong>${serviceRegistry.localHost}</strong></a>
</div>
</div>
<hr/>
<#list serviceRegistry.getAllAppNames() as appName>
<#assign services = serviceRegistry.getExportedServices(appName)>
<#if services?size gt 0>
<h3 style="text-align:center;">${appName}</h3>
<div class="services">
	<ul class="thumbnails">
	<#list services as service,description>
	<li class="span6<#if description?has_content> poped</#if>"<#if description?has_content> data-placement="bottom" data-content="${description}"</#if>>
	<button type="button" class="btn btn-block service">${service}</button>
	</li>
	</#list>
	</ul>
</div>
<hr/>
</#if>
</#list>
</body>
</html>
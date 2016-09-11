<#ftl output_format='HTML'>
<!DOCTYPE html>
<html>
<head>
<title>${action.getText('service')}${action.getText('console')}</title>
<script>
$(function(){
$('.service').click(function(){
	var t = $(this);
	if(t.siblings().length){
		t.siblings().remove();
	}else{
		var url = '${actionBaseUrl}/hosts/'+t.text();
		$.getJSON(url,function(data){
			if(!data.length){
				alert('no active providers for this service');
				return;
			}
			var ul = $('<ul class="thumbnails"/>').insertAfter(t);
			$.each(data,function(i,v){
				$('<li class="span3"/>').appendTo(ul).html('<a class="host" href="#">'+v+'</a>');
			});
			$('<h5 style="text-align:center;">Exported By:</h5>').insertAfter(t);
		});
	}
});
$(document).on('click','a.host',function(e){
	$('#discovered-services').remove();
	var t = $(this);
	var host = t.text();
	var url = '${actionBaseUrl}/services/'+host;
	$.getJSON(url,function(data){
		$('<div id="discovered-services"/>').insertAfter($('hr')).prepend('<h4 style="text-align:center;">Imported By: '+host+'</h4>');
		var ul = $('<ul class="thumbnails"/>').appendTo($('#discovered-services'));
		$.each(data,function(k,v){
			$('<li/>').addClass('span6').appendTo(ul).html(k+'<a class="host pull-right" href="#">'+v+'</a>');
		});	
		if(!$('#discovered-services li').length){
			$('#discovered-services').remove();
			alert('no discovered services');
		}
	});
	return false;
});
});
</script>
</head>
<body>

<div class="row<#if fluidLayout>-fluid</#if>" style="margin-bottom: 20px;">
<div class="span4 offset4">
<a class="host btn btn-block" href="#"><strong>${serviceRegistry.localHost}</strong></a>
</div>
</div>
<#assign services = serviceRegistry.getAllServices()>
<#if services?size gt 0>
<div id="services">
	<ul class="thumbnails">
	<#list services as service>
	<li class="span6">
	<button type="button" class="btn btn-block service">${service}</button>
	</li>
	</#list>
	</ul>
</div>
<hr/>
</#if>
</body>
</html>
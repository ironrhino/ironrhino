<#assign view=Parameters.view!/>
<!DOCTYPE html>
<#escape x as x?html><html>
<head>
<title><#if page.new>${action.getText('create')}<#else>${action.getText('edit')}</#if>${action.getText('page')}</title>
<meta name="cms_path" content="${cmsPath}" />
<script type="text/javascript" src="<@url value="/assets/components/tinymce/tinymce.min.js"/>"></script>
<script type="text/javascript" src="<@url value="/assets/components/tinymce/ironrhino.tinymce.js"/>"></script>
<script type="text/javascript">
$(function() {
		var cmsPath= $('meta[name="cms_path"]').attr('content') || '';
		var options = {
			relative_urls: false,
		    selector: "#page_content",
		    content_css : '<#if Parameters.content_css?has_content>${Parameters.content_css}<#else><@url value="/assets/styles/ironrhino-min.css"/></#if>',
		    plugins: [
		        "advlist autolink lists link image charmap print preview anchor textcolor",
		        "searchreplace visualblocks code fullscreen",
		        "insertdatetime media table contextmenu paste"
		    ],
		    toolbar: "insertfile undo redo | styleselect | forecolor backcolor | fontselect fontsizeselect | bold italic | alignleft aligncenter alignright alignjustify | bullist numlist outdent indent | link image"
		};
		if(MessageBundle && MessageBundle.lang()!='en')
			options.language=MessageBundle.lang();
		tinymce.init(options);
		setTimeout(function(){
		var ed = tinymce.EditorManager.get('page_content');
		$('#draft').click(function(){
			if(!$('#page_input').hasClass('dirty')&&!ed.isDirty()) return false;
			var action = $('#page_input').attr('action');
			$('#page_input').attr('action',action.substring(0,action.lastIndexOf('/')+1)+'draft');
			ed.save();
		});
		$('#save').click(function(){
			var action = $('#page_input').attr('action');
			$('#page_input').attr('action',action.substring(0,action.lastIndexOf('/')+1)+'save');
			ed.save();
		});
		var form = $('#page_input');
		form[0].onsuccess = function(){
		$('#page_input').removeClass('dirty');
			ed.isNotDirty = 1;
			if($('#page_input').attr('action').indexOf('save')>0 && window.parent!=window){
					var win = $('.window-pop:last',window.parent.document).closest('.ui-dialog');
					if(!win.length)
						win = $('.window-richtable:last',window.parent.document).closest('.ui-dialog');
					if(win.length){
						$('.ui-dialog-titlebar-close',win).click();
						return;
					}
				}
			var page = Ajax.jsonResult.page;
			if(page){
				var date = page.draftDate;
				var path = page.path;
				$('#page_id').val(page.id);
				$('#page_path').val(path);
				path = CONTEXT_PATH+cmsPath+path;
				if(date){
					//save draft 
					$('.draft').show();
					$('.draftDate').text(date);
					$('#preview').attr('href',path+'?preview=true');
				}else{
					//save
					$('.draft').hide();
					$('#view').attr('href',path);
				}
			}else{
				document.location.href=document.location.href;
			}
		};
		setInterval(function(){
			if(($('#page_input').hasClass('dirty')||ed.isDirty())&&$('#page_path').val()&&$('#page_content').val()){
				var action = $('#page_input').attr('action');
				$('#page_input').attr('action',action.substring(0,action.lastIndexOf('/')+1)+'draft');
				ed.save();
				form.submit();
				}
		},60000);
		},1500);
		$('input[name="page.path"],input[name="page.title"]').keyup(function(){
			$('#page_input').addClass('dirty');
		});
		$('#drop').click(function(){
			var form = $('#page_input');
			var action = $('#page_input').attr('action');
			$('#page_input').attr('action',action.substring(0,action.lastIndexOf('/')+1)+'drop');
			$('#page_content').text('');
			form[0].onsuccess=function(){
				document.location.href = document.location.href;
			};
			}
		);
});

</script>
</head>
<body>
	<#assign pageContentDynamicAttributes={}>
	<@classPresentConditional value="org.ironrhino.common.action.UploadAction">
	<#assign pageContentDynamicAttributes={'data-uploadurl':'/common/upload'}>
	</@classPresentConditional>
<@s.form id="page_input" action="${actionBaseUrl}/${view?has_content?string('save','draft')}" method="post" class="ajax form-horizontal" style="padding-top:13px;">
	<@s.hidden name="page.id" />
	<@s.hidden name="page.version" class="version" />
	<#if view=='embedded'>
	<@s.hidden name="page.path"/>
	<@s.hidden name="page.displayOrder"/>
	<@s.hidden name="page.tags" value="${(page.tags?join(','))}"/>
	<@s.hidden name="page.head"/>
	<@s.hidden name="page.title"/>
	<@s.textarea theme="simple" id="page_content" label="%{getText('content')}" labelposition="top" name="page.content" style="width:100%;height:320px;" dynamicAttributes=pageContentDynamicAttributes/>
	<#elseif view=='brief'>
	<@s.hidden name="page.path"/>
	<@s.hidden name="page.displayOrder"/>
	<@s.hidden name="page.tags" value="${(page.tags?join(','))}"/>
	<@s.hidden name="page.head"/>
	<@s.textfield label="%{getText('title')}" name="page.title" style="width:600px;"/>
	<@s.textarea theme="simple" id="page_content" label="%{getText('content')}" labelposition="top" name="page.content" style="width:600px;height:320px;" dynamicAttributes=pageContentDynamicAttributes/>
	<#else>
	<ul class="nav nav-tabs">
		<li class="active"><a href="#_page_base" data-toggle="tab">${action.getText('base')}</a></li>
		<li><a href="#_page_content" data-toggle="tab">${action.getText('content')}</a></li>
		<li><a href="#_page_head" data-toggle="tab">${action.getText('head')}</a></li>
	</ul>
	<div class="tab-content">
	<div id="_page_base" class="tab-pane active">
	<@s.textfield id="page_path" label="%{getText('path')}" readonly=!page.new name="page.path" class="required checkavailable" style="width:600px;"/>
	<@s.textfield label="%{getText('displayOrder')}" name="page.displayOrder" type="number" class="integer"/>
	<@s.textfield label="%{getText('tags')}" name="page.tags"  class="tags" data\-source="${actionBaseUrl}/suggest" style="width:600px;"/>
	<@s.textfield label="%{getText('title')}" name="page.title" style="width:600px;"/>
	</div>
	<div id="_page_content" class="tab-pane">
	<@s.textarea theme="simple" id="page_content" label="%{getText('content')}" labelposition="top" name="page.content" style="width:800px;height:260px;" dynamicAttributes=pageContentDynamicAttributes/>
	</div>
	<div id="_page_head" class="tab-pane">
	<@s.textarea theme="simple" id="page_head" name="page.head" style="width:800px;height:300px;"/>
	</div>
	</div>
	</#if>
	<div class="form-actions">
	<@s.submit id="save" value="%{getText('save')}" theme="simple" class="btn-primary"/>
	
	<#if view!='embedded'>
	<#if page?? && !page.new>
	<@s.submit id="draft" value="%{getText('draft')}" theme="simple"/>
	</#if>
	<span class="draft" <#if !draft>style="display: none;"</#if>>
	${action.getText('draftDate')}:<span class="draftDate"><#if page.draftDate??>${page.draftDate?datetime}</#if></span>
	<#if page.id??>
	<a class="btn" id="preview" href="${getUrl(cmsPath+page.path)}?preview=true" target="_blank">${action.getText('preview')}</a>
	<#else>
	<a class="btn" id="preview" target="_blank">${action.getText('preview')}</a>
	</#if>
	<@s.submit id="drop" value="%{getText('drop')}" theme="simple"/>
	</span>
	<#if page.id??>
	<a class="btn" id="view" href="${getUrl(cmsPath+page.path)}" target="_blank">${action.getText('view')}</a>
	<#else>
	<a class="btn" id="view" target="_blank">${action.getText('view')}</a>
	</#if>
	</#if>
	</div>
	
</@s.form>
</body>
</html></#escape>
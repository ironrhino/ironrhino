<#ftl output_format='HTML'>
<!DOCTYPE html>
<html>
<head>
<title>${getText('qrcode')}</title>
<script type="text/javascript" src="<@url value="/assets/components/decodeqrcode/decodeqrcode-min.js"/>"></script>
</head>
<body>
<ul class="nav nav-tabs">
	<li class="active"><a href="#decode" data-toggle="tab">${getText('decode')}</a></li>
	<li><a href="#encode" data-toggle="tab">${getText('encode')}</a></li>
</ul>
<div class="tab-content">
	<div id="decode" class="tab-pane active">
		<@s.form id="qrcode_form" action="${actionBaseUrl}" method="post" enctype="multipart/form-data" class="form-horizontal">
		<@s.hidden name="decode" value="true"/>
		<@s.textfield id="decoded-content" name="content" class="input-xxlarge">
		<@s.param name="after"><button type="button" class="btn decodeqrcode" data-target="#decoded-content"><i class="glyphicon glyphicon-screenshot"></i></button></@s.param>
		</@s.textfield>
		<@s.file label=getText('qrcode') name="file" class="custom" accept="image/*"/>
		<@s.textfield name="url" class="input-xxlarge"/>
		<@s.textfield name="encoding" class="input-small"/>
		<@s.submit value=getText('confirm') />
		</@s.form>
	</div>
	<div id="encode" class="tab-pane">
		<@s.form id="qrcode_form" action="${actionBaseUrl}" method="post" enctype="multipart/form-data" class="form-horizontal" target="_blank">
		<@s.textfield name="content" value="" class="input-xxlarge"/>
		<@s.textfield name="encoding" class="input-small"/>
		<@s.textfield type="number" name="width" class="integer positive" min="10"/>
		<@s.textfield type="number" name="height" class="integer positive" min="10"/>
		<@s.file label=getText('watermark') name="file" class="custom" accept="image/*"/>
		<@s.submit value=getText('confirm') />
		</@s.form>
	</div>
</div>
</body>
</html>



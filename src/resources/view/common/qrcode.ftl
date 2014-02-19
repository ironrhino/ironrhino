<!DOCTYPE html>
<#escape x as x?html><html>
<head>
<title>${action.getText('qrcode')}</title>
<script type="text/javascript" src="<@url value="/assets/components/decodeqrcode/decodeqrcode-min.js"/>"></script>
</head>
<body>
<ul class="nav nav-tabs">
	<li class="active"><a href="#decode" data-toggle="tab">${action.getText('decode')}</a></li>
	<li><a href="#encode" data-toggle="tab">${action.getText('encode')}</a></li>
</ul>
<div class="tab-content">
	<div id="decode" class="tab-pane active">
		<@s.form id="qrcode_form" action="qrcode" method="post" enctype="multipart/form-data" cssClass="form-horizontal">
		<@s.hidden name="decode" value="true"/>
		<@s.textfield id="decoded-content" label="%{getText('content')}" name="content" cssClass="input-xxlarge">
		<@s.param name="after"><button type="button" class="btn decodeqrcode" data-target="#decoded-content"><i class="glyphicon glyphicon-screenshot"></i></button></@s.param>
		</@s.textfield>
		<@s.file label="%{getText('qrcode')}" name="file"/>
		<@s.textfield label="%{getText('url')}" name="url" cssClass="input-xxlarge"/>
		<@s.textfield label="%{getText('encoding')}" name="encoding" cssClass="input-small"/>
		<@s.submit value="%{getText('confirm')}" />
		</@s.form>
	</div>
	<div id="encode" class="tab-pane">
		<@s.form id="qrcode_form" action="qrcode" method="post" enctype="multipart/form-data" cssClass="form-horizontal" target="_blank">
		<@s.textfield label="%{getText('content')}" name="content" value="" cssClass="input-xxlarge"/>
		<@s.textfield label="%{getText('encoding')}" name="encoding" cssClass="input-small"/>
		<@s.textfield label="%{getText('width')}" type="number" name="width" cssClass="integer positive" min="10"/>
		<@s.textfield label="%{getText('height')}" type="number" name="height" cssClass="integer positive" min="10"/>
		<@s.file label="%{getText('watermark')}" name="file"/>
		<@s.submit value="%{getText('confirm')}" />
		</@s.form>
	</div>
</div>
</body>
</html></#escape>



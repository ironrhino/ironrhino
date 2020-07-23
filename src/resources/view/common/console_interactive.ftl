<#ftl output_format='HTML'>
<!DOCTYPE html>
<html>
<head>
<title>${getText('console')}</title>
<link href="<@url value="/assets/components/console/jquery.console.css"/>" media="all" rel="stylesheet" type="text/css">
<script src="<@url value="/assets/components/console/jquery.console.js"/>" type="text/javascript"></script>
<script>
$(function() {
	$('.console').console({
		promptLabel: '> ',
		commandValidate: function(line) {
			if (!line)
				return false;
			return true;
		},
		commandHandle: function(line, report) {
			$.ajax({
				type: 'POST',
				global: false,
				url: '${actionBaseUrl}/executeJson',
				data: {
					expression: line
				},
				success: function(data) {
					var messages = [];
					if (data) {
						if (data.actionErrors) {
							$.each(data.actionErrors, function() {
								messages.push({
									msg: this,
									className: 'jquery-console-message-error'
								});
							});
						} else {
							messages.push({
								msg: ((typeof data == 'object') ? JSON.stringify(data) : data),
								className: 'jquery-console-message-value'
							});
						}
					} else {
						messages.push({
							msg: 'null',
							className: 'jquery-console-message-type'
						});
					}
					report(messages);
				}
			});
		},
		autofocus: true,
		animateScroll: true,
		promptHistory: true
	});
});
</script>
</head>
<body>
<div class="console"></div>
</body>
</html>
Initialization.apiplayground = function() {
	$(document).on('submit', 'form.api-playground', function(e) {
		if (!Form.validate(e.target))
			return false;
		var form = $(e.target);
		var params = [];
		form.find('table.requestParams tr').each(function(i, v) {
			var row = $(this);
			var name = row.find('input:eq(1)').attr('name')
					|| row.find('input:eq(0)').val();
			var value = row.find('input:eq(1)').val();
			if (name)
				params.push(encodeURIComponent(name) + '='
						+ encodeURIComponent(value));
		});
		var headers = {};
		form.find('table.requestHeaders tr').each(function(i, v) {
			var row = $(this);
			var name = row.find('input:eq(1)').attr('name')
					|| row.find('input:eq(0)').val();
			var value = row.find('input:eq(1)').val();
			if (name)
				headers[name] = value;
		});
		var accessToken = form.find('[name="accessToken"]').val();
		if (accessToken) {
			headers['Authorization'] = 'Bearer ' + accessToken;
			localStorage.setItem('accessToken', accessToken);
		}
		var url = form.find('.url').text();
		if (params.length)
			url += '?' + params.join('&');
		var startTime = new Date().getTime();
		var options = {
			global : false,
			url : url,
			method : form.attr('method'),
			headers : headers,
			dataType : 'text',
			beforeSend : function() {
				if (typeof $.fn.mask != 'undefined')
					form.mask();
				else
					form.addClass('loading');
			},

			complete : function(xhr) {
				if (typeof $.fn.mask != 'undefined')
					form.unmask();
				else
					form.removeClass('loading');
				form.find('.responseStatus').html(xhr.status + ' '
						+ xhr.statusText + ' <span class="badge">'
						+ (new Date().getTime() - startTime) + 'ms</span>');
				form.find('.responseHeaders').text(xhr.getAllResponseHeaders());
				form.find('.responseBody').text(xhr.responseText);
			}
		};
		if (form.find('.requestBody').length) {
			options.data = form.find('.requestBody').text();
			options.contentType = 'application/json; charset=UTF-8';
		}
		$.ajax(options);
		return false;
	}).on('click', 'form.api-playground .last-accessToken', function() {
				var accessToken = localStorage.getItem('accessToken');
				if (!accessToken)
					alert("Not Found");
				else
					$(this).prev('[name="accessToken"]').val(accessToken);
			}).on('click', 'form.api-playground .fetch-accessToken',
			function() {
				$(this).next('.fetch-accessToken-form').toggle();
			}).on('change',
			'form.api-playground .fetch-accessToken-form [name="grant_type"]',
			function() {
				var div = $(this).closest('.fetch-accessToken-form')
						.find('.grant-type-password');
				if ('password' == $(this).val()) {
					div.show();
				} else {
					div.hide();
				}
			}).on('click',
			'form.api-playground .fetch-accessToken-form button', function() {
				var f = $(this).closest('.fetch-accessToken-form');
				var valid = true;
				var data = {};
				var fields = f.find(':input:visible');
				for (var i = 0; i < fields.length; i++) {
					var field = fields[i];
					if (!Form.validate(field))
						valid = false;
					else if (field.name)
						data[field.name] = field.value;
				}
				if (!valid)
					return;
				var options = {
					global : false,
					url : f.data('endpoint'),
					method : 'GET',
					data : data,
					beforeSend : function() {
						if (typeof $.fn.mask != 'undefined')
							f.mask();
						else
							f.addClass('loading');
					},
					success : function(data) {
						if (data.access_token)
							f.closest('.api-playground')
									.find('[name="accessToken"]')
									.val(data.access_token);
						f.hide();
					},
					complete : function(xhr) {
						if (typeof $.fn.mask != 'undefined')
							f.unmask();
						else
							f.removeClass('loading');
						if (xhr.status != 200)
							alert(xhr.responseText);
					}
				};
				$.ajax(options);
			});
};
Observation.formatJson = function(container) {
	$$('code.json.block').each(function() {
				var t = $(this);
				if (t.text()) {
					try {
						t
								.text(JSON.stringify(JSON.parse(t.text()),
										null, '   '));
					} catch (e) {

					}
				}
			});
}

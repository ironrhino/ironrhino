Initialization.apiplayground = function() {
	$(document).on('submit', 'form.api-playground', function(e) {
		if (!Form.validate(e.target))
			return false;
		var form = $(e.target);
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
		var method = form.attr('method');
		var data;
		var contentType = false;
		var processData = true;
		if (form.find('table.requestParams input[type="file"]').length) {
			var formdata = new FormData();
			form.find('table.requestParams tr').each(function(i, v) {
						var row = $(this);
						var input = row.find('input:eq(1)');
						var name = input.attr('name')
								|| row.find('input:eq(0)').val();
						if (!name)
							return;
						if (input.attr('type') == 'file') {
							var files = input[0].files;
							for (var n = 0; n < files.length; n++)
								formdata.append(name, files[n]);
						} else {
							formdata.append(name, input.val());
						}

					});
			data = formdata;
			processData = false;
		} else {
			if (form.find('.requestBody').length) {
				data = form.find('.requestBody').text();
				contentType = 'application/json; charset=UTF-8';
			}
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
			params = params.join('&');
			if (params.length) {
				if (!data && method.indexOf('P') == 0) {
					data = params;
					contentType = 'application/x-www-form-urlencoded; charset=UTF-8';
				} else {
					url += (url.indexOf('?') > 0 ? '&' : '?') + params;
				}
			}
		}
		var startTime = new Date().getTime();
		var options = {
			global : false,
			url : url,
			data : data,
			contentType : contentType,
			processData : processData,
			method : method,
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
				var responseText = xhr.responseText;
				if (responseText) {
					try {
						responseText = JSON.stringify(JSON.parse(responseText),
								null, '   ');
					} catch (e) {
					}
				}
				form.find('.responseBody').text(responseText);
			}
		};

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
						t.text(JSON
								.stringify(JSON.parse(t.text()), null, '   '));
					} catch (e) {
					}
				}
			});
}

$(document).ajaxSuccess(function(ev, xhr, ajaxOptions) {
	if (xhr.getResponseHeader('X-Double-Check')
		|| xhr.getResponseHeader('X-Current-Password')) {
		var dc = xhr.getResponseHeader('X-Double-Check');
		var modal = $('<div class="modal"><div class="modal-header"><a class="close" data-dismiss="modal"/><h3 style="text-align:center;">'
			+ MessageBundle.get(dc ? 'double.check' : '&nbsp;')
			+ '</h3></div><div class="modal-body"><form class="form-horizontal"><fieldset><div class="form-actions"><button type="submit" class="btn btn-primary">'
			+ MessageBundle.get('confirm')
			+ '</button> <button type="button" class="btn dialog-close">'
			+ MessageBundle.get('cancel')
			+ '</button></div></fieldset></form></div></div>')
			.appendTo(topDocument.body);
		if (dc) {
			modal
				.find('fieldset')
				.prepend('<div class="control-group"><label class="control-label" for="doubleCheckUsername">'
					+ MessageBundle.get('double.check.username')
					+ '</label><div class="controls"><input id="doubleCheckUsername" type="text" name="doubleCheckUsername" class="required" autocomplete="off"></div></div><div id="cg-doubleCheckPassword" class="control-group"><label class="control-label" for="doubleCheckPassword">'
					+ MessageBundle.get('double.check.password')
					+ '</label><div class="controls"><input id="doubleCheckPassword" type="password" name="doubleCheckPassword" class="required input-pattern sha submit" autocomplete="off"></div></div>')
				.find('[name="doubleCheckUsername"]').change(function() {
					var t = $(this);
					var username = t.val();
					$('#cg-doubleCheckPassword').show().find('input').prop('disabled', false);
					$.get(CONTEXT_PATH + '/login/verificationCodeRequirement', { username: username }, function(data) {
						var cg = modal.find('#cg-verificationCode');
						if (data && data.required) {
							if (data.passwordHidden) {
								$('#cg-doubleCheckPassword').hide().find('input').prop('disabled', true);
							}
							if (!cg.length) {
								cg = $('<div id="cg-verificationCode" class="control-group"><label class="control-label" for="doubleCheckVerificationCode">'
									+ MessageBundle.get('verification.code')
									+ '</label><div class="controls"><input type="text" id="doubleCheckVerificationCode" name="verificationCode" class="required input-small" maxlength="' + data.length + '"> </div></div>').insertAfter(
										t.closest('fieldset').find('[name="doubleCheckPassword"]').closest('.control-group')
									);
								if (data.sendingRequired) {
									$('<button type="button" class="btn input-mini sendVerificationCode" data-username="doubleCheckUsername" data-url="' + CONTEXT_PATH + '/login/sendVerificationCode" data-interval="' + data.resendInterval + '">'
										+ MessageBundle.get('send')
										+ '</button>').appendTo(cg.find('.controls'));
								}
							}
						} else {
							cg.remove();
						}
					});
				});

		} else {
			modal
				.find('fieldset')
				.prepend('<div class="control-group"><label class="control-label" for="currentPassword">'
					+ MessageBundle.get('current.password')
					+ '</label><div class="controls"><input id="currentPassword" type="password" name="currentPassword" class="required input-pattern sha submit" autocomplete="off"></div></div>');
		}
		_observe(modal);
		modal.on('shown', function() {
			$('.modal-backdrop').insertAfter(this);
		}).on('hidden', function() {
			$(this).remove();
		}).modal('show');
		$('form', modal).submit(function() {
			if (!Form.validate(this))
				return false;
			var extra = {};
			$(this).find('input').each(function() {
				extra[this.name] = $.fieldValue(this);
			});
			var data = ajaxOptions.data;
			if (data) {
				var values = [];
				var arr = data.split('&');
				for (var i = 0; i < arr.length; i++) {
					var pair = arr[i];
					var exists = false;
					for (var key in extra) {
						if (pair.indexOf(key + '=') == 0) {
							exists = true;
							break;
						}
					}
					if (!exists)
						values.push(pair);
				}
				data = values.join('&');
				if (data)
					data += '&';
				data += $.param(extra, $.ajaxSettings.traditional);
			} else {
				data = $.param(extra, $.ajaxSettings.traditional);
			}
			ajaxOptions.data = data;
			ajaxOptions.type = 'POST';
			var success = ajaxOptions.success;
			ajaxOptions.success = function(data, textStatus, xhr) {
				if (data.fieldErrors) {
					for (var key in extra) {
						var error = data.fieldErrors[key];
						if (error) {
							Message.showFieldError(modal.find('[name="' + key
								+ '"]').get(0), error);
							return;
						}
					}
				}
				modal.find('a.close').click();
				if (success)
					success(data, textStatus, xhr);
			};
			$.ajax(ajaxOptions);
			return false;
		}).find('input:eq(0)').focus();
		return false;
	}
});

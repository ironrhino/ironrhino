;
(function($) {

	$.alerts = function(options) {
		if (typeof options == 'string')
			options = {
				message : options
			};
		var _options = {
			showConfirmButton : true
		}
		$.extend(_options, options);
		options = _options;
		var okButton = MessageBundle.get('ok') || 'OK';
		var cancelButton = MessageBundle.get('cancel') || 'Cancel';
		var type = options.type || 'info';
		var title = options.title;
		var message = options.message;
		var callback = options.callback;
		if (type == 'confirm') {
			var title = title || MessageBundle.get('select');
			var message = message || MessageBundle.get('confirm.action');
		} else if (type == 'success') {
			if (!options.timer)
				options.timer = 2000;
			options.showConfirmButton = false;
		}

		hide();
		overlay('show');

		var container = $('<div id="popup-container"><div class="popup-content"><div class="popup-icon"></div><div class="popup-message"></div></div></div>')
				.appendTo(topDocument.body);
		var popupMessage = container.find(".popup-message");
		if (title != null)
			container.prepend('<h1 class="popup-title">' + title + '</h1>');
		else
			container.find(".popup-title").remove();
		container.find(".popup-content").addClass(type);
		popupMessage.html(message);

		container.css({
					minWidth : container.outerWidth(),
					maxWidth : container.outerWidth()
				});

		reposition();
		maintainPosition(true);
		var glyphicon = 'info-sign';
		switch (type) {
			case 'info' :
			case 'warning' :
			case 'success' :
			case 'error' :
				if (type == 'success')
					glyphicon = 'ok-circle';
				else if (type == 'error')
					glyphicon = 'remove-circle';
				else if (type == 'warning')
					glyphicon = 'warning-sign';
				if (options.showConfirmButton) {
					var popupOk = $('<div class="popup-panel"><button class="popup-ok">'
							+ okButton + '</button></div>')
							.insertAfter(popupMessage).find(".popup-ok");
					popupOk.click(function() {
								hide();
								if (callback)
									callback(true);
							}).focus();
				}
				if (options.timer) {
					var timer = setTimeout(function() {
								hide();
							}, options.timer);
					container.data('timer', timer);
				}
				break;
			case 'confirm' :
				glyphicon = 'question-sign';
				var popupPanel = $('<div class="popup-panel"><button class="popup-ok">'
						+ okButton
						+ '</button> <button class="popup-cancel">'
						+ cancelButton + '</button></div>')
						.insertAfter(popupMessage);
				var popupOk = popupPanel.find(".popup-ok");
				var popupCancel = popupPanel.find(".popup-cancel");
				popupOk.click(function() {
							hide();
							if (callback)
								callback(true);
						}).focus();
				popupCancel.click(function() {
							hide();
							if (callback)
								callback(false);
						});
				if (options.timer) {
					setTimeout(function() {
								popupOk.click();
							}, options.timer);
				}
				break;
			case 'prompt' :
				glyphicon = 'question-sign';
				popupMessage
						.append('<input type="text" size="30" class="popup-prompt" />')
						.after('<div class="popup-panel"><button class="popup-ok">'
								+ okButton
								+ '</button> <button class="popup-cancel">'
								+ cancelButton + '</button></div>');
				var popupPrompt = container.find(".popup-prompt");
				popupPrompt.width(popupMessage.width());
				var popupOk = container.find(".popup-ok");
				var popupCancel = container.find(".popup-cancel");
				popupOk.click(function() {
							var val = popupPrompt.val();
							hide();
							if (callback)
								callback(val);
						});
				popupCancel.click(function() {
							hide();
							if (callback)
								callback(null);
						});
				popupPrompt.keyup(function(e) {
							if (e.keyCode == 13)
								popupOk.trigger('click');
							if (e.keyCode == 27)
								popupCancel.trigger('click');
						});
				if (options.value)
					popupPrompt.val(options.value);
				if (options.inputPlaceholder)
					popupPrompt.attr('placeholder', options.inputPlaceholder);
				popupPrompt.focus().select();
				if (options.timer) {
					setTimeout(function() {
								popupOk.click();
							}, options.timer);
				}
				break;
		}

		container.find('.popup-icon')
				.append('<span class="glyphicon glyphicon-' + glyphicon
						+ '"></span>');

		// Make draggable
		if (title != null) {
			try {
				container.draggable({
							handle : $('.popup-title')
						});
				container.find('.popup-title').css({
							cursor : 'move'
						});
			} catch (e) { /* requires jQuery UI draggables */
			}
		}
		return container;
	}

	$.alerts.show = $.alerts;

	function hide() {
		var container = $('#popup-container', topDocument);
		if (container.length) {
			var timer = container.data('timer');
			if (timer)
				clearTimeout(timer);
			container.remove();
			overlay('hide');
			maintainPosition(false);
		}
	}

	function overlay(status) {
		switch (status) {
			case 'show' :
				overlay('hide');
				$('<div id="popup-overlay"></div>').appendTo(topDocument.body);
				break;
			case 'hide' :
				$('#popup-overlay', topDocument).remove();
				break;
		}
	}

	function reposition() {
		var container = $('#popup-container', topDocument);
		var top = (($(topWindow).height() / 2) - (container.outerHeight() / 2)) - 75;
		var left = (($(topWindow).width() / 2) - (container.outerWidth() / 2));
		if (top < 0)
			top = 0;
		if (left < 0)
			left = 0;
		container.css({
					top : top + 'px',
					left : left + 'px'
				});
	}

	function maintainPosition(status) {
		switch (status) {
			case true :
				$(topWindow).on('resize', reposition);
				break;
			case false :
				$(topWindow).off('resize', reposition);
				break;
		}
	}

})(jQuery);
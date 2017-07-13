// jQuery Alert Dialogs Plugin
//
// Version 1.1
//
// Cory S.N. LaViska
// A Beautiful Site (http://abeautifulsite.net/)
// 14 May 2009
//
// Visit http://abeautifulsite.net/notebook/87 for more information
//
// Usage:
// jAlert( message, [title, callback] )
// jConfirm( message, [title, callback] )
// jPrompt( message, [value, title, callback] )
// 
// History:
//
// 1.00 - Released (29 December 2008)
//
// 1.01 - Fixed bug where unbinding would destroy all resize events
//
// License:
// 
// This plugin is dual-licensed under the GNU General Public License and the MIT
// License and
// is copyright 2008 A Beautiful Site, LLC.
//
(function($) {

	$.alerts = {

		// These properties can be read/written by accessing
		// $.alerts.propertyName from your scripts at any time

		verticalOffset : -75, // vertical offset of the dialog from center
		// screen, in pixels
		horizontalOffset : 0, // horizontal offset of the dialog from center
		// screen, in pixels/
		repositionOnResize : true, // re-centers the dialog on window resize
		overlayOpacity : .6, // transparency level of overlay
		overlayColor : '#000', // base color of overlay
		draggable : true, // make the dialogs draggable (requires UI
		// Draggables plugin)
		okButton : '&nbsp;OK&nbsp;', // text for the OK button
		cancelButton : '&nbsp;Cancel&nbsp;', // text for the Cancel button

		show : function(options) {
			if (typeof options == 'string')
				options = {
					message : options
				};
			var _options = {
				showConfirmButton : true
			}
			$.extend(_options, options);
			options = _options;

			var type = options.type || 'info';
			var title = options.title;
			var message = options.message;
			var callback = options.callback;
			if (type == 'confirm') {
				var title = title || MessageBundle.get('select');
				var message = message || btn.data('confirm')
						|| MessageBundle.get('confirm.action');
			} else if (type == 'success') {
				if (!options.timer)
					options.timer = 2000;
				options.showConfirmButton = false;
			}

			$.alerts._hide();
			$.alerts._overlay('show');

			var popupContainer = $('<div id="popup-container"><div class="popup-content"><div class="popup-icon"></div><div class="popup-message"></div></div></div>')
					.appendTo(document.body);

			popupContainer.css({
						position : 'fixed',
						zIndex : 99999,
						padding : 0,
						margin : 0
					});

			var popupMessage = popupContainer.find(".popup-message");
			if (title != null)
				popupContainer.prepend('<h1 class="popup-title">' + title
						+ '</h1>');
			else
				popupContainer.find(".popup-title").remove();
			popupContainer.find(".popup-content").addClass(type);
			popupMessage.html(message);

			popupContainer.css({
						minWidth : popupContainer.outerWidth(),
						maxWidth : popupContainer.outerWidth()
					});

			$.alerts._reposition();
			$.alerts._maintainPosition(true);
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
								+ $.alerts.okButton + '</button></div>')
								.insertAfter(popupMessage).find(".popup-ok");
						popupOk.click(function() {
									$.alerts._hide();
									if (callback)
										callback(true);
								}).focus();
					}
					if (options.timer) {
						var timer = setTimeout(function() {
									$.alerts._hide();
								}, options.timer);
						popupContainer.data('timer', timer);
					}
					break;
				case 'confirm' :
					glyphicon = 'question-sign';
					var popupPanel = $('<div class="popup-panel"><button class="popup-ok">'
							+ $.alerts.okButton
							+ '</button> <button class="popup-cancel">'
							+ $.alerts.cancelButton + '</button></div>')
							.insertAfter(popupMessage);
					var popupOk = popupPanel.find(".popup-ok");
					var popupCancel = popupPanel.find(".popup-cancel");
					popupOk.click(function() {
								$.alerts._hide();
								if (callback)
									callback(true);
							}).focus();
					popupCancel.click(function() {
								$.alerts._hide();
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
									+ $.alerts.okButton
									+ '</button> <button class="popup-cancel">'
									+ $.alerts.cancelButton + '</button></div>');
					var popupPrompt = popupContainer.find(".popup-prompt");
					popupPrompt.width(popupMessage.width());
					var popupOk = popupContainer.find(".popup-ok");
					var popupCancel = popupContainer.find(".popup-cancel");
					popupOk.click(function() {
								var val = popupPrompt.val();
								$.alerts._hide();
								if (callback)
									callback(val);
							});
					popupCancel.click(function() {
								$.alerts._hide();
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
						popupPrompt.attr('placeholder',
								options.inputPlaceholder);
					popupPrompt.focus().select();
					if (options.timer) {
						setTimeout(function() {
									popupOk.click();
								}, options.timer);
					}
					break;
			}

			popupContainer.find('.popup-icon')
					.append('<span class="glyphicon glyphicon-' + glyphicon
							+ '"></span>');

			// Make draggable
			if ($.alerts.draggable && title != null) {
				try {
					popupContainer.draggable({
								handle : $(".popup-title")
							});
					popupContainer.find(".popup-title").css({
								cursor : 'move'
							});
				} catch (e) { /* requires jQuery UI draggables */
				}
			}
			return popupContainer;
		},

		_hide : function() {
			var popupContainer = $("#popup-container");
			if (popupContainer.length) {
				var timer = popupContainer.data('timer');
				if (timer)
					clearTimeout(timer);
				popupContainer.remove();
				$.alerts._overlay('hide');
				$.alerts._maintainPosition(false);
			}
		},

		_overlay : function(status) {
			switch (status) {
				case 'show' :
					$.alerts._overlay('hide');
					$('<div id="popup-overlay"></div>').appendTo(document.body)
							.css({
										position : 'absolute',
										zIndex : 99998,
										top : '0px',
										left : '0px',
										width : '100%',
										height : $(document).height(),
										background : $.alerts.overlayColor,
										opacity : $.alerts.overlayOpacity
									});
					break;
				case 'hide' :
					$("#popup-overlay").remove();
					break;
			}
		},

		_reposition : function() {
			var top = (($(window).height() / 2) - ($("#popup-container")
					.outerHeight() / 2))
					+ $.alerts.verticalOffset;
			var left = (($(window).width() / 2) - ($("#popup-container")
					.outerWidth() / 2))
					+ $.alerts.horizontalOffset;
			if (top < 0)
				top = 0;
			if (left < 0)
				left = 0;

			// IE6 fix
			if ($.browser.msie && parseInt($.browser.version) <= 6)
				top = top + $(window).scrollTop();

			$("#popup-container").css({
						top : top + 'px',
						left : left + 'px'
					});
			$("#popup-overlay").height($(document).height());
		},

		_maintainPosition : function(status) {
			if ($.alerts.repositionOnResize) {
				switch (status) {
					case true :
						$(window).bind('resize', $.alerts._reposition);
						break;
					case false :
						$(window).unbind('resize', $.alerts._reposition);
						break;
				}
			}
		}

	}

})(jQuery);
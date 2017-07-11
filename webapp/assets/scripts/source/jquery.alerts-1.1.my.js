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
		dialogClass : null, // if specified, this class will be applied to all
		// dialogs

		// Public methods

		info : function(message, title, callback) {
			$.alerts._show(title, message, null, 'info', function(result) {
						if (callback)
							callback(result);
					});
		},

		alert : function(message, title, callback) {
			$.alerts._show(title, message, null, 'alert', function(result) {
						if (callback)
							callback(result);
					});
		},

		confirm : function(message, title, callback) {
			$.alerts._show(title, message, null, 'confirm', function(result) {
						if (callback)
							callback(result);
					});
		},

		prompt : function(message, value, title, callback) {
			$.alerts._show(title, message, value, 'prompt', function(result) {
						if (callback)
							callback(result);
					});
		},

		// Private methods

		_show : function(title, msg, value, type, callback) {

			$.alerts._hide();
			$.alerts._overlay('show');

			var popupContainer = $('<div id="popup-container"><h1 class="popup-title"></h1><div class="popup-content"><div class="popup-icon"></div><div class="popup-message"></div></div></div>')
					.appendTo(document.body);

			popupContainer.find('.popup-icon')
					.html('<span class="glyphicon glyphicon-'
							+ (type == 'confirm' || type == 'alert'
									? 'alert'
									: type == 'prompt'
											? 'question-sign'
											: 'info-sign') + '"></span>');

			if ($.alerts.dialogClass)
				popupContainer.addClass($.alerts.dialogClass);

			// IE6 Fix
			var pos = ($.browser.msie && parseInt($.browser.version) <= 6)
					? 'absolute'
					: 'fixed';

			popupContainer.css({
						position : pos,
						zIndex : 99999,
						padding : 0,
						margin : 0
					});

			var popupMessage = popupContainer.find(".popup-message");
			if (title != null)
				popupContainer.find(".popup-title").html(title);
			else
				popupContainer.find(".popup-title").remove();
			popupContainer.find(".popup-content").addClass(type);
			popupContainer.find(".popup-content").addClass(type == 'info'
					? 'alert-info'
					: type == 'confirm' ? 'alert' : '');
			popupMessage.html(msg);

			popupContainer.css({
						minWidth : popupContainer.outerWidth(),
						maxWidth : popupContainer.outerWidth()
					});

			$.alerts._reposition();
			$.alerts._maintainPosition(true);

			switch (type) {
				case 'info' :
				case 'alert' :
					var popupOk = $('<div class="popup-panel"><button class="popup-ok">'
							+ $.alerts.okButton + '</button></div>')
							.insertAfter(popupMessage).find(".popup-ok");
					popupOk.click(function() {
								$.alerts._hide();
								callback(true);
							}).keypress(function(e) {
								if (e.keyCode == 13 || e.keyCode == 27)
									popupOk.trigger('click');
							}).focus();
					break;
				case 'confirm' :
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
					popupPanel.keyup(function(e) {
								if (e.keyCode == 13)
									popupOk.trigger('click');
								if (e.keyCode == 27)
									popupCancel.trigger('click');
							});
					break;
				case 'prompt' :
					popupMessage
							.append('<br /><input type="text" size="30" class="popup-prompt" />')
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
					popupContainer.keyup(function(e) {
								if (e.keyCode == 13)
									popupOk.trigger('click');
								if (e.keyCode == 27)
									popupCancel.trigger('click');
							});
					if (value)
						popupPrompt.val(value);
					popupPrompt.focus().select();
					break;
			}

			// Make draggable
			if ($.alerts.draggable) {
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
		},

		_hide : function() {
			$("#popup-container").remove();
			$.alerts._overlay('hide');
			$.alerts._maintainPosition(false);
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

	// Shortuct functions
	jAlert = function(message, title, callback) {
		$.alerts.alert(message, title, callback);
	}

	jConfirm = function(message, title, callback) {
		$.alerts.confirm(message, title, callback);
	};

	jPrompt = function(message, value, title, callback) {
		$.alerts.prompt(message, value, title, callback);
	};

})(jQuery);
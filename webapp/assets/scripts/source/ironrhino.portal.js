(function($) {
	$.fn.portal = function() {
		if (arguments.length == 0) {
			this.addClass('clearfix').each(function() {
				var portal = $(this);
				var savable = portal.hasClass('savable');
				$('.portal-column', portal).sortable({
							connectWith : '.portal-column',
							handle : '.portlet-header',
							opacity : 0.6,
							receive : function(event, ui) {
								var col = $(event.target);
								if (col.hasClass('empty'))
									col.removeClass('empty');
								if (savable)
									portal.portal('layout', 'save');
							},
							remove : function(event, ui) {
								var col = $(event.target);
								if ($('.portlet', col).length == 0)
									col.addClass('empty');
								if (savable)
									portal.portal('layout', 'save');
							},
							sort : function(event, ui) {
								if (savable)
									portal.portal('layout', 'save');
							}
						});
				$('.portlet', portal).each(function() {
					var header = $('.portlet-header', this);
					header
							.append('<div class="portlet-icon"><a class="btn btn-fold"><i class="glyphicon glyphicon-chevron-up"></i></a><a class="btn btn-close"><i class="glyphicon glyphicon-remove"></i></a><a class="btn btn-resize"><i class="glyphicon glyphicon-resize-full"></i></a></div>');
					if ($('.ajaxpanel,iframe', $(this)).length) {
						$('<a class="btn btn-refresh"><i class="glyphicon glyphicon-refresh"></i></a>')
								.insertBefore($('.portlet-header .btn-fold',
										this));
						$('iframe', $(this))
								.attr('scrolling', 'no')
								.attr('onload',
										'this.style.height=(this.contentDocument.body.offsetHeight)+\'px\'');
					}
				});
				portal.on('click', '.portlet-header .btn-close', function() {
					var p = $(this).closest('.portlet');
					var id = p.attr('id');
					if (savable && window.localStorage && id) {
						var hidden = localStorage[document.location.pathname
								+ '_portal-hidden'];
						if (hidden) {
							var hidden = hidden.split(',');
							if ($.inArray(id, hidden) < 0)
								hidden.push(id);
						} else {
							hidden = [id];
						}
						localStorage[document.location.pathname
								+ '_portal-hidden'] = hidden.join(',');
					}
					p.remove();
					addRestoreButton(portal);
				}).on('click', '.portlet-header .btn-resize', function() {
					var p = $(this).closest('.portlet')
							.toggleClass('portlet-fullscreen');
					var portal = p.closest('.portal');
					if (p.hasClass('portlet-fullscreen')) {
						p.wrap('<div class="replaceme"></div>')
								.find('.btn-resize i')
								.removeClass('glyphicon-resize-full')
								.addClass('glyphicon-resize-small');
						$('.portlet', portal).fadeOut(200, function() {
									p.prependTo(portal).fadeIn(200);
								});
					} else {
						p.hide().find('.btn-resize i')
								.removeClass('glyphicon-resize-small')
								.addClass('glyphicon-resize-full');
						portal.find('.replaceme').replaceWith(p);
						$('.portlet', portal).fadeIn();
					}
				}).on('click', '.portlet-header .btn-fold', function() {
					$('i', this).toggleClass('glyphicon-chevron-up')
							.toggleClass('glyphicon-chevron-down');
					var pc = $(this).closest('.portlet')
							.find('.portlet-content');
					pc.is(':visible') ? pc.slideUp() : pc.slideDown();
					var p = $(this).closest('.portlet');
					var id = p.attr('id');
					if (savable && window.localStorage && id) {
						var folded = localStorage[document.location.pathname
								+ '_portal-folded'];
						var folded = folded ? folded.split(',') : [];
						var isfold = $('i', this)
								.hasClass('glyphicon-chevron-down');
						if (isfold) {
							if ($.inArray(id, folded) < 0)
								folded.push(id);
						} else {
							folded.splice($.inArray(id, folded), 1);
						}
						if (folded.length)
							localStorage[document.location.pathname
									+ '_portal-folded'] = folded.join(',');
						else
							delete localStorage[document.location.pathname
									+ '_portal-folded']
					}
					addRestoreButton(portal);
				}).on('click', '.portlet-header .btn-refresh', function() {
					var portlet = $(this).closest('.portlet');
					$('.ajaxpanel', portlet).trigger('load');
					$('iframe', portlet).each(function(i, v) {
						var mask = typeof $.fn.mask != 'undefined';
						var pc = portlet.find('.portlet-content');
						if (mask)
							pc.mask(MessageBundle.get('ajax.loading'));
						v.onload = function() {
							if (mask)
								pc.unmask();
							this.style.height = (this.contentDocument.body.offsetHeight)
									+ 'px';
						};
						v.contentWindow.location.reload(true);
					});
				}).on('click', '.portal-footer .restore', function() {
							$(this).closest('.portal').portal('layout',
									'restore');
						});
				if (window.localStorage) {
					var layout = localStorage[document.location.pathname
							+ '_portal-layout'];
					var hidden = localStorage[document.location.pathname
							+ '_portal-hidden'];
					var folded = localStorage[document.location.pathname
							+ '_portal-folded'];
					if (layout || hidden || folded) {
						$(this).portal('layout', 'render', layout, hidden,
								folded);
						if (savable)
							addRestoreButton(portal);
					}
				}
			});
			return this;
		}
		if (arguments[0] == 'layout') {
			if (!arguments[1]) {
				var layout = [];
				$('.portal-column', this.eq(0)).each(function() {
					var portlets = [];
					$('.portlet:visible', this).each(function() {
								if ($(this).attr('id'))
									portlets.push('"' + $(this).attr('id')
											+ '"');
							});
					layout.push('[' + portlets.join(',') + ']');
				});
				return '[' + layout.join(',') + ']';
			} else {
				if (arguments[1] == 'save') {
					if (localStorage) {
						localStorage[document.location.pathname
								+ '_portal-layout'] = this.eq(0)
								.portal('layout');
						addRestoreButton(this.eq(0));
					}
				} else if (arguments[1] == 'restore') {
					delete localStorage[document.location.pathname
							+ '_portal-layout'];
					delete localStorage[document.location.pathname
							+ '_portal-hidden'];
					delete localStorage[document.location.pathname
							+ '_portal-folded'];
					document.location.reload();
				} else if (arguments[1] == 'render') {
					var layout = $.parseJSON(arguments[2] || '[]');
					var hidden = arguments[3];
					hidden = hidden ? hidden.split(',') : [];
					var folded = arguments[4];
					folded = folded ? folded.split(',') : [];
					$('.portlet', this).each(function() {
								var t = $(this);
								var id = t.attr('id');
								if (id && $.inArray(id, hidden) > -1)
									t.remove();
							});
					for (var i = 0; i < layout.length; i++) {
						$('.portal-column:eq(' + i + ')', this).each(
								function() {
									var portlets = layout[i];
									for (var j = 0; j < portlets.length; j++) {
										$('#' + portlets[j]).appendTo(this)
												.show();
									}
								});
					}
					$('.portlet', this).each(function() {
								var t = $(this);
								var id = t.attr('id');
								if (id && $.inArray(id, folded) > -1)
									t.find('.glyphicon-chevron-up').click();
							});
				}
				return this;
			}
		}
	};

	function addRestoreButton(portal) {
		if (!portal.find('.portal-footer .restore').length) {
			var footer = portal.find('.portal-footer');
			if (!footer.length)
				footer = $('<div class="portal-footer"><button class="btn restore">'
						+ MessageBundle.get('restore') + '</button></div>')
						.appendTo(portal);
			if (!footer.find('.restore').length)
				footer.append('<button class="btn restore">'
						+ MessageBundle.get('restore') + '</button> ');
		}
	}

})(jQuery);

Observation._portal = function(container) {
	var c = $(container);
	var selector = '.portal';
	c.is(selector) ? c.portal() : $(selector, c).portal();
};
(function($) {
	$.fn.portal = function() {
		if (arguments.length == 0) {
			this
					.addClass('clearfix')
					.each(
							function() {
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
								$('.portlet', portal)
										.each(
												function() {
													var header = $(
															'.portlet-header',
															this);
													header
															.append('<div class="portlet-icon"><a class="btn btn-fold"><i class="glyphicon glyphicon-chevron-up"></i></a><a class="btn btn-close"><i class="glyphicon glyphicon-remove"></i></a></div>');
													if ($('.ajaxpanel', $(this)).length) {
														$(
																'<a class="btn btn-refresh"><i class="glyphicon glyphicon-refresh"></i></a>')
																.insertBefore(
																		$(
																				'.portlet-header .btn-fold',
																				this));
													}
												});
								portal
										.on(
												'click',
												'.portlet-header .btn-close',
												function() {
													var p = $(this).closest(
															'.portlet');
													var id = p.attr('id');
													if (savable
															&& window.localStorage
															&& id) {
														var hidden = localStorage[document.location.pathname
																+ '_portal-hidden'];
														if (hidden) {
															var hidden = hidden
																	.split(',');
															if ($.inArray(id,
																	hidden) < 0)
																hidden.push(id);
														} else {
															hidden = [ id ];
														}
														localStorage[document.location.pathname
																+ '_portal-hidden'] = hidden
																.join(',');
													}
													p.remove();
													addRestoreButton(portal);
												})
										.on(
												'click',
												'.portlet-header .btn-fold',
												function() {
													$('i', this)
															.toggleClass(
																	'glyphicon-chevron-up')
															.toggleClass(
																	'glyphicon-chevron-down');
													$(this)
															.closest('.portlet')
															.find(
																	'.portlet-content')
															.toggle();
												})
										.on(
												'click',
												'.portlet-header .btn-refresh',
												function() {
													$(
															'.ajaxpanel',
															$(this).closest(
																	'.portlet'))
															.trigger('load');
												}).on(
												'click',
												'.portal-footer .restore',
												function() {
													$(this).closest('.portal')
															.portal('layout',
																	'restore');
												});
								if (window.localStorage) {
									var layout = localStorage[document.location.pathname
											+ '_portal-layout'];
									var hidden = localStorage[document.location.pathname
											+ '_portal-hidden'];
									if (layout || hidden) {
										$(this).portal('layout', 'render',
												layout, hidden);
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
							portlets.push('"' + $(this).attr('id') + '"');
					});
					layout.push('[' + portlets.join(',') + ']');
				});
				return '[' + layout.join(',') + ']';
			} else {
				if (arguments[1] == 'save') {
					if (localStorage) {
						localStorage[document.location.pathname
								+ '_portal-layout'] = this.eq(0).portal(
								'layout');
						addRestoreButton(this.eq(0));
					}
				} else if (arguments[1] == 'restore') {
					delete localStorage[document.location.pathname
							+ '_portal-layout'];
					delete localStorage[document.location.pathname
							+ '_portal-hidden'];
					document.location.reload();
				} else if (arguments[1] == 'render') {
					var layout = $.parseJSON(arguments[2] || '[]');
					var hidden = arguments[3];
					hidden = hidden ? hidden.split(',') : [];
					$('.portlet', this).each(function() {
						var t = $(this);
						var id = t.attr('id');
						if (id && $.inArray(id, hidden) > -1)
							t.remove();
					});
					for ( var i = 0; i < layout.length; i++) {
						$('.portal-column:eq(' + i + ')', this).each(
								function() {
									var portlets = layout[i];
									for ( var j = 0; j < portlets.length; j++) {
										$('#' + portlets[j]).appendTo(this)
												.show();
									}
								});
					}
				}
				return this;
			}
		}
	};

	function addRestoreButton(portal) {
		if (!portal.find('.portal-footer .restore').length) {
			var footer = portal.find('.portal-footer');
			if (!footer.length)
				footer = $(
						'<div class="portal-footer"><button class="btn restore">'
								+ MessageBundle.get('restore')
								+ '</button></div>').appendTo(portal);
			if (!footer.find('.restore').length)
				footer.append('<button class="btn restore">'
						+ MessageBundle.get('restore') + '</button> ');
		}
	}

})(jQuery);

Observation._portal = function(container) {
	$('.portal', container).portal();
};
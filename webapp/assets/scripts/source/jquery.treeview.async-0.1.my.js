/*
 * Async Treeview 0.1 - Lazy-loading extension for Treeview
 * 
 * http://bassistance.de/jquery-plugins/jquery-plugin-treeview/
 * 
 * Copyright (c) 2007 Jörn Zaefferer
 * 
 * Dual licensed under the MIT and GPL licenses:
 * http://www.opensource.org/licenses/mit-license.php
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Revision: $Id$
 * 
 */
(function($) {
	var fullname;
	function load(settings, root, child, container) {
		if (root == 0)
			fullname = settings.value;
		$.getJSON(settings.url, {
					parent : root
				}, function(response) {
					if (response.length == 0) {
						var li = child.parent('li');
						if (li.length) {
							if (li.hasClass('lastExpandable')
									|| li.hasClass('lastCollapsable'))
								li.removeClass('lastExpandable')
										.removeClass('lastCollapsable')
										.addClass('last');
							li.removeClass('collapsable')
									.removeClass('expandable').find('.hitarea')
									.remove();
							child.remove();
						}
						return;
					}
					function createNode(parent, activeid) {
						var parentTreenode = $(parent).parent('li')
								.data('treenode');
						this.parent = parentTreenode;
						if (!this.fullname)
							if (parentTreenode)
								this.fullname = (parentTreenode.fullname || parentTreenode.name)
										+ (settings.separator) + this.name;
							else
								this.fullname = this.name;
						var template = settings.template;
						var current = $("<li/>").data('treenode', this)
								.appendTo(parent);
						if (activeid == this.id)
							current.addClass('active');
						if (template) {
							current.html($.tmpl(template, this));
							_observe(current);
						} else {
							current.html("<a><span>" + (this.name)
									+ "</span></a>");
						}
						$("a", current).click(function(e) {
							var li = $(e.target).closest('li');
							$('li,div.head', li.closest('.treeview'))
									.removeClass('active');
							li.addClass('active');
						});
						if (settings.click)
							$("span", current).click(settings.click);
						if (this.classes) {
							current.children("span").addClass(this.classes);
						}
						if (this.expanded) {
							current.addClass("open");
						}
						if (this.hasChildren || this.children
								&& this.children.length) {
							var branch = $("<ul/>").appendTo(current);
							if (this.hasChildren) {
								current.addClass("hasChildren");
								createNode.call({
											text : settings.placeholder
													|| "placeholder",
											id : "placeholder",
											children : []
										}, branch);
							}
							if (this.children && this.children.length) {
								$.each(this.children, function() {
											createNode.apply(this, branch)
										})
							}
						}
					}
					var activeid = container.data('activeid');
					container.removeData('activeid');
					$.each(response, function() {
								createNode.apply(this, [child, activeid])
							});
					$(container).treeview({
								add : child
							});
					var list = child.children('li');
					if (fullname) {
						for (var i = 0; i < list.length; i++) {
							var t = $(list.get(i));
							var name = t.data('treenode').name;
							if (name == fullname) {
								t.children('a').click();
								break;
							} else if (name
									&& fullname.indexOf(name
											+ settings.separator) == 0) {
								fullname = fullname.substring(name.length
										+ settings.separator.length);
								$('.hitarea', t).click();
								break;
							}
						}
					} else if (child.hasClass('treeview') && list.length == 1) {
						var t = $(list.get(0));
						$('.hitarea', t).click();
					}
				});
	}

	var proxied = $.fn.treeview;
	$.fn.treeview = function(settings) {
		if (settings.separator == undefined) 
			settings.separator = '';
		if (!settings.url) 
			return proxied.apply(this, arguments);
		var container = this;
		this.on('reload', 'li', function() {
					var t = $(this);
					var hitarea = t.find('.hitarea');
					if (hitarea.length) {
						if (t.hasClass('expandable')) {
							t.addClass("hasChildren").find("ul").empty();
							hitarea.click();
							hitarea.click();
						} else {
							hitarea.click();
							t.addClass("hasChildren").find("ul").empty();
							hitarea.click();
						}
					}
					return false;
				}).on('reload', function() {
			var t = $(this);
			var active = t.find('li.active');
			if (active.length) {
				t.data('activeid', active.data('treenode').id);
				if (active.hasClass('collapsable')
						|| active.hasClass('expandable')) {
					active.trigger('reload');
					return false;
				} else {
					var parent = active.closest('li.collapsable');
					if (parent.length) {
						parent.trigger('reload');
						return false;
					}
				}
			}
			t.children('li').remove();
			load(settings, settings.root || "0", t, container);
			return false;
		});
		load(settings, settings.root || "0", this, container);
		var userToggle = settings.toggle;
		return proxied.call(this, $.extend({}, settings, {
			collapsed : true,
			toggle : function() {
				var $this = $(this);
				if ($this.hasClass('collapsable')
						&& $this.hasClass("hasChildren")) {
					var childList = $this.removeClass("hasChildren").find("ul");
					childList.empty();
					load(settings, $(this).data('treenode').id, childList,
							container);
				}
				if (userToggle) {
					userToggle.apply(this, arguments);
				}
			}
		}));
	};

})(jQuery);
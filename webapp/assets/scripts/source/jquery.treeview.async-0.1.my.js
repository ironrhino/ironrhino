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
/*
 * changes by zhouyanming 1.change "source" to "0"; 2.change attr('id',this.id)
 * to data('treenode',this), change this.text to (this.name) 3.add <a> out of
 * <span> 4.add setting.onclick to <span> 5.if settings.unique=true will call
 * toggle on siblings,if not add $this.hasClass('collapsable') will load
 * siblings's data,change parameter name from root to parent
 */
;
(function($) {
	var fullname;
	function load(settings, root, child, container) {
		if (root == 0)
			fullname = settings.value;
		$.getJSON(settings.url, {
					parent : root
				}, function(response) {
					function createNode(parent) {
						var parentTreenode = $(parent).parent('li')
								.data('treenode');
						this.parent = parentTreenode;
						if (!this.fullname)
							if (parentTreenode)
								this.fullname = (parentTreenode.fullname || parentTreenode.name)
										+ (settings.separator || '')
										+ this.name;
							else
								this.fullname = this.name;
						var template = settings.template;
						var current = $("<li/>").data('treenode', this)
								.appendTo(parent);
						if (template) {
							current.html($.tmpl(template, this));
							_observe(current);
						} else {
							current.html("<a><span>" + (this.name)
									+ "</span></a>");
						}
						$("a", current).click(function(e) {
							var li = $(e.target).closest('li');
							$('li', li.closest('.treeview'))
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
					$.each(response, function() {
								createNode.apply(this, child)
							});
					$(container).treeview({
								add : child
							});
					if (fullname) {
						var list = $('li', child);
						for (var i = 0; i < list.length; i++) {
							var t = $(list.get(i));
							var name = t.data('treenode').name;
							if (name
									&& fullname.indexOf(name
											+ settings.separator) == 0) {
								fullname = fullname.substring(name.length
										+ settings.separator.length);
								$('.hitarea', t).click();
								break;
							}
						}
					}
				});
	}

	var proxied = $.fn.treeview;
	$.fn.treeview = function(settings) {
		if (!settings.url) {
			return proxied.apply(this, arguments);
		}
		var container = this;
		this.on('refresh', 'li', function() {
					var t = $(this);
					if (t.hasClass('hasChildren'))
						return false;
					if (t.hasClass('expandable')) {
						t.addClass("hasChildren").find("ul").empty();
					} else {
						t.find('.hitarea').click();
						t.addClass("hasChildren").find("ul").empty();
						t.find('.hitarea').click();
					}
					return false;
				}).on('refresh', function() {
					var t = $(this);
					if (t.find('li.active').length) {
						t.find('li.active').trigger('refresh');
						return false;
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
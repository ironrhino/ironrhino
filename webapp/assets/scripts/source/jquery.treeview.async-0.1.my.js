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
						if (parentTreenode)
							this.fullname = (parentTreenode.fullname || parentTreenode.name)
									+ (settings.separator || '') + this.name;
						else
							this.fullname = this.name;
						var current = $("<li/>")
								.data('treenode', this)
								.html("<a><span>" + (this.name) + "</span></a>")
								.appendTo(parent);
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
								$.each(this.children, createNode, [branch])
							}
						}
					}
					$.each(response, createNode, [child]);
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
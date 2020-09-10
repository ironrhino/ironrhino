;
$(function() {
	if ($(document.body).hasClass('sidebar')) {
		var nav = $('.navbar .nav');
		var sidebar = $('.nav-sidebar');
		if (nav.length && !sidebar.length) {
			var accordion = $('<aside class="nav-sidebar"><div class="accordion"></div></aside>')
				.insertAfter($('.navbar')).find('.accordion');
			var emptyHeading = '<span class="glyphicon glyphicon-menu-down"></span>';
			var headings = [];
			var bodies = [];
			var current = [];
			nav.find('> li').each(function() {
				var li = $(this);
				if (li.hasClass('dropdown')) {
					if (current.length) {
						headings.push(emptyHeading);
						bodies.push(current);
						current = [];
					}
					headings.push(li.find('.dropdown-toggle').html()
						|| emptyHeading);
					bodies.push(li.find('.dropdown-menu').children());
				} else {
					current.push(li);
				}
			});
			if (current.length) {
				headings.push(emptyHeading);
				bodies.push(current);
			}
			$.each(headings, function(i, v) {
				var id = 'nav-sidebar-accordion-group-' + i;
				var group = $('<div class="accordion-group"><div class="accordion-heading"><a class="accordion-toggle" data-toggle="collapse"></a></div><div class="accordion-body collapse"><div class="accordion-inner"><ul class="nav nav-list"></ul></div></div></div>')
					.appendTo(accordion);
				group.find('.accordion-toggle').attr('href', '#' + id).html(v);
				var ab = group.find('.accordion-body').attr('id', id);
				if (v == emptyHeading)
					ab.addClass('in');
				var list = ab.find('.nav-list');
				$.each(bodies[i], function() {
					list.append(this);
				});
			});
			nav.remove();
			Nav.activate(document.location.href);
		}
		$(document).on('click', '.btn.btn-navbar', function(e) {
			var sidebar = $('.nav-sidebar');
			sidebar.toggleClass('visible');
			if (sidebar.hasClass('visible')) {
				var modal = $('<div class="nav-sidebar-modal"></div>')
					.appendTo(document.body);
				modal.addClass('visible');
			} else {
				$('.nav-sidebar-modal').remove();
			}
		}).on('click', '.nav-sidebar-modal', function() {
			$('.btn.btn-navbar').click();
		}).on('click', '.nav-sidebar .nav-list li a', function() {
			$('.btn.btn-navbar').click();
		});
	}
});

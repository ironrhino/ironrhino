$(function() {
			var pressed = false;
			var start = undefined;
			var startX, startWidth;
			$(document).on('mousedown', 'table.resizable span.resizeBar',
					function(e) {
						start = $(this).parent();
						pressed = true;
						startX = e.pageX;
						startWidth = start.width();
					}).mousemove(function(e) {
				if (pressed) {
					document.body.style.cursor = 'col-resize';
					var newwidth = startWidth + (e.pageX - startX);
					var minColWidth = $(e.target).closest('table')
							.data('mincolwidth')
							|| '60';
					if (minColWidth && parseInt(minColWidth) > newwidth)
						start.width(parseInt(minColWidth));
					else
						start.width(newwidth);
				}
			}).mouseup(function() {
						if (pressed) {
							pressed = false;
							document.body.style.cursor = '';
						}
					});
		});
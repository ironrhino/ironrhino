(function($) {
	$.fn.pattern = function(_options) {
		_options = _options || {};
		_options = $.extend({
					pathColor : 'green',
					rows : 3,
					cols : 3
				}, _options);
		this.each(function() {
			options = $.extend(_options, (new Function("return "
							+ ($(this).data('options') || '{}')))());
			if (options.cols < 2 || options.cols > 5)
				return;
			var pattern = $(this).addClass('pattern');
			var line = $('<div class="line unselectable"></div>')
					.appendTo(pattern);
			var cell = $('<div class="cell"><div class="circle"><div class="dot"></div></div></div>')
					.appendTo(line);
			for (var i = 0; i < options.cols - 1; i++)
				cell.clone().appendTo(line);
			for (var i = 0; i < options.rows - 1; i++)
				line.clone().appendTo(pattern);
			var modal = pattern.closest('.modal');
			if (modal.length)
				modal.css({
							'width' : '300px',
							'left' : '50%',
							'margin-left' : '-150px',
							'top' : '50%',
							'margin-top' : '-150px'
						});
			if (!('ontouchstart' in document.documentElement)) {
				pattern.on('mousedown', '.dot', function() {
							pattern.addClass('recording');
							$(this).addClass('active');
							pattern.data('coords', [getCoords(this)]);
							pattern.data('previous', this);
						}).on('mouseover', '.dot', function() {
							var previous = pattern.data('previous');
							if (!pattern.hasClass('recording')
									|| previous == this)
								return;
							$(this).addClass('active');
							connect(previous, this, options.pathColor);
							pattern.data('coords').push(getCoords(this));
							pattern.data('previous', this);
						}).on('mouseout', '.dot', function() {
							$(this).removeClass('active');
						}).on('mouseup', function() {
					if (pattern.hasClass('recording')) {
						var coords = pattern.data('coords');
						pattern.removeClass('recording').removeData('previous')
								.removeData('coords').find('div.path').remove();
						if (options.oncomplete)
							options.oncomplete(coords);
					}
				});
			} else {
				pattern.bind('touchmove', function(ev) {
					ev.preventDefault();
					var dot = getTouchedDot(ev.originalEvent.touches[0],
							pattern);

					if (!dot)
						return;
					var previous = pattern.data('previous');
					if (dot == previous)
						return;
					if (previous == null) {
						pattern.data('coords', [getCoords(dot)]);
						pattern.data('previous', dot);
					} else {
						connect(previous, dot, options.pathColor);
						pattern.data('coords').push(getCoords(dot));
						pattern.data('previous', dot);
					}
				}).bind('touchend', function(ev) {
					ev.preventDefault();
					var coords = pattern.data('coords');
					if (coords) {
						pattern.removeData('previous').removeData('coords')
								.find(	'div.path').remove();
						if (options.oncomplete)
							options.oncomplete(coords);
					}
				});
			}
		});
	};

	function getTouchedDot(touch, pattern) {
		var x = touch.pageX;
		var y = touch.pageY;
		var dot = null;
		$('.dot', pattern).each(function() {
					var t = $(this);
					var xx = x - t.offset().left;
					var yy = y - t.offset().top;
					if (xx >= 0 && xx <= t.width() && yy >= 0
							&& yy <= t.height())
						dot = this;
				});
		return dot;
	}

	function getCoords(dot) {
		var pattern = $(dot).closest('.pattern');
		var x = 0, y = 0;
		$('.line', pattern).each(function(i, v) {
					$('.dot', v).each(function(j, v1) {
								if (this == dot) {
									x = i;
									y = j;
								}
							});
				});
		return [x, y];
	}

	function connect(dot1, dot2, color) {
		var modal = $(dot1).closest('.modal-body');
		var x1 = $(dot1).offset().left + $(dot1).width() / 2;
		if (modal.length)
			x1 -= modal.offset().left;
		var y1 = $(dot1).offset().top + $(dot1).height() / 2;
		if (modal.length)
			y1 -= modal.offset().top;
		var x2 = $(dot2).offset().left + $(dot2).width() / 2;
		if (modal.length)
			x2 -= modal.offset().left;
		var y2 = $(dot2).offset().top + $(dot2).height() / 2;
		if (modal.length)
			y2 -= modal.offset().top;
		var thickness = $(dot1).width() / 2;
		var length = Math.sqrt(((x2 - x1) * (x2 - x1))
				+ ((y2 - y1) * (y2 - y1)));
		var cx = ((x1 + x2) / 2) - (length / 2);
		var cy = ((y1 + y2) / 2) - (thickness / 2);
		var angle = Math.atan2((y1 - y2), (x1 - x2)) * (180 / Math.PI);
		var line = $('<div class="path"></div>').appendTo($(dot1)
						.closest('.pattern'));
		line.css({
					'z-index' : -1,
					'padding' : 0,
					'margin' : 0,
					'height' : thickness + 'px',
					'background-color' : color,
					'line-height' : '1px',
					'position' : 'absolute',
					'left' : cx + 'px',
					'top' : cy + 'px',
					'width' : length + 'px',
					'-moz-transform' : 'rotate(' + angle + 'deg)',
					'-webkit-transform' : 'rotate(' + angle + 'deg)',
					'-ms-transform' : 'rotate(' + angle + 'deg)',
					'transform' : 'rotate(' + angle + 'deg)'
				});
	}
})(jQuery);
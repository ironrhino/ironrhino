(function($) {

	var duration = 750;

	function showWave(e, element) {
		// Disable right click
		if (e.button === 2)
			return;
		element = element || this;
		var pos = $(element).addClass('wave').offset();
		var ripple = $('<div class="ripple"></div>').appendTo(element);
		var x = (e.pageX - pos.left);
		var y = (e.pageY - pos.top);
		x = x >= 0 ? x : 0;
		y = y >= 0 ? y : 0;
		var scale = 'scale(' + ((element.clientWidth / 100) * 3) + ')';
		var translate = 'translate(0,0)';
		ripple.data('hold', Date.now()).data('x', x).data('y', y).data('scale',
				scale).data('translate', translate);
		ripple.css({
					transition : 'none !important',
					top : y + 'px',
					left : x + 'px'
				});
		ripple.css({
					transition : 'inherit',
					transform : scale + ' ' + translate,
					opacity : '1',
					'transition-duration' : (e.type === 'mousemove'
							? 2500
							: duration)
							+ 'ms'
				});
	};

	function hideWave(e, element) {
		element = element || this;
		$(element).find('.ripple').each(function() {
					removeRipple(e, element, this);
				});
	}

	function removeRipple(e, el, ripple) {
		ripple = $(ripple);
		var x = ripple.data('x');
		var y = ripple.data('y');
		var scale = ripple.data('scale');
		var translate = ripple.data('translate');
		var diff = Date.now() - ripple.data('hold');
		var delay = 350 - diff;
		delay = delay >= 0 ? delay : 0;
		if (e.type === 'mousemove')
			delay = 150;
		var _duration = e.type === 'mousemove' ? 2500 : duration;
		setTimeout(function() {
					ripple.css({
								top : y + 'px',
								left : x + 'px',
								opacity : '0',
								'transition-duration' : _duration + 'ms',
								'transform' : scale + ' ' + translate
							});
					setTimeout(function() {
								ripple.remove();
								if (!$(el).find('.ripple').length)
									$(el).removeClass('wave');
							}, _duration);
				}, delay);
	}

	$(function() {
		if (!$.browser.msie) {
			var selector = '.waves-effect, table.richtable th:not(.filtercolumn), table.richtable td:not(.action), button, a[href]:not(.nav-tabs > li > a)';
			$(document).on('mousedown', selector, showWave).on(
					'mouseup mouseleave', selector, hideWave);
		}
	});

})(jQuery);

(function($) {
	$(document).on('click', '.combobox .add-on', function(e) {
		var input = $('input', $(e.target).closest('.combobox'));
		var menu = $('.combobox-menu', $(e.target).closest('.combobox'));
		var val = input.val();
		if (val) {
			var exists = false;
			$('li', menu).each(function() {
						if ($(this).text() == val) {
							$(this).addClass('active');
							exists = true;
						} else
							$(this).removeClass('active');
					});
			if (!exists)
				$('<li class="active"><a href="#">' + val + '</a></li>')
						.appendTo(menu);
		}
		menu.width(input.closest('.combobox').width() - 2).toggle();
	}).on('click', '.combobox .combobox-menu a', function(e) {
				e.preventDefault();
				if (!$(this).parent('li.group').length) {
					var input = $('input', $(e.target).closest('.combobox'));
					var menu = $('.combobox-menu', $(e.target)
									.closest('.combobox'));
					input.val($(this).text());
					input.trigger('change').trigger('blur');
					menu.hide();
				}
				return false;
			}).on('mouseenter', '.combobox .combobox-menu', function(e) {
				e.preventDefault();
				$('li', this).removeClass('active');
				return false;
			});
	$.fn.combobox = function() {
		var t = $(this);
		if (t.prop('tagName') == 'SELECT') {
			var div = $('<div class="input-append combobox"><ul class="dropdown-menu combobox-menu" role="menu"></ul><input type="text" name="'
					+ t.attr('name')
					+ '" value="'
					+ t.val()
					+ '"/><span class="add-on"><i class="glyphicon glyphicon-chevron-down"></i></span></div>')
					.insertBefore(t);
			$('input', div).width(t.width() - 27);
			if (t.hasClass('required'))
				$('input', div).addClass('required');
			var _menu = $('.combobox-menu', div);
			t.children().each(function(i, v) {
				if ($(v).prop('tagName') == 'OPTION' && $(v).attr('value')) {
					$('<li><a href="#">' + $(v).attr('value') + '</a></li>')
							.appendTo(_menu);
				} else if ($(v).prop('tagName') == 'OPTGROUP') {
					var label = $(v).attr('label');
					var group = $('<li class="group"><a href="#">' + label
							+ '</a><ul></ul></li>').appendTo(_menu);
					group = $('ul', group);
					$(v).children('option').each(function(i, v) {
						$('<li><a href="#">' + $(v).attr('value') + '</a></li>')
								.appendTo(group);
					});
				}
			});
			t.remove();
			t = div;
		}
		var arr = [];
		$('.combobox-menu li:not(.group)', t).each(function() {
					arr.push($(this).text());
				});
		$('input', t).attr('data-provide', 'typeahead').attr('data-source',
				JSON.stringify(arr));
		var menu = $('.combobox-menu', t);
		$('li.group ul', menu).addClass('unstyled');
		$('li.group > a ', menu).css({
					'font-weight' : 'bold'
				});
		$('li.group li a', menu).css({
					'padding-left' : '25px'
				});
		t.css({
					'display' : 'inline-block',
					'position' : 'relative'
				});
		$('.add-on', t).css('cursor', 'pointer');
		return t;
	}
})(jQuery);

Observation.combobox = function(container) {
	$('.combobox', container).each(function() {
				$(this).combobox()
			});
};
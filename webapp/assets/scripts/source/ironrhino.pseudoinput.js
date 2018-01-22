(function($) {
	$(function() {
		$(document).on('mouseenter', '.input-pseudo .text:not(.tags)',
				function(e) {
					var t = $(this);
					var text = t.text();
					if (text)
						t.attr('title', text);
				}).on('mouseleave', '.input-pseudo .text:not(.tags)',
				function(e) {
					$(this).removeAttr('title');
				}).on('click', '.input-pseudo .remove', function(e) {
					var t = $(e.target).closest('.input-pseudo');
					t.find('input[name]').val('').trigger('change');
					t.find('.text').text('');
					return false;
				}).on('click', '.input-pseudo .tag-remove', function(e) {
					var t = $(e.target).closest('.input-pseudo');
					var tag = $(e.target).closest('.tag');
					var index = tag.parent().find('.tag').index(tag);
					var input = t.find('> input');
					var value = input.val();
					if (value) {
						var arr = value.split(/\s*,\s*/);
						arr.splice(index, 1);
						input.val(arr.join(',')).trigger('change');
					}
					tag.remove();
					return false;
				}).on('val', '.input-pseudo', function(e, val, textOnly) {
			if (!val)
				return;
			var input = $(this).find('> input');
			var text = $(this).find('.text');
			text.removeClass('tags').html('');
			if (val.constructor === Array) {
				text.addClass('tags');
				var keys = [];
				$.each(val, function(i, v) {
							keys.push((typeof v == 'string') ? v : v.key);
							$('<div class="tag"><span class="tag-label"></span>'
									+ (textOnly
											? ''
											: '<span class="tag-remove"/>')
									+ '</div>').appendTo(text)
									.find('.tag-label')
									.html((typeof v == 'string') ? v : v.value);
						});
				if (!textOnly)
					input.val(keys.join(',')).trigger('change');
			} else {
				text.html((typeof val == 'string') ? val : val.value);
				if (!textOnly)
					input.val((typeof val == 'string') ? val : val.key)
							.trigger('change');
			}
			return false;
		});
	});
})(jQuery);
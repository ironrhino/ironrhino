(function($) {

	function check(group) {
		var boxes = $('input[type=checkbox][name]', group);
		var allchecked = boxes.length > 0;
		if (allchecked)
			for (var i = 0; i < boxes.length; i++)
				if (!boxes[i].checked) {
					allchecked = false;
					break;
				}
		$('input[type=checkbox]:not(.normal):not([name])', group).prop(
				'checked', allchecked);
	}

	$.fn.checkbox = function() {
		$('.checkboxgroup', this).each(function() {
					check(this);
				});

		$('input[type=checkbox]', this).click(function(event) {
			if ($(this).hasClass('normal'))
				return;
			var group = $(this).closest('.checkboxgroup');
			if (!group.length)
				group = $(this).closest('form.richtable');
			if (!this.name) {
				var b = this.checked;
				if (group.length)
					$('input[type=checkbox][name]', group).each(function() {
								this.checked = b;
								var tr = $(this).closest('tr');
								if (tr.length) {
									if (b)
										tr.addClass('selected');
									else
										tr.removeClass('selected');
								}
							});
			} else {
				try {
					document.getSelection().removeAllRanges();
				} catch (e) {
				}
				if (!event.shiftKey) {
					var tr = $(this).closest('tr');
					if (tr) {
						if (group.length && this.checked)
							tr.addClass('selected');
						else
							tr.removeClass('selected');
					}
					var table = $(this).closest('table');
					if (table.hasClass('treeTable')) {
						var checked = this.checked;
						$('tr.child-of-node-' + this.value, table)
								.find('input[type=checkbox]').prop('checked',
										checked).end().each(function() {
											if (checked)
												$(this).addClass('selected');
											else
												$(this).removeClass('selected');
										});
					}
				} else {
					var boxes = $('input[type=checkbox][name]', group);
					var start = -1, end = -1, checked = false;
					for (var i = 0; i < boxes.length; i++) {
						if ($(boxes[i]).hasClass('lastClicked')) {
							checked = boxes[i].checked;
							start = i;
						}
						if (boxes[i] == this) {
							end = i;
						}
					}
					if (start > end) {
						var tmp = end;
						end = start;
						start = tmp;
					}
					for (var i = start; i <= end; i++) {
						boxes[i].checked = checked;
						tr = $(boxes[i]).closest('tr');
						if (tr) {
							if (boxes[i].checked)
								tr.addClass('selected');
							else
								tr.removeClass('selected');
						}
					}
				}
				$('input[type=checkbox]', group).removeClass('lastClicked');
				$(this).addClass('lastClicked');
				check(group);
			}
		});
		return this;
	}
})(jQuery);

Observation.checkbox = function(container) {
	$(container).checkbox();
};
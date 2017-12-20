(function($) {

	function check(group) {
		var boxes = $('input[type=checkbox]:not(.normal):not(.checkall)', group);
		var allchecked = boxes.length > 0;
		if (allchecked)
			for (var i = 0; i < boxes.length; i++)
				if (!boxes[i].checked) {
					allchecked = false;
					break;
				}
		$('input.checkall[type=checkbox]:not(.normal)', group).prop('checked',
				allchecked);
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
			if (!group.length)
				group = $(this).closest('div.controls');
			if ($(this).hasClass('checkall')) {
				var b = this.checked;
				if (group.length)
					$('input[type=checkbox]:not(.normal)', group).each(
							function() {
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
				} else if (group.length) {
					var boxes = $(
							'input[type=checkbox]:not(.checkall):not(.normal)',
							group);
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
					if (start >= 0 && end > start)
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

	$(function() {
				$(document).on('change',
						'table .checkbox input[type="checkbox"]', function() {
							var rows = [];
							if ($(this).hasClass('checkall')) {
								if (this.checked)
									$('tbody tr', $(this).closest('table'))
											.each(function() {
														rows.push(this);
													});
							} else {
								$('tbody tr', $(this).closest('table')).each(
										function() {
											if ($(
													'td:eq(0) input[type="checkbox"]',
													this).is(':checked'))
												rows.push(this);
										});
							}
							var form = $(this).closest('form');
							$('[data-shown]', form).each(function() {
								var t = $(this);
								var filter = t.data('filterselector');
								var allmatch = t.data('allmatch');
								if (allmatch == undefined)
									allmatch = true;
								var count = 0;
								$.each(rows, function(i, v) {
											var row = $(v);
											try {
												if (!filter || row.is(filter)
														|| row.find(filter) > 0)
													count++;
											} catch (e) {

											}
										});
								t.is('[data-shown="selected"]')
										&& (!allmatch || count == rows.length)
										&& count > 0
										|| t
												.is('[data-shown="singleselected"]')
										&& (!allmatch || count == rows.length)
										&& count == 1
										|| t.is('[data-shown="multiselected"]')
										&& (!allmatch || count == rows.length)
										&& count > 1 ? t
										.addClass('btn-primary').show() : t
										.removeClass('btn-primary').hide();
							});
						});
			});
})(jQuery);

Observation.checkbox = function(container) {
	$(container).checkbox();
};
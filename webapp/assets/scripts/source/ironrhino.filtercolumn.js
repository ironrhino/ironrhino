(function($) {
	$.fn.filtercolumn = function() {
		$(this).each(function() {
			var cells = this.rows[0].cells;
			var filtercolumn = $('.filtercolumn', this.rows[0]);
			if (!filtercolumn.length)
				filtercolumn = $(cells[cells.length - 1])
						.addClass('filtercolumn');
			if (!$('ul.filtercolumn', filtercolumn).length)
				filtercolumn
						.prepend('<ul class="filtercolumn"><li><i class="glyphicon glyphicon-th"></i><ul class="listTarget"></ul></li></ul>');
			var hideInList = [];
			var colsHidden = [];
			for (var i = 0; i < cells.length; i++) {
				var th = $(cells[i]);
				if (th.hasClass('nofilter') || th.hasClass('filtercolumn'))
					hideInList.push(i + 1);
				if (th.hasClass('filtered'))
					colsHidden.push(i + 1);
			}
			$(this).columnManager({
						listTarget : $('ul.listTarget', this),
						onClass : 'selecton',
						offClass : 'selectoff',
						hideInList : hideInList,
						colsHidden : colsHidden
					});
			$('ul.filtercolumn', this).clickMenu({
						onClick : function() {
						}
					});
		});
		return this;
	};
})(jQuery);

Observation.filtercolumn = function(container) {
	$('table.filtercolumn', container).filtercolumn();
};
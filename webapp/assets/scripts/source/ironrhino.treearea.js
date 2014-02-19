(function($) {
	$.fn.treearea = function(treeoptions) {
		treeoptions = treeoptions || {};
		this.addClass('treearea').each(function() {
			_treeoptions = $.extend(treeoptions, (new Function("return "
							+ ($(this).data('options') || '{}')))());
			var treearea = $(this);
			var fullname = _treeoptions.value;
			var i = 0;
			var callback = fullname ? function() {
				var area = $('.area:eq(' + (i++) + ')', treearea);
				var match = false;
				var id = null;
				var target = null;
				$('span', area).each(function() {
					if (!match) {
						var name = $(this).text();
						if (fullname == name
								|| fullname.indexOf(name
										+ _treeoptions.separator) == 0) {
							match = true;
							target = $(this);
							fullname = fullname.substring(name.length
									+ _treeoptions.separator.length);
							id = target.data('treenode').id;
						}
					}
				});
				if (match)
					if (fullname.length) {
						expand(_treeoptions, treearea, target, callback);
					} else {
						$('span', target.closest('.area'))
								.removeClass('selected');
						target.addClass('selected');
					}
			} : null;
			expand(_treeoptions, $(this), null, callback);
		});
		return this;
	};

	function expand(treeoptions, treearea, target, callback) {
		var level = 0;
		var areas = $('.area', treearea);
		var id = treeoptions.root || 0;
		if (target) {
			id = $(target).data('treenode').id;
			$('span', target.closest('.area')).removeClass('selected');
			target.addClass('selected');
		}
		if (id > 0)
			areas.each(function(i) {
						var index = i + 1;
						var match = false;
						$('span', this).each(function() {
									if ($(this).data('treenode').id == id)
										match = true;
								});
						if (match)
							level = index;
					});
		for (var i = areas.size() - 1; i > level; i--)
			areas.eq(i).fadeOut().remove();
		areas = $('.area', treearea);
		var area = level + 1 == areas.size()
				? areas.eq(level).html('')
				: $('<div/>').addClass('clearfix').addClass('area')
						.appendTo(treearea);
		var url = treeoptions.url;
		if (id > 0)
			url += '?parent=' + id;
		if (!treeoptions.cache)
			url += (url.indexOf('?') > -1 ? '&' : '?') + 'r=' + Math.random();
		$.getJSON(url, function(data) {
			$.each(data, function() {
				var fullname = '';
				$('.area', treearea).each(function() {
					$('span', this).each(function() {
						if ($(this).hasClass('selected'))
							fullname += (fullname
									? (treeoptions.separator || '')
									: '')
									+ $(this).text();
					});
				});
				this.fullname = fullname
						+ (fullname ? (treeoptions.separator || '') : '')
						+ this.name;
				var span = $('<span/>').text(this.name).data('treenode', this)
						.appendTo(area).click(function(ev) {
							var target = $(ev.target);
							if ((!target.hasClass('hasChildren') || (!treeoptions.leafonly && target
									.hasClass('selected')))
									&& treeoptions.click) {
								$('span', target.closest('.area'))
										.removeClass('selected');
								target.addClass('selected');
								treeoptions.click(target.data('treenode'));
							} else
								expand(treeoptions,
										target.closest('.treearea'), target,
										callback);
						});;
				if (this.hasChildren)
					span.addClass('hasChildren');

			});
			area.fadeIn();
			if (callback)
				callback();
		});
	}
})(jQuery);
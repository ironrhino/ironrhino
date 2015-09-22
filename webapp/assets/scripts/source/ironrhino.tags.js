(function($) {
	$.fn.tags = function() {
		$(this).each(function() {
			var t = $(this);
			var options = {
				prompt : '...',
				autocomplete : {
					dropdownMaxHeight : '200px',
					render : function(suggestion) {
						if (typeof suggestion == 'string') {
							return suggestion;
						} else {
							if (!suggestion.label)
								return suggestion.value;
							else
								return '<div value="' + suggestion.value + '">'
										+ suggestion.label + '</div>';
						}
					}
				},
				ext : {
					core : {
						serializeData : function(data) {
							return data.join(',');
						}
					},
					itemManager : {
						itemToString : function(item) {
							var str = typeof item == 'string'
									? item
									: item.value;
							return str;
						}
					},
					autocomplete : {
						showDropdown : function() {
							if ($('.text-suggestion', this.containerElement()).length)
								this.containerElement().show();
						},
						renderSuggestions : function(suggestions) {
							var self = this;
							self.clearItems();
							var inputed = [];
							$('.text-tags .text-label', self.container).each(
									function() {
										inputed.push($(this).text())
									});
							var empty = true;
							$.each(suggestions || [], function(index, item) {
										var value = self.itemManager()
												.itemToString(item);
										var exists = false;
										for (var j = 0; j < inputed.length; j++)
											if (inputed[j] == value) {
												exists = true;
												break;
											}
										if (!exists) {
											self.addSuggestion(item);
											empty = false;
										}
									});
						}
					}
				}
			};
			var value = t.val();
			if (value) {
				if (value.indexOf("[\"") == 0)
					options.tagsItems = $.parseJSON(value);
				else
					options.tagsItems = value.split(',');
			}
			if (t.data('source')) {
				if (t.data('source').indexOf('[') != 0) {
					options.plugins = 'tags prompt focus autocomplete ajax arrow';
					options.ajax = {
						global : false,
						url : t.data('source'),
						cacheResults : false,
						dataCallback : function(q) {
							return {
								'keyword' : q
							};
						}
					};
				} else {
					options.plugins = 'tags prompt focus autocomplete arrow';
					var list = (new Function("return " + t.data('source')))();
					t.bind('getSuggestions', function(e, data) {
								var textext = $(e.target).textext()[0], query = (data
										? data.query
										: '')
										|| '';
								$(this).trigger('setSuggestions', {
									result : textext.itemManager().filter(list,
											query)
								});
							});
				}
			} else if (t.attr('list') && $('#' + t.attr('list')).length) {
				options.plugins = 'tags prompt focus autocomplete arrow';
				var list = [];
				$('option', $('#' + t.attr('list'))).each(function() {
							list.push(this.value);
						});
				t.bind('getSuggestions', function(e, data) {
							var textext = $(e.target).textext()[0], query = (data
									? data.query
									: '')
									|| '';
							$(this).trigger('setSuggestions', {
								result : textext.itemManager().filter(list,
										query)
							});
						});
			} else {
				options.plugins = 'tags prompt focus';
			}
			t.val('').textext(options).bind('isTagAllowed', function(e, data) {
				var inputed = [];
				$('.text-tags .text-label',
						$(this).data('textext').wrapElement()).each(function() {
							inputed.push($(this).text())
						});
				for (var i = 0; i < inputed.length; i++)
					if (inputed[i] == data.tag) {
						data.result = false;
						break;
					}
			});
			t.blur(function() {
						if (t.val())
							t.trigger('enterKeyPress').val('').blur();
					});
		})
		return this;
	};
})(jQuery);
Observation.tags = function(container) {
	var c = $(container);
	var selector = 'input.tags';
	if (c.is(selector))
		c.tags();
	else
		$(selector, c).tags();
};
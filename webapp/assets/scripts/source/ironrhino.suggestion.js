Observation.suggestion = function(container) {
	var c = $(container);
	var selector = 'input.suggestion';
	var ele = c.is(selector) ? c : $(selector, c);
	ele.each(function() {
				var t = $(this);
				t.typeahead({
							minLength : 2,
							items : 20,
							source : function(query, process) {
								$.ajax({
											global : false,
											url : t.data('source'),
											type : 'GET',
											dataType : "json",
											data : {
												keyword : query
											},
											success : function(data) {
												process(data);
											}
										});
							},
							matcher : function(item) {
								return true;
							}
						});
			});
};
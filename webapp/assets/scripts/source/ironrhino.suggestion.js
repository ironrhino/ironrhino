Observation.suggestion = function(container) {
	$$('input.suggestion', container).each(function() {
				var t = $(this);
				var source = t.data('source');
				if (source) {
					if (source.indexOf('[') == 0)
						source = JSON.parse(source.replace(/'/g, '"'));
					else
						source = function(query, process) {
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
						}
					t.typeahead({
								minLength : 2,
								items : 20,
								source : source,
								matcher : function(item) {
									return true;
								}
							});
				}
			});
};
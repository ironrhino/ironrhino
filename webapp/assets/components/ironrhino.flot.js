(function($) {
	$.fn.flotlinechart = function() {
		$(this).each(function() {
			var ul = $(this), data = [], lies = $('li', ul), istime = false, options = {
				series : {
					lines : {
						show : true
					},
					points : {
						show : true
					}
				},
				grid : {
					hoverable : true
				}
			};
			if (!ul.data('least') || lies.length > parseInt(ul.data('least'))) {
				lies.each(function() {
							var point = [];
							if ($('span', this).data('time')) {
								istime = true;
								point.push(parseInt($('span', this)
										.data('time')));
							} else {
								point.push(parseInt($('span', this).text()));
							}
							point.push(parseInt($('strong', this).text()));
							data.push(point);
						});
				if (istime) {
					options.xaxis = {
						mode : 'time',
						timeformat : ul.data('timeformat') || '%m-%d'
					}
				}
				$.plot(ul, [data], options);
				var previousPoint = null;
				ul.bind('plothover', function(event, pos, item) {
							if (item) {
								if (previousPoint != item.dataIndex) {
									previousPoint = item.dataIndex;
									$('#tooltip').remove();
									var x = item.datapoint[0], y = item.datapoint[1];
									var content = '<strong style="margin-right:5px;">'
											+ (ul.hasClass('percent') ? y * 100
													+ '%' : y) + '</strong>';
									if (istime)
										content += '<span>'
												+ new Date(x).format($(this)
														.data('timeformat')
														|| '%m-%d') + '</span>';
									showTooltip(item.pageX, item.pageY, content);
								}
							} else {
								$('#tooltip').remove();
								previousPoint = null;
							}
						});

			}
		});
		return this;
	};

	$.fn.flotbarchart = function() {
		$(this).each(function() {
			var ul = $(this);
			var data = [];
			var xticks = [];
			var lies = $('li', ul);
			if (lies.length > 2) {
				lies.each(function() {
							var point = [];
							point.push(parseInt($('span', this).text()));
							point.push(parseInt($('strong', this).text()));
							xticks.push(point[0]);
							data.push(point);
						});
				$.plot(ul, [data], {
							series : {
								bars : {
									show : true
								}
							},
							grid : {
								hoverable : true
							},
							xaxis : {
								ticks : xticks,
								tickLength : 0,
								tickDecimals : 0
							}
						});
				var previousPoint = null;
				ul.bind('plothover', function(event, pos, item) {
							if (item) {
								if (previousPoint != item.dataIndex) {
									previousPoint = item.dataIndex;
									$('#tooltip').remove();
									var x = item.datapoint[0], y = item.datapoint[1];
									var content = '<strong style="margin-right:5px;">'
											+ y + ' </strong>';
									showTooltip(item.pageX, item.pageY, content);
								}
							} else {
								$('#tooltip').remove();
								previousPoint = null;
							}
						});

			}

		});
		return this;
	}

	$.fn.flotpiechart = function() {
		$(this).each(function() {
			var ul = $(this);
			var data = [];
			var lies = $('li', ul);
			lies.each(function() {
						var share = {};
						share.label = $('span', this).text();
						share.data = parseInt($('strong', this).text());
						data.push(share);
					});
			$.plot(ul, data, {
				series : {
					pie : {
						show : true,
						radius : 1,
						label : {
							show : true,
							radius : 2 / 3,
							formatter : function(label, series) {
								return "<div style='font-size:8pt; text-align:center; padding:2px; color:white;'>"
										+ label
										+ "<br/>"
										+ series.percent.toFixed(1) + "%</div>";
							},
							threshold : 0.1
						}
					}
				},
				legend : {
					show : true
				},
				grid : {
					// hoverable:true,
					clickable : true
				}
			});
			ul.bind('plotclick', function(event, pos, obj) {
						if (!obj) {
							$('#tooltip').remove();
							return;
						}
						showTooltip(pos.pageX, pos.pageY, obj.series.label
										+ ': ' + obj.series.data[0][1]);
					});

		});
		return this;
	}

	function showTooltip(x, y, content) {
		$('#tooltip').remove();
		$('<div id="tooltip">' + content + '</div>').css({
					position : 'absolute',
					display : 'none',
					top : y + 5,
					left : x + 5,
					border : '1px solid #fdd',
					padding : '2px',
					'background-color' : '#fee',
					opacity : 0.80,
					zIndex : 10010
				}).appendTo("body").fadeIn(200);
	}
})(jQuery);

Observation.flot = function(container) {
	$('ul.flotlinechart', container).flotlinechart();
	$('ul.flotbarchart', container).flotbarchart();
	$('ul.flotpiechart', container).flotpiechart();
}
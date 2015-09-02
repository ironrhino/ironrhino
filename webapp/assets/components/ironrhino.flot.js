(function($) {
	$.fn.flotchart = function() {
		$(this).each(function() {
			var ul = $(this), data = [];
			if ($('li', ul).length == 0 || ul.data('least')
					&& $('li', ul).length < parseInt(ul.data('least')))
				return;
			ul.wrap('<div class="plot-wrap" style="position:relative;"></div>');
			var plot = $('<div></div>').insertAfter(ul).width(ul.width())
					.height(ul.height());
			$('<div style="position: absolute;width: 20px;height: 20px;font-size: 20px;'
					+ (ul.hasClass('flotpiechart') ? 'bottom' : 'top')
					+ ': 15px;right: 20px;cursor: pointer;"><i class="glyphicon glyphicon-list"></i></div>')
					.insertAfter(plot).click(function() {
						$('.glyphicon', this).toggleClass('glyphicon-list')
								.toggleClass('glyphicon-picture');
						ul.toggle();
						plot.toggle();
					});
			if (ul.hasClass('flotlinechart')) {
				var istime = false, options = {
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
				$('li', ul).each(function() {
							var point = [];
							var label = $('.label,span', this);
							var value = $('.value,strong', this);
							if (label.data('time')) {
								istime = true;
								point.push(parseInt(label.data('time')));
							} else {
								point.push(parseInt(label.text()));
							}
							point.push(parseInt(value.text()));
							data.push(point);
						});
				if (istime) {
					options.xaxis = {
						mode : 'time',
						timeformat : ul.data('timeformat') || '%m-%d'
					}
				}

				$.plot(plot, [data], options);
				var previousPoint = null;
				plot.bind('plothover', function(event, pos, item) {
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
												+ $.format.date(new Date(x),
														'MM-dd') + '</span>';
									showTooltip(item.pageX, item.pageY, content);
								}
							} else {
								$('#tooltip').remove();
								previousPoint = null;
							}
						});
			} else if (ul.hasClass('flotbarchart')) {
				var xticks = [];
				$('li', ul).each(function() {
							var point = [];
							var label = $('.label,span', this);
							var value = $('.value,strong', this);
							point.push(parseInt(label.text()));
							point.push(parseInt(value.text()));
							xticks.push(point[0]);
							data.push(point);
						});
				$.plot(plot, [data], {
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
				plot.bind('plothover', function(event, pos, item) {
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
			} else if (ul.hasClass('flotpiechart')) {
				$('li', ul).each(function() {
							var share = {};
							var label = $('.label,span', this);
							var value = $('.value,strong', this);
							share.label = label.text();
							share.data = parseInt(value.text());
							data.push(share);
						});
				$.plot(plot, data, {
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
											+ series.percent.toFixed(1)
											+ "%</div>";
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
				plot.bind('plotclick', function(event, pos, obj) {
							if (!obj) {
								$('#tooltip').remove();
								return;
							}
							showTooltip(pos.pageX, pos.pageY, obj.series.label
											+ ': ' + obj.series.data[0][1]);
						});

			}

			ul.hide();

		});
		return this;
	};

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
	var c = $(container);
	var selector = 'ul.flotlinechart,ul.flotbarchart,ul.flotpiechart';
	c.is(selector) ? c.flotchart() : $(selector, c).flotchart();
}
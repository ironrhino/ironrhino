var margin = 20; // to avoid scrollbars

window.addEventListener('load', function() {

	var source = location.hash;
	if (!source)
		return;

	source = unescape(decodeURI(source.substring(1)));
	if (source.indexOf('graph ') == 0 || source.indexOf('digraph ') == 0) {
		render(source);
	} else {
		fetch(source)
			.then(function(response) {
				return response.text();
			}).then(render);
	}

	d3.select(window).on('resize', function() {
		var width = window.innerWidth;
		var height = window.innerHeight;
		d3.select('#graph').selectWithoutDataPropagation('svg')
			.transition()
			.duration(700)
			.attr('width', width - margin)
			.attr('height', height - margin);
	});
	d3.select(window).on('click', function() {
		graphviz
			.resetZoom(d3.transition().duration(1000));
	});

});

function render(source) {
	document.title = source.match(/graph\s+"?([^"]+).*\{/i)[1];
	var graph = document.getElementById('graph');
	if (!graph) {
		graph = document.createElement('div');
		graph.id = 'graph';
		document.body.appendChild(graph);
	}
	d3.select(graph).graphviz()
		.zoom(false)
		//.zoomScaleExtent([0.5, 2])
		.attributer(function(datum) {
			var selection = d3.select(this);
			if (datum.tag == 'svg') {
				var width = window.innerWidth;
				var height = window.innerHeight;
				selection
					.attr('width', width)
					.attr('height', height)
				datum.attributes.width = width - margin;
				datum.attributes.height = height - margin;
			}
		})
		.renderDot(source);
}
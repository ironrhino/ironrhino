(function($) {
	$.fn.latlng = function() {
		var key = $('meta[name="autonavi-maps-key"]').attr('content');
		if (typeof AMAP_KEY != 'undefined')
			key = AMAP_KEY;
		if (!key)
			key = '9ef5c43ba470e46e355160c9fddc7086';
		var t = $(this);
		var anchor;
		if (t.is('[type="hidden"]')) {
			anchor = $('<i class="glyphicon glyphicon-map-marker" style="cursor:pointer;"></i>')
					.insertAfter(t);
		} else {
			if (!t.parent('.input-append').length)
				t
						.wrap('<div class="input-append"></div>')
						.after('<span class="add-on"><i class="glyphicon glyphicon-map-marker" style="cursor:pointer;"></i></span>');
			anchor = $(
					'<i class="glyphicon glyphicon-map-marker" style="cursor:pointer;"></i>',
					t.next());
		}
		if (t.closest('.control-group').length)
			anchor = $('.control-label,.glyphicon-map-marker', t
							.closest('.control-group'));
		anchor.click(function() {
			window.latlng_input = t;
			if (!$('#_maps_window').length) {
				var win = $('<div id="_maps_window" title="'
						+ MessageBundle.get('select')
						+ '"><div id="_maps_container" style="width:500px;height:400px;"></div></div>')
						.appendTo(document.body).dialog({
									minWidth : 520,
									minHeight : 400
								});
				win.closest('.ui-dialog').css('z-index', 2500);
				if (typeof AMap == 'undefined') {
					var script = document.createElement('script');
					script.src = 'http://webapi.amap.com/maps?v=1.3&callback=latlng_initMaps&key='
							+ key;
					script.type = 'text/javascript';
					document.getElementsByTagName("head")[0]
							.appendChild(script);
				}

			} else {
				$('#_maps_window').dialog('open');
				latlng_getLatLng();
			}
		});
		if (t.data('staticmapselector')) {
			t.change(function(e) {
				var holder = $(t.data('staticmapselector'));
				var value = $(t).val();
				if (value) {
					var arr = value.split(',');
					value = arr[1] + ',' + arr[0];
					holder
							.html('<img src="http://restapi.amap.com/v3/staticmap?location='
									+ value
									+ '&zoom='
									+ (t.data('zoom') || 13)
									+ '&size='
									+ (t.data('size') || '200*200')
									+ '&key=' + key + '"/>');
				} else {
					holder.html('');
				}
			});
		}
	};

})(jQuery);

var geocoder;
var latlng_input;
var latlng_map;
var latlng_marker;

function latlng_initMaps() {
	latlng_map = new AMap.Map('_maps_container', {
				center : [104.0967, 35.6622],
				zoom : 3
			});
	latlng_map.on('click', function(event) {
				latlng_createOrMoveMarker(event.lnglat);
				latlng_setLatLng(event.lnglat);
			});
	latlng_map.on('rightclick', function(event) {
				latlng_setLatLng(event.lnglat);
				$('#_maps_window').dialog('close');
			});
	AMap.service(['AMap.Geocoder'], function() {
				geocoder = new AMap.Geocoder();
			});
	latlng_getLatLng();
}
function latlng_resetMaps() {
	if (latlng_marker != null) {
		latlng_marker.setMap(null);
		latlng_marker = null;
	}
	latlng_map.setZoom(3);
	latlng_map.setCenter(new AMap.LngLat(104.0967, 35.6622));
}
function latlng_createOrMoveMarker(latLng) {
	if (!latLng)
		return;
	if (typeof latLng == 'string') {
		var arr = latLng.split(',');
		latLng = new AMap.LngLat(parseFloat(arr[1]), parseFloat(arr[0]));
	}
	if (latlng_marker == null) {
		latlng_marker = new AMap.Marker({
					position : latLng,
					draggable : true,
					map : latlng_map
				});
		latlng_marker.on('dragend', function(event) {
					latlng_setLatLng(event.lnglat);
				});
	} else {
		latlng_marker.setPosition(latLng);
	}
	var zoom = latlng_input.data('zoom') || 8;
	if (latlng_map.getZoom() < zoom)
		latlng_map.setZoom(zoom);
	latlng_map.setCenter(latLng);
}
function latlng_setLatLng(latLng) {
	$(latlng_input).val(latLng.getLat().toFixed(6) + ','
			+ latLng.getLng().toFixed(6)).trigger('change').trigger('validate');
}
function latlng_getLatLng() {
	latlng_resetMaps();
	if (latlng_input) {
		if (latlng_input.val()) {
			latlng_createOrMoveMarker(latlng_input.val());
		} else if (latlng_input.data('address') && geocoder) {
			geocoder.getLocation(latlng_input.data('address'), function(status,
							result) {
						if (status == 'complete') {
							if (result.info == 'OK') {
								var geocodes = result.geocodes;
								if (geocodes && geocodes.length) {
									var pos = geocodes[0].location;
									latlng_setLatLng(pos);
									latlng_createOrMoveMarker(pos);
								} else {
									latlng_resetMaps();
								}
							} else {
								latlng_resetMaps();
							}
						}
					});
		} else if (latlng_input.data('regionselector')) {
			var coordinate;
			var region = $(latlng_input.data('regionselector'))
					.data('treenode');
			var zoom = latlng_input.data('zoom') || 8;
			if (region) {
				var coordinate = region.coordinate;
				while (!coordinate && region.parent) {
					region = region.parent;
					coordinate = region.coordinate;
					if (zoom > 5)
						zoom -= 2;
				}
			}
			if (coordinate) {
				latlng_map
						.setCenter(new AMap.LngLat(region.coordinate.longitude,
								region.coordinate.latitude));
				latlng_map.setZoom(zoom);
			}
		}
	}
}
Observation.latlng = function(container) {
	var c = $(container);
	var selector = 'input.latlng';
	c.is(selector) ? c.latlng() : $(selector, c).latlng();
};
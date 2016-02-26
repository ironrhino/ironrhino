(function($) {
	$.fn.latlng = function() {
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
				if (typeof google == 'undefined') {
					var script = document.createElement('script');
					script.src = 'http://www.google.com/jsapi?callback=latlng_loadMaps';
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
					holder
							.html('<img src="http://maps.googleapis.com/maps/api/staticmap?center='
									+ value
									+ '&zoom='
									+ (t.data('zoom') || 13)
									+ '&size='
									+ (t.data('size') || '200x200')
									+ '&sensor=false"/>');
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
function latlng_loadMaps() {
	if (typeof google != 'undefined' && typeof google.maps == 'undefined') {
		var other_params = 'sensor=true&region=CN';
		var key = $('meta[name="google-maps-key"]').attr('content');
		if (typeof GMAP_KEY != 'undefined')
			key = GMAP_KEY;
		if (key)
			other_params += '&key=' + key;
		google.load("maps", "3", {
					other_params : other_params,
					'callback' : latlng_initMaps
				});
	}
}
function latlng_initMaps() {
	geocoder = new google.maps.Geocoder();
	latlng_map = new google.maps.Map(
			document.getElementById('_maps_container'), {
				zoom : 3,
				mapTypeId : google.maps.MapTypeId.ROADMAP
			});
	google.maps.event.addListener(latlng_map, 'click', function(event) {
				latlng_createOrMoveMarker(event.latLng);
				latlng_setLatLng(event.latLng);
			});
	google.maps.event.addListener(latlng_map, 'rightclick', function(event) {
				latlng_setLatLng(event.latLng);
				$('#_maps_window').dialog('close');
			});
	latlng_getLatLng();
}
function latlng_resetMaps() {
	if (latlng_marker != null) {
		latlng_marker.setMap(null);
		latlng_marker = null;
	}
	latlng_map.setZoom(3);
	latlng_map.setCenter(new google.maps.LatLng(35.6622, 104.0967));
}
function latlng_createOrMoveMarker(latLng) {
	if (!latLng)
		return;
	if (typeof latLng == 'string') {
		var arr = latLng.split(',');
		latLng = new google.maps.LatLng(parseFloat(arr[0]), parseFloat(arr[1]));
	}
	if (latlng_marker == null) {
		latlng_marker = new google.maps.Marker({
					position : latLng,
					draggable : true,
					map : latlng_map
				});
		google.maps.event.addListener(latlng_marker, "dragend",
				function(event) {
					latlng_setLatLng(event.latLng);
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
	$(latlng_input)
			.val(latLng.lat().toFixed(6) + ',' + latLng.lng().toFixed(6))
			.trigger('change').trigger('validate');
}
function latlng_getLatLng() {
	latlng_resetMaps();
	if (latlng_input) {
		if (latlng_input.val())
			latlng_createOrMoveMarker(latlng_input.val());
		else if (latlng_input.data('address'))
			geocoder.geocode({
						'address' : latlng_input.data('address')
					}, function(results, status) {
						if (status == google.maps.GeocoderStatus.OK) {
							var pos = results[0].geometry.location;
							latlng_setLatLng(pos);
							latlng_createOrMoveMarker(pos);
						}
					});
		else if (latlng_input.data('regionselector')) {
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
						.setCenter(new google.maps.LatLng(
								region.coordinate.latitude,
								region.coordinate.longitude));
				latlng_map.setZoom(zoom);
			}
		}
	}

}
Observation.latlng = function(container) {
	$$('input.latlng', container).latlng();
};
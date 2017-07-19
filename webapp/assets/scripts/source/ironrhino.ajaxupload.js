(function($) {
	$.ajaxupload = function(files, options) {
		var options = options || {};
		var formdata;
		var data = options.data;
		if (data) {
			if (data.constructor === FormData) {
				// for $.fn.ajaxsubmit
				formdata = data;
			} else {
				formdata = new FormData();
				for (var key in data)
					formdata.append(key, data[key]);
			}
		} else {
			formdata = new FormData();
		}
		if ($.isArray(files)) {
			// for $.fn.ajaxsubmit
			$.each(files, function(i, v) {
						if (v.name && v.value)
							formdata.append(v.name, v.value);
						else
							formdata.append(options.name || 'file', v);
					});
		} else {
			for (var i = 0; i < files.length; i++)
				formdata.append(options.name || 'file', files[i]);
		}
		options.data = formdata;
		$.ajax($.extend(options, {
					contentType : false,
					processData : false,
					cache : false,
					type : 'POST'
				}));
	}
})(jQuery);
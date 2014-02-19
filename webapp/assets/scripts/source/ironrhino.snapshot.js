(function($) {
	var videoStream;
	var interval;
	$.snapshot = function(options) {
		options = options || {};
		if (!navigator.getUserMedia)
			navigator.getUserMedia = navigator.oGetUserMedia
					|| navigator.mozGetUserMedia
					|| navigator.webkitGetUserMedia || navigator.msGetUserMedia;
		if (!navigator.getUserMedia) {
			var error = MessageBundle.get('unsupported.browser');
			if (options.onerror)
				options.onerror(error);
			else
				alert(error);
			return;
		}
		if (typeof Indicator.show != 'undefined')
			Indicator.show();
		navigator
				.getUserMedia(
						{
							video : true
						},
						function(stream) {
							videoStream = stream;
							if (typeof Indicator.hide != 'undefined')
								Indicator.hide();
							var container = options.container;
							if (!container) {
								var modal = $(
										'<div id="snapshot-modal" class="modal" style="z-index:10000;"><div class="modal-close"><button type="button" class="close" data-dismiss="modal" aria-hidden="true">×</button></div><div id="snapshot-modal-body" class="modal-body" style="max-height:600px;"></div></div>')
										.appendTo(document.body).find(
												'button.close').click(
												function() {
													destroy();
												});
								container = 'snapshot-modal-body';
							}
							if (typeof container == 'string')
								container = document.getElementById(container);
							var video = document
									.getElementById('snapshot-video');
							if (!video) {
								video = document.createElement('video');
								video.id = 'snapshot-video';
								video.autoplay = true;
								video.style.cursor = 'pointer';
								video.style.width = '100%';
								container.appendChild(video);
							}
							if (options.onsnapshot) {
								video
										.addEventListener(
												'click',
												function(e) {
													video.style.zIndex = '2000';
													video.style.width = '20%';
													video.style.position = 'absolute';
													var canvas = document
															.getElementById('snapshot-canvas');
													if (!canvas) {
														canvas = document
																.createElement('canvas');
														canvas.id = 'snapshot-canvas';
														canvas.style.cursor = 'pointer';
														canvas.style.width = '100%';
														container
																.appendChild(canvas);
													}
													canvas.width = this.videoWidth;
													canvas.height = this.videoHeight;
													canvas.getContext('2d')
															.drawImage(video,
																	0, 0);
													canvas.setAttribute(
															'data-timestamp',
															new Date()
																	.getTime());
													canvas
															.addEventListener(
																	'click',
																	function() {

																		options
																				.onsnapshot(
																						this,
																						parseInt(this
																								.getAttribute('data-timestamp')));
																		destroy();
																	});
												});
								video.addEventListener('dblclick', function(e) {
									var canvas = document
											.createElement('canvas');
									canvas.getContext('2d').drawImage(video, 0,
											0);
									options.onsnapshot(canvas, new Date()
											.getTime());
									canvas = null;
									destroy();
								});
							} else if (options.oncapture) {
								interval = setInterval(function() {
									var canvas = document
											.getElementById('snapshot-canvas');
									if (!canvas) {
										canvas = document
												.createElement('canvas');
										canvas.id = 'snapshot-canvas';
										canvas.style.display = 'none';
										container.appendChild(canvas);
									}
									canvas.width = video.videoWidth;
									canvas.height = video.videoHeight;
									canvas.getContext('2d').drawImage(video, 0,
											0);
									options.oncapture(canvas, destroy);
								}, 1000);
							}
							video.onerror = function() {
								if (video)
									destroy();
							};
							// stream.onended = noStream;
							if (window.webkitURL)
								video.src = window.webkitURL
										.createObjectURL(stream);
							else if (video.mozSrcObject !== undefined) {// FF18a
								video.mozSrcObject = stream;
								video.play();
							} else if (navigator.mozGetUserMedia) {// FF16a,
																	// 17a
								video.src = stream;
								video.play();
							} else if (window.URL)
								video.src = window.URL.createObjectURL(stream);
							else
								video.src = stream;
						}, function() {
							if (typeof Indicator.hide != 'undefined')
								Indicator.hide();
							var error = MessageBundle.get('action.denied');
							if (options.onerror)
								options.onerror(error);
							else
								alert(error);
						});
	}

	function destroy() {
		if (interval) {
			clearInterval(interval);
			interval = null;
		}
		var video = document.getElementById('snapshot-video');
		var canvas = document.getElementById('snapshot-canvas');
		if (canvas)
			canvas.parentNode.removeChild(canvas);
		if (videoStream) {
			if (videoStream.stop)
				videoStream.stop();
			else if (videoStream.msStop)
				videoStream.msStop();
			videoStream.onended = null;
			videoStream = null;
		}
		video.onerror = null;
		video.pause();
		if (video.mozSrcObject)
			video.mozSrcObject = null;
		if (window.URL)
			window.URL.revokeObjectURL(video.src);
		video.src = '';
		video.parentNode.removeChild(video);
		var modal = document.getElementById('snapshot-modal');
		if (modal)
			modal.parentNode.removeChild(modal);
	}
})(jQuery);
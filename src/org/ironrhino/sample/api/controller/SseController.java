package org.ironrhino.sample.api.controller;

import java.time.LocalTime;
import java.util.concurrent.ExecutorService;

import org.ironrhino.core.metadata.Authorize;
import org.ironrhino.core.security.role.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Controller
@RequestMapping("/sse")
@Authorize(ifAnyGranted = UserRole.ROLE_ADMINISTRATOR)
public class SseController {

	@Autowired
	private ExecutorService executorService;

	@GetMapping("/sample")
	public ModelAndView sample() {
		return new ModelAndView("sample/event");
	}

	@GetMapping(path = "/event", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public ResponseBodyEmitter event() {
		final SseEmitter emitter = new SseEmitter();
		executorService.execute(() -> {
			for (int i = 0; i < 10; i++) {
				try {
					emitter.send(LocalTime.now().toString(), MediaType.TEXT_PLAIN);
					Thread.sleep(200);
				} catch (Exception e) {
					e.printStackTrace();
					emitter.completeWithError(e);
					return;
				}
			}
			emitter.complete();
		});
		return emitter;
	}

}
package org.ironrhino.batch.tasklet.http;

import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Setter
@Slf4j
public class WebhookTask implements Tasklet {

	private URI url;

	private String body = "";

	private Map<String, String> headers = Collections.singletonMap(HttpHeaders.CONTENT_TYPE,
			MediaType.APPLICATION_JSON_VALUE);

	private HttpMethod method = HttpMethod.POST;

	private Pattern responseFailurePattern;

	private boolean suppressFailure;

	@Override
	public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
		RestTemplate rt = new RestTemplate();
		HttpHeaders httpHeaders = new HttpHeaders();
		headers.forEach((k, v) -> {
			httpHeaders.add(k, v);
		});
		RequestEntity<String> request = new RequestEntity<>(body, httpHeaders, method, url);
		try {
			ResponseEntity<String> response = rt.exchange(request, String.class);
			String responseBody = response.getBody();
			if (isFailure(responseBody)) {
				throw new UnexpectedJobExecutionException(
						String.format("Requested %s with [%s] and received [%s] with status code %d", url, body,
								responseBody, response.getStatusCodeValue()));
			} else {
				log.info("Requested {} with [{}] and received [{}]", url, body, responseBody);
			}
		} catch (HttpStatusCodeException e) {
			if (suppressFailure)
				log.error("Requested {} with [{}] and received [{}] with status code {}", url, body,
						e.getResponseBodyAsString(), e.getRawStatusCode());
			else
				throw e;
		}
		return RepeatStatus.FINISHED;
	}

	protected boolean isFailure(String responseBody) {
		if (responseFailurePattern != null)
			return responseFailurePattern.matcher(responseBody).find();
		return false;
	}

}

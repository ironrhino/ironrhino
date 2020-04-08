package org.ironrhino.batch.tasklet.http;

import java.io.File;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Setter
@Slf4j
public class UploadTask implements Tasklet {

	private URI url;

	private File file;

	private String fieldName;

	private Map<String, String> headers = Collections.emptyMap();

	private Pattern responseFailurePattern;

	private boolean suppressFailure;

	@Override
	public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
		RestTemplate rt = new RestTemplate();
		HttpHeaders httpHeaders = new HttpHeaders();
		headers.forEach((k, v) -> {
			httpHeaders.add(k, v);
		});
		RequestEntity<Object> request;
		if (fieldName == null) {
			request = new RequestEntity<>(new FileSystemResource(file), httpHeaders, HttpMethod.POST, url);
		} else {
			httpHeaders.set(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA_VALUE);
			MultiValueMap<String, Object> requestParams = new LinkedMultiValueMap<>();
			requestParams.add(fieldName, new FileSystemResource(file));
			request = new RequestEntity<>(requestParams, httpHeaders, HttpMethod.POST, url);
		}
		validate(rt.exchange(request, String.class));
		return RepeatStatus.FINISHED;
	}

	protected void validate(ResponseEntity<String> response) throws Exception {
		String responseBody = response.getBody();
		if (response.getStatusCode().is2xxSuccessful() && !isFailure(responseBody)) {
			log.info("Uploaded {} to {} and received [{}]", file, url, responseBody);
		} else {
			if (suppressFailure) {
				log.error("Uploaded {} to {} and received [{}] with status code {}", file, url, responseBody,
						response.getStatusCodeValue());
			} else {
				throw new UnexpectedJobExecutionException(
						String.format("Uploaded %s to %s and received [%s] with status code %d", file.toString(), url,
								responseBody, response.getStatusCodeValue()));
			}
		}
	}

	protected boolean isFailure(String responseBody) {
		if (responseFailurePattern != null)
			return responseFailurePattern.matcher(responseBody).find();
		return false;
	}

}

package api.oauth;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ironrhino.core.util.JsonUtils;
import org.ironrhino.security.oauth.server.component.OAuthAccessUnauthorizedHandler;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import api.RestStatus;

@Component
public class OAuthAccessUnauthorizedHandlerImpl implements
		OAuthAccessUnauthorizedHandler {

	@Override
	public void handle(HttpServletRequest request,
			HttpServletResponse response, String message) {
		RestStatus rs = RestStatus.valueOf(RestStatus.CODE_ACCESS_UNAUTHORIZED,
				message);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		try {
			response.getWriter().write(JsonUtils.toJson(rs));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}

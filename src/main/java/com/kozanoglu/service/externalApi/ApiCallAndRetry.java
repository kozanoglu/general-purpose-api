package com.kozanoglu.service.externalApi;

import java.util.List;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import static java.util.Collections.singletonList;

class ApiCallAndRetry<T>
{
	private static final Log LOGGER = LogFactory.getLog(ApiCallAndRetry.class);

	private static final List<HttpStatus.Series> ERROR_STATUS = singletonList(HttpStatus.Series.SERVER_ERROR);

	CallResponse<T> invoke(Callable<ResponseEntity<T>> callable, int retryAttempts, int readTimeOut) throws Exception
	{
		ResponseEntity<T> response = null;
		HttpStatus.Series status = null;
		String error = null;
		int retry = 0;
		while ((status == null || ERROR_STATUS.contains(status)) && retry++ < retryAttempts)
		{
			try
			{
				response = callable.call();
				status = response.getStatusCode().series();
			}
			catch (HttpClientErrorException e)
			{
				LOGGER.error(e);
				status = e.getStatusCode().series();
				error = e.getResponseBodyAsString();
			}
			catch (ResourceAccessException e)
			{
				LOGGER.error(e);
				status = null;
				error = e.getMessage();
				Thread.sleep(readTimeOut);
			}
		}
		return new CallResponse<>(status, response, error);
	}
}

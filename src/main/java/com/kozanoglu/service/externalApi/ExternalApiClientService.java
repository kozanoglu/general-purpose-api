package com.kozanoglu.service.externalApi;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import static java.lang.System.lineSeparator;

@Service
public class ExternalApiClientService
{
	private static final Log LOGGER = LogFactory.getLog(ExternalApiClientService.class);

	@Value("${external.url}")
	private String visualMetaApiUrl;

	@Value("${external.oauth_consumer_key}")
	private String oauthConsumerKey;

	@Value("${external.oauth_signature}")
	private String oauthSignature;

	@Value("${external.read_timeout}")
	private int readTimeout;

	@Value("${external.retry_attempts}")
	private int retryAttempts;

	private final RestTemplate restTemplate;

	@Autowired
	public ExternalApiClientService(@Qualifier("externalAPIRestTemplate") RestTemplate restTemplate)
	{
		this.restTemplate = restTemplate;
	}

	/**
	 * Calls the External API to create an object and returns the response.
	 *
	 * @param endpointUrl  the relative URL for the External API endpoint to call (e.g. "shops/de")
	 * @param requestBody  the object to create
	 * @param responseType type of the result expected from the call (normally the type of the created object)
	 * @return the External API's response (normally the created object)
	 */
	public <T> T create(String endpointUrl, Object requestBody, Class<T> responseType)
	{
		return callExternalApiForObject(endpointUrl, HttpMethod.POST, requestBody, responseType);
	}

	/**
	 * Calls the External API to retrieve a collection of {@code responseType} objects and returns them.
	 *
	 * @param endpointUrl  the relative URL for the External API endpoint to call (e.g. "shops/de")
	 * @param responseType type of the results expected from the call
	 * @return the {@code responseType} objects retrieved, or an empty collection if no results
	 */
	public <T> Collection<T> retrieve(String endpointUrl, Class<T> responseType)
	{
		return callExternalApiForCollection(endpointUrl, HttpMethod.GET, null, responseType);
	}

	/**
	 * Calls the External API to retrieve a collection of resources for {@code responseType} objects and returns them.
	 *
	 * @param endpointUrl  the relative URL for the External API endpoint to call (e.g. "shops/de")
	 * @param responseType type of the results expected from the call
	 * @return the {@code responseType} objects retrieved, or an empty collection if no results
	 */
	public <T> Collection<Resource<T>> retrieveResources(String endpointUrl, Class<T> responseType)
	{
		return callExternalApiForResources(endpointUrl, HttpMethod.GET, null, responseType);
	}

	/**
	 * Calls the External API to update an object and returns the response.
	 *
	 * @param endpointUrl  the relative URL for the External API endpoint to call (e.g. "shops/de/11")
	 * @param requestBody  the object to update
	 * @param responseType type of the result expected from the call (normally the type of the updated object)
	 * @return the External API's response (normally the updated object)
	 */
	public <T> T update(String endpointUrl, Object requestBody, Class<T> responseType)
	{
		return callExternalApiForObject(endpointUrl, HttpMethod.PUT, requestBody, responseType);
	}

	/**
	 * Calls the External API to delete an object.
	 *
	 * @param endpointUrl the relative URL for the VM API endpoint to call (e.g. "shops/de/11")
	 */
	public void delete(String endpointUrl)
	{
		callExternalApiForObject(endpointUrl, HttpMethod.DELETE, null, Void.class);
	}

	/**
	 * Calls the External API from the provided URL, using specified the HTTP method, connection and read timeouts, and returns the response.
	 *
	 * @param url           the relative URL to connect to External API (e.g. "shops/de" for getting list of shops)
	 * @param httpMethod    the HTTP method to execute
	 * @param requestObject the {@link HttpEntity} body
	 * @param responseType  the type of response expected from the call
	 * @return {@code responseType} instance returned from the provided URL, or {@code null} if no result
	 */
	private <T> T callExternalApiForObject(String url, HttpMethod httpMethod, Object requestObject, Class<T> responseType)
	{
		String ExternalApiUrl = createVisualMetaApiUrl(url);
		LOGGER.info(String.format("Calling External API at [%s]", ExternalApiUrl));
		HttpEntity entity = createHttpEntity(requestObject);
		try
		{
			Callable<ResponseEntity<T>> callable = () -> restTemplate.exchange(ExternalApiUrl, httpMethod, entity, responseType);
			CallResponse<T> callResponse = new ApiCallAndRetry<T>().invoke(callable, retryAttempts, readTimeout);

			if (callResponse.getResponse() != null)
			{
				HttpStatus responseStatus = callResponse.getResponse().getStatusCode();
				LOGGER.info(
					String.format("External API call response status is [%d - %s]", responseStatus.value(), responseStatus.getReasonPhrase()));
			}

			if (callResponse.getStatus() == HttpStatus.Series.SUCCESSFUL)
			{
				ResponseEntity<T> response = callResponse.getResponse();
				if (response.getBody() != null)
				{
					T responseResult = response.getBody();
					LOGGER.info(String.format("External API call returned [%d] results for URL [%s]", 1, ExternalApiUrl));
					return responseResult;
				}
				else
				{
					LOGGER.info(String.format("External API call returned no results for URL [%s]", ExternalApiUrl));
					return null;
				}
			}
			else
			{
				LOGGER.error("Error");
			}
		}
		catch (Exception e)
		{
			LOGGER.error(String.format("Couldn't call External API at [%s]", ExternalApiUrl), e);
		}

		return null;
	}

	/**
	 * Looks up a collection of {@code responseType} instances from the provided URL, using specified the HTTP method, connection and read timeouts.
	 *
	 * @param url           the relative URL to connect to External API (e.g. "shops/de" for getting list of shops)
	 * @param httpMethod    the HTTP method to execute
	 * @param requestObject the {@link HttpEntity} body
	 * @param responseType  the type of response expected from the call
	 * @return the collection of {@code responseType} instances looked up from the provided URL
	 */
	private <T> Collection<T> callExternalApiForCollection(String url, HttpMethod httpMethod, Object requestObject, Class<T> responseType)
	{
		String ExternalApiUrl = createVisualMetaApiUrl(url);
		LOGGER.info(String.format("Calling External API at [%s]", ExternalApiUrl));
		HttpEntity entity = createHttpEntity(requestObject);
		ParameterizedTypeReference<Resources<T>> parameterizedType = createParameterizedTypeReference(responseType);
		try
		{
			Callable<ResponseEntity<Resources<T>>> callable = () -> restTemplate.exchange(ExternalApiUrl, httpMethod, entity, parameterizedType);
			CallResponse<Resources<T>> callResponse = new ApiCallAndRetry<Resources<T>>().invoke(callable, retryAttempts, readTimeout);

			if (callResponse.getResponse() != null)
			{
				HttpStatus responseStatus = callResponse.getResponse().getStatusCode();
				LOGGER.info(
					String.format("External API call response status is [%d - %s]", responseStatus.value(), responseStatus.getReasonPhrase()));
			}

			if (callResponse.getStatus() == HttpStatus.Series.SUCCESSFUL)
			{
				ResponseEntity<Resources<T>> response = callResponse.getResponse();
				if (response.getBody() != null)
				{
					Collection<T> responseResult = response.getBody().getContent();
					LOGGER.info(String.format("External API call returned [%d] results for URL [%s]", responseResult.size(), ExternalApiUrl));
					return responseResult;
				}
				else
				{
					LOGGER.info(String.format("External API call returned no results for URL [%s]", ExternalApiUrl));
					return null;
				}
			}
			else
			{
				LOGGER.error("Error");
			}
		}
		catch (Exception e)
		{
			LOGGER.error(String.format("Couldn't call External API at [%s]", ExternalApiUrl), e);
		}

		return Collections.emptyList();
	}

	private <T> Collection<Resource<T>> callExternalApiForResources(String url, HttpMethod httpMethod, Object requestObject, Class<T> responseType)
	{
		String ExternalApiUrl = createVisualMetaApiUrl(url);
		LOGGER.info(String.format("Calling External API at [%s]", ExternalApiUrl));
		HttpEntity entity = createHttpEntity(requestObject);
		ParameterizedTypeReference<Resources<Resource<T>>> parameterizedType = createParameterizedResourceTypeReference(responseType);
		try
		{
			Callable<ResponseEntity<Resources<Resource<T>>>> callable = () -> restTemplate.exchange(ExternalApiUrl, httpMethod, entity, parameterizedType);
			CallResponse<Resources<Resource<T>>> callResponse = new ApiCallAndRetry<Resources<Resource<T>>>().invoke(callable, retryAttempts,
				readTimeout);

			if (callResponse.getResponse() != null)
			{
				HttpStatus responseStatus = callResponse.getResponse().getStatusCode();
				LOGGER.info(
					String.format("External API call response status is [%d - %s]", responseStatus.value(), responseStatus.getReasonPhrase()));
			}

			if (callResponse.getStatus() == HttpStatus.Series.SUCCESSFUL)
			{
				ResponseEntity<Resources<Resource<T>>> response = callResponse.getResponse();
				if (response.getBody() != null)
				{
					Collection<Resource<T>> responseResult = response.getBody().getContent();
					LOGGER.info(String.format("External API call returned [%d] results for URL [%s]", responseResult.size(), ExternalApiUrl));
					return responseResult;
				}
				else
				{
					LOGGER.info(String.format("External API call returned no results for URL [%s]", ExternalApiUrl));
					return null;
				}
			}
			else
			{
				LOGGER.error("Error");
			}
		}
		catch (Exception e)
		{
			LOGGER.error(String.format("Couldn't call External API at [%s]", ExternalApiUrl), e);
		}

		return Collections.emptyList();
	}

	private String createVisualMetaApiUrl(String url)
	{
		return String.format("%s/%s", visualMetaApiUrl, url);
	}

	private <T> ParameterizedTypeReference<Resources<T>> createParameterizedTypeReference(Class<T> responseType)
	{
		return new ParameterizedTypeReference<Resources<T>>()
		{
			@Override
			public Type getType()
			{
				return new ExternalParameterizedType((ParameterizedType) super.getType(), new Type[] { responseType });
			}
		};
	}

	private <T> ParameterizedTypeReference<Resources<Resource<T>>> createParameterizedResourceTypeReference(Class<T> responseType)
	{
		return new ParameterizedTypeReference<Resources<Resource<T>>>()
		{
			@Override
			public Type getType()
			{
				ParameterizedType resourcesType = new ExternalParameterizedType((ParameterizedType) super.getType(), new Type[] { Resources.class });
				ParameterizedType resourceType = new ExternalParameterizedType((ParameterizedType) super.getType(), new Type[] { Resource.class })
				{
					@Override
					public Type getRawType()
					{
						return Resource.class;
					}
				};
				return new ExternalParameterizedType(resourcesType, new Type[] { new ExternalParameterizedType(resourceType, new Type[] { responseType }) });
			}
		};
	}

	private HttpEntity createHttpEntity(Object requestObject)
	{
		HttpHeaders headers = createHttpHeaders();
		return new HttpEntity<>(requestObject, headers);
	}

	private HttpHeaders createHttpHeaders()
	{
		HttpHeaders headers = new HttpHeaders();
		headers.set("Authorization ", "OAuth");
		headers.set("oauth_version", "1.0");
		headers.set("oauth_signature_method", "HMAC-SHA1");
		headers.set("oauth_consumer_key", oauthConsumerKey);
		headers.set("oauth_signature", oauthSignature);
		return headers;
	}

	private String createErrorMailMessage(String url, HttpMethod httpMethod, CallResponse callResponse)
	{
		StringBuilder message = new StringBuilder("Url: ").append(url).append(lineSeparator());
		message.append("Method: ").append(httpMethod).append(lineSeparator());
		if (callResponse != null && callResponse.getStatus() != null)
		{
			message.append("Api Status: ").append(callResponse.getStatus()).append(lineSeparator());
		}
		if (callResponse != null && callResponse.getError() != null)
		{
			message.append("Response Body : ").append(callResponse.getError()).append(lineSeparator());
		}
		return message.toString();
	}

	void setRetryAttempts(int retryAttempts)
	{
		this.retryAttempts = retryAttempts;
	}

	private class ExternalParameterizedType implements ParameterizedType
	{
		private ParameterizedType delegate;
		private Type[] actualTypeArguments;

		ExternalParameterizedType(ParameterizedType delegate, Type[] actualTypeArguments)
		{
			this.delegate = delegate;
			this.actualTypeArguments = actualTypeArguments;
		}

		@Override
		public Type[] getActualTypeArguments()
		{
			return actualTypeArguments;
		}

		@Override
		public Type getRawType()
		{
			return delegate.getRawType();
		}

		@Override
		public Type getOwnerType()
		{
			return delegate.getOwnerType();
		}
	}
}
package com.kozanoglu.config;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.hal.Jackson2HalModule;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class RestTemplateConfig
{
	@Value("${external.read_timeout}")
	private int readTimeout;

	@Value("${external.connect_timeout}")
	private int connectTimeout;

	@Value("${external.connection_request_timeout}")
	private int connectionRequestTimeout;

	@Bean(name = "externalAPIRestTemplate")
	public RestTemplate getRestTemplate()
	{
		HttpComponentsClientHttpRequestFactory httpRequestFactory = new HttpComponentsClientHttpRequestFactory();
		httpRequestFactory.setReadTimeout(readTimeout);
		httpRequestFactory.setConnectTimeout(connectTimeout);
		httpRequestFactory.setConnectionRequestTimeout(connectionRequestTimeout);

		RestTemplate restTemplate = new RestTemplate(httpRequestFactory);
		MappingJackson2HttpMessageConverter messageConverter = createJackson2HttpMessageConverter();
		restTemplate.setMessageConverters(Collections.singletonList(messageConverter));
		return restTemplate;
	}

	private MappingJackson2HttpMessageConverter createJackson2HttpMessageConverter()
	{
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.registerModule(new Jackson2HalModule());
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		MappingJackson2HttpMessageConverter messageConverter = new MappingJackson2HttpMessageConverter();
		messageConverter.setObjectMapper(objectMapper);
		messageConverter.setSupportedMediaTypes(Collections.singletonList(MediaType.APPLICATION_JSON));
		return messageConverter;
	}
}

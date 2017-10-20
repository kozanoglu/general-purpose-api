package com.kozanoglu.service.externalApi;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class CallResponse<T>
{
	private HttpStatus.Series status;
	private ResponseEntity<T> response;
	private String error;

	CallResponse(HttpStatus.Series status, ResponseEntity<T> response, String error)
	{
		this.status = status;
		this.response = response;
		this.error = error;
	}

	HttpStatus.Series getStatus()
	{
		return status;
	}

	ResponseEntity<T> getResponse()
	{
		return response;
	}

	String getError()
	{
		return error;
	}
}

package de.symeda.sormas.rest.exception.mappers;

import jakarta.persistence.EntityNotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;

@Provider
public class EntityNotFoundExceptionMapper implements ExceptionMapper<EntityNotFoundException> {

	@Override
	public Response toResponse(EntityNotFoundException exception) {
		String message = exception.getLocalizedMessage();
		return Response.status(HttpStatus.SC_NOT_FOUND)
			.entity(StringUtils.isNotBlank(message) ? message : "The requested entity was not found.")
			.build();
	}
}

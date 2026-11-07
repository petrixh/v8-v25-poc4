package de.symeda.sormas.rest.audit;

import de.symeda.sormas.api.FacadeProvider;
import org.glassfish.jersey.server.ContainerRequest;

import jakarta.annotation.Priority;
import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.SessionContext;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;

@Provider
@Priority(Priorities.USER)
public class RequestAuditFilter implements ContainerRequestFilter {

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		String actionMethod = requestContext.getMethod();

		FacadeProvider.getAuditLoggerFacade().logRestCall(((ContainerRequest) requestContext).getPath(true), actionMethod);
	}
}

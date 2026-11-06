package de.symeda.sormas.rest.resources;

import java.util.Date;
import java.util.List;
import java.util.function.UnaryOperator;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.environment.EnvironmentDto;
import de.symeda.sormas.rest.resources.base.EntityDtoResource;

@Path("/environments")
@Produces(MediaType.APPLICATION_JSON + "; charset=UTF-8")
@Consumes(MediaType.APPLICATION_JSON + "; charset=UTF-8")
public class EnvironmentResource extends EntityDtoResource<EnvironmentDto> {

	@GET
	@Path("/all/{since}")
	public List<EnvironmentDto> getAllEnvironments(@PathParam("since") long since) {
		return FacadeProvider.getEnvironmentFacade().getAllAfter(new Date(since));
	}

	@GET
	@Path("/all/{since}/{size}/{lastSynchronizedUuid}")
	public List<EnvironmentDto> getAllEnvironments(
		@PathParam("since") long since,
		@PathParam("size") int size,
		@PathParam("lastSynchronizedUuid") String lastSynchronizedUuid) {
		return FacadeProvider.getEnvironmentFacade().getAllAfter(new Date(since), size, lastSynchronizedUuid);
	}

	@POST
	@Path("/query")
	public List<EnvironmentDto> getByUuids(List<String> uuids) {
		return FacadeProvider.getEnvironmentFacade().getByUuids(uuids);
	}

	@GET
	@Path("/uuids")
	public List<String> getAllUuids() {
		return FacadeProvider.getEnvironmentFacade().getAllActiveUuids();
	}

	@GET
	@Path("/{uuid}")
	public EnvironmentDto getByUuid(@PathParam("uuid") String uuid) {
		return FacadeProvider.getEnvironmentFacade().getEnvironmentByUuid(uuid);
	}

	@GET
	@Path("/obsolete/{since}")
	public List<String> getObsoleteUuidsSince(@PathParam("since") long since) {
		return FacadeProvider.getEnvironmentFacade().getObsoleteUuidsSince(new Date(since));
	}

	@Override
	public UnaryOperator<EnvironmentDto> getSave() {
		return FacadeProvider.getEnvironmentFacade()::save;
	}

	@Override
	public Response postEntityDtos(List<EnvironmentDto> environmentDtos) {
		return super.postEntityDtos(environmentDtos);
	}
}

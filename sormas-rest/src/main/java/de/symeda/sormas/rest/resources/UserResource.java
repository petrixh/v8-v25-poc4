/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2023 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package de.symeda.sormas.rest.resources;

import java.util.Date;
import java.util.List;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.caze.CriteriaWithSorting;
import de.symeda.sormas.api.common.Page;
import de.symeda.sormas.api.task.TaskContextIndexCriteria;
import de.symeda.sormas.api.user.UserCriteria;
import de.symeda.sormas.api.user.UserDto;
import de.symeda.sormas.api.user.UserReferenceWithTaskNumbersDto;
import de.symeda.sormas.api.user.UserRight;
import io.swagger.v3.oas.annotations.parameters.RequestBody;

/**
 * @see <a href="https://jersey.java.net/documentation/latest/">Jersey documentation</a>
 * @see <a href="https://jersey.java.net/documentation/latest/jaxrs-resources.html#d0e2051">Jersey documentation HTTP Methods</a>
 *
 */
@Path("/users")
@Produces(MediaType.APPLICATION_JSON + "; charset=UTF-8")
@Consumes(MediaType.APPLICATION_JSON + "; charset=UTF-8")
public class UserResource {

	@GET
	@Path("/all/{since}")
	public List<UserDto> getAll(@PathParam("since") long since) {
		return FacadeProvider.getUserFacade().getAllAfter(new Date(since));
	}

	@POST
	@Path("/query")
	public List<UserDto> getByUuids(List<String> uuids) {
		return FacadeProvider.getUserFacade().getByUuids(uuids);
	}

	@GET
	@Path("/uuids")
	public List<String> getAllUuids() {
		return FacadeProvider.getUserFacade().getAllUuids();
	}

	@POST
	@Path("/indexList")
	public Page<UserDto> getIndexList(
		@RequestBody CriteriaWithSorting<UserCriteria> criteriaWithSorting,
		@QueryParam("offset") int offset,
		@QueryParam("size") int size) {
		return FacadeProvider.getUserFacade().getIndexPage(criteriaWithSorting.getCriteria(), offset, size, criteriaWithSorting.getSortProperties());
	}

	@GET
	@Path("/{uuid}")
	public UserDto getByUuid(@PathParam("uuid") String uuid) {
		return FacadeProvider.getUserFacade().getByUuid(uuid);
	}

	@POST
	@Path("/userReferenceWithNoOfTask")
	public List<UserReferenceWithTaskNumbersDto> getUsersWithTaskNumbers(@RequestBody TaskContextIndexCriteria taskContextIndexCriteria) {
		return FacadeProvider.getUserFacade().getAssignableUsersWithTaskNumbers(taskContextIndexCriteria);
	}

	@GET
	@Path("/rights")
	public List<UserRight> getUserRightsForCurrentUser() {
		return FacadeProvider.getUserFacade().getUserRights(null);
	}

	@GET
	@Path("/rights/{uuid}")
	public List<UserRight> getUserRights(@PathParam("uuid") String uuid) {
		return FacadeProvider.getUserFacade().getUserRights(uuid);
	}
}

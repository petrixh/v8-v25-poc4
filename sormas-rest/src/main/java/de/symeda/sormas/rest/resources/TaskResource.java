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
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.caze.CriteriaWithSorting;
import de.symeda.sormas.api.common.Page;
import de.symeda.sormas.api.common.progress.ProcessedEntity;
import de.symeda.sormas.api.task.TaskCriteria;
import de.symeda.sormas.api.task.TaskDto;
import de.symeda.sormas.api.task.TaskIndexDto;
import de.symeda.sormas.rest.resources.base.EntityDtoResource;
import io.swagger.v3.oas.annotations.parameters.RequestBody;

/**
 * @see <a href="https://jersey.java.net/documentation/latest/">Jersey
 *      documentation</a>
 * @see <a href=
 *      "https://jersey.java.net/documentation/latest/jaxrs-resources.html#d0e2051">Jersey
 *      documentation HTTP Methods</a>
 *
 */
@Path("/tasks")
@Produces(MediaType.APPLICATION_JSON + "; charset=UTF-8")
@Consumes(MediaType.APPLICATION_JSON + "; charset=UTF-8")
public class TaskResource extends EntityDtoResource<TaskDto> {

	@GET
	@Path("/all/{since}")
	public List<TaskDto> getAll(@PathParam("since") long since) {
		return FacadeProvider.getTaskFacade().getAllActiveTasksAfter(new Date(since));
	}

	@GET
	@Path("/all/{since}/{size}/{lastSynchronizedUuid}")
	public List<TaskDto> getAll(
		@PathParam("since") long since,
		@PathParam("size") int size,
		@PathParam("lastSynchronizedUuid") String lastSynchronizedUuid) {
		return FacadeProvider.getTaskFacade().getAllActiveTasksAfter(new Date(since), size, lastSynchronizedUuid);
	}

	@POST
	@Path("/query")
	public List<TaskDto> getByUuids(List<String> uuids) {
		return FacadeProvider.getTaskFacade().getByUuids(uuids);
	}

	@GET
	@Path("/uuids")
	public List<String> getAllActiveUuids() {
		return FacadeProvider.getTaskFacade().getAllActiveUuids();
	}

	@POST
	@Path("/indexList")
	public Page<TaskIndexDto> getIndexList(
		@RequestBody CriteriaWithSorting<TaskCriteria> criteriaWithSorting,
		@QueryParam("offset") int offset,
		@QueryParam("size") int size) {
		return FacadeProvider.getTaskFacade().getIndexPage(criteriaWithSorting.getCriteria(), offset, size, criteriaWithSorting.getSortProperties());
	}

	@GET
	@Path("/{uuid}")
	public TaskDto getByUuid(@PathParam("uuid") String uuid) {
		return FacadeProvider.getTaskFacade().getTaskByUuid(uuid);
	}

	@GET
	@Path("/archived/{since}")
	public List<String> getArchivedUuidsSince(@PathParam("since") long since) {
		return FacadeProvider.getTaskFacade().getArchivedUuidsSince(new Date(since));
	}

	@GET
	@Path("/obsolete/{since}")
	public List<String> getObsoleteUuidsSince(@PathParam("since") long since) {
		return FacadeProvider.getTaskFacade().getObsoleteUuidsSince(new Date(since));
	}

	@POST
	@Path("/delete")
	public List<String> delete(List<String> uuids) {
		return FacadeProvider.getTaskFacade().delete(uuids).stream().map(ProcessedEntity::getEntityUuid).collect(Collectors.toList());
	}

	@Override
	public UnaryOperator<TaskDto> getSave() {
		return FacadeProvider.getTaskFacade()::saveTask;
	}

	@Override
	public Response postEntityDtos(List<TaskDto> taskDtos) {
		return super.postEntityDtos(taskDtos);
	}
}

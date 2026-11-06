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

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.infrastructure.InfrastructureChangeDatesDto;
import de.symeda.sormas.api.infrastructure.InfrastructureSyncDto;

@Path("/infrastructure")
@Produces(MediaType.APPLICATION_JSON + "; charset=UTF-8")
public class InfrastructureResource {

	@POST
	@Path("/sync")
	public InfrastructureSyncDto getInfrastructureSyncData(InfrastructureChangeDatesDto changeDates) {
		return FacadeProvider.getInfrastructureSyncFacade().getInfrastructureSyncData(changeDates);
	}
}

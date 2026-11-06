package de.symeda.sormas.api.infrastructure;

import jakarta.ejb.Remote;

@Remote
public interface ClientInfraSyncFacade {

	InfrastructureSyncDto getInfrastructureSyncData(InfrastructureChangeDatesDto changeDates);
}

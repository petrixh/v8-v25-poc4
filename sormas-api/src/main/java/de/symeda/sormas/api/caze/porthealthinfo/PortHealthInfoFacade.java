package de.symeda.sormas.api.caze.porthealthinfo;

import jakarta.ejb.Remote;

@Remote
public interface PortHealthInfoFacade {

	PortHealthInfoDto getByCaseUuid(String caseUuid);
}

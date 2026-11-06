package de.symeda.sormas.backend.systemevent;

import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;

import de.symeda.sormas.backend.common.BaseAdoService;

@Stateless
@LocalBean
public class SystemEventService extends BaseAdoService<SystemEvent> {

	public SystemEventService() {
		super(SystemEvent.class);
	}

}

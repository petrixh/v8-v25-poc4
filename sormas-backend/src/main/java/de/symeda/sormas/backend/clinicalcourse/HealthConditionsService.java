package de.symeda.sormas.backend.clinicalcourse;

import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;

import de.symeda.sormas.backend.common.BaseAdoService;

@Stateless
@LocalBean
public class HealthConditionsService extends BaseAdoService<HealthConditions> {

	public HealthConditionsService() {
		super(HealthConditions.class);
	}

}

package de.symeda.sormas.backend.therapy;

import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;

import de.symeda.sormas.backend.common.BaseAdoService;

@Stateless
@LocalBean
public class TherapyService extends BaseAdoService<Therapy> {

	public TherapyService() {
		super(Therapy.class);
	}

}

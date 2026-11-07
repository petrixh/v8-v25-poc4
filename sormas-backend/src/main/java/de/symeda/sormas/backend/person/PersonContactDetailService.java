package de.symeda.sormas.backend.person;

import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;

import de.symeda.sormas.backend.common.BaseAdoService;

@Stateless
@LocalBean
public class PersonContactDetailService extends BaseAdoService<PersonContactDetail> {

	public PersonContactDetailService() {
		super(PersonContactDetail.class);
	}
}

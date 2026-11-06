package de.symeda.sormas.backend.caze.maternalhistory;

import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;

import de.symeda.sormas.backend.common.BaseAdoService;

@Stateless
@LocalBean
public class MaternalHistoryService extends BaseAdoService<MaternalHistory> {

	public MaternalHistoryService() {
		super(MaternalHistory.class);
	}

}

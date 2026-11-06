package de.symeda.sormas.backend.deletionconfiguration;

import jakarta.ejb.Asynchronous;
import jakarta.ejb.EJB;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;

import de.symeda.sormas.api.deletionconfiguration.DeletionConfigurationFacade;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.backend.common.CronService;
import de.symeda.sormas.backend.util.RightsAllowed;

@Stateless(name = "DeletionConfigurationFacade")
public class DeletionConfigurationFacadeEjb implements DeletionConfigurationFacade {

	@EJB
	private CronService cronService;

	@Override
	@Asynchronous
	@RightsAllowed(UserRight._DEV_MODE)
	public void startAutomaticDeletion() {
		cronService.deleteExpiredEntities();
	}

	@LocalBean
	@Stateless
	public static class DeletionConfigurationFacadeEjbLocal extends DeletionConfigurationFacadeEjb {

	}
}

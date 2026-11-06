package de.symeda.sormas.api.deletionconfiguration;

import jakarta.ejb.Remote;

@Remote
public interface DeletionConfigurationFacade {

	void startAutomaticDeletion();

}

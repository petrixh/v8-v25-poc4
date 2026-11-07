package de.symeda.sormas.backend.importexport;

import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;

import de.symeda.sormas.backend.common.BaseAdoService;

@Stateless
@LocalBean
public class ExportConfigurationService extends BaseAdoService<ExportConfiguration> {

	public ExportConfigurationService() {
		super(ExportConfiguration.class);
	}

}

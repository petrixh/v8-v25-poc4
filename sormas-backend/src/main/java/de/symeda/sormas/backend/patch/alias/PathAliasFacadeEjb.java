package de.symeda.sormas.backend.patch.alias;

import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import de.symeda.sormas.api.survey.alias.PathAliasFacade;

@Stateless(name = "PathAliasFacade")
public class PathAliasFacadeEjb implements PathAliasFacade {

	@Inject
	private PathAliasHelper pathAliasHelper;

	@Override
	public String fetchAliasPath(String path) {
		return pathAliasHelper.toAliasPath(path);
	}

	@LocalBean
	@Stateless
	public static class PathAliasFacadeEjbLocal extends PathAliasFacadeEjb {

	}
}

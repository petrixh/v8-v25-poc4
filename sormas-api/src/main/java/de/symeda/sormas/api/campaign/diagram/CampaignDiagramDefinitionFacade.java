package de.symeda.sormas.api.campaign.diagram;

import java.util.List;

import jakarta.ejb.Remote;
import jakarta.validation.Valid;

@Remote
public interface CampaignDiagramDefinitionFacade {

	CampaignDiagramDefinitionDto save(@Valid CampaignDiagramDefinitionDto campaignDiagramDefinitionDto);

	List<CampaignDiagramDefinitionDto> getAll();

	List<CampaignDiagramDefinitionDto> getByUuids(List<String> uuids);

	boolean exists(String diagramId);

	CampaignDiagramDefinitionDto getByDiagramId(String diagramId);
}

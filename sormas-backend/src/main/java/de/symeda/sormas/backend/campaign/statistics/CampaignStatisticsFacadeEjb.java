package de.symeda.sormas.backend.campaign.statistics;

import java.util.List;

import jakarta.ejb.EJB;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;

import de.symeda.sormas.api.campaign.statistics.CampaignStatisticsCriteria;
import de.symeda.sormas.api.campaign.statistics.CampaignStatisticsDto;
import de.symeda.sormas.api.campaign.statistics.CampaignStatisticsFacade;

@Stateless(name = "CampaignStatisticsFacade")
public class CampaignStatisticsFacadeEjb implements CampaignStatisticsFacade {

	@EJB
	private CampaignStatisticsService campaignStatisticsService;

	@Override
	public List<CampaignStatisticsDto> getCampaignStatistics(CampaignStatisticsCriteria criteria) {

		return campaignStatisticsService.getCampaignStatistics(criteria);
	}

	@LocalBean
	@Stateless
	public static class CampaignStatisticsFacadeEjbLocal extends CampaignStatisticsFacadeEjb {
	}
}

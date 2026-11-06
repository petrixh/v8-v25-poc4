package de.symeda.sormas.backend.campaign;

import jakarta.persistence.criteria.From;

import de.symeda.sormas.backend.common.QueryJoins;

public class CampaignJoins extends QueryJoins<Campaign> {

	public CampaignJoins(From<?, Campaign> root) {
		super(root);
	}
}

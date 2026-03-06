package org.ilias.influapp.services;

import org.ilias.influapp.dtos.BusinessDashboardDto;
import org.ilias.influapp.dtos.InfluencerDashboardDto;

import java.time.LocalDate;

public interface DashboardService {

    InfluencerDashboardDto getInfluencerDashboard(Long influencerId, LocalDate from, LocalDate to);

    BusinessDashboardDto getBusinessDashboard(Long businessId, LocalDate from, LocalDate to);
}


package org.ilias.influapp.integration;

import org.ilias.influapp.entities.Business;
import org.ilias.influapp.entities.Campaign;
import org.ilias.influapp.entities.Collaboration;
import org.ilias.influapp.entities.Enums.CampaignStatus;
import org.ilias.influapp.entities.Enums.Category;
import org.ilias.influapp.entities.Enums.CollaborationStatus;
import org.ilias.influapp.entities.Enums.CompanySize;
import org.ilias.influapp.entities.Enums.InfluencerType;
import org.ilias.influapp.entities.Enums.UserRole;
import org.ilias.influapp.entities.Influencer;
import org.ilias.influapp.repository.BusinessRepository;
import org.ilias.influapp.repository.CampaignRepository;
import org.ilias.influapp.repository.CollaborationRepository;
import org.ilias.influapp.repository.InfluencerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@WebAppConfiguration
class CollaborationFlowIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private CollaborationRepository collaborationRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private InfluencerRepository influencerRepository;

    @Autowired
    private BusinessRepository businessRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        collaborationRepository.deleteAll();
        campaignRepository.deleteAll();
        influencerRepository.deleteAll();
        businessRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = "biz1@example.com", roles = "BUSINESS")
    void submitRequestWithExistingCampaignCreatesPendingCollaboration() throws Exception {
        Business business = createBusiness("biz1@example.com", "biz1");
        Influencer influencer = createInfluencer("inf1@example.com", "inf1");
        Campaign campaign = createCampaign(business, "Summer Promo", 1500.0);

        mockMvc.perform(post("/collaborations/request")
                        .param("influencerId", influencer.getId().toString())
                        .param("campaignId", campaign.getId().toString())
                        .param("paymentAmount", "300")
                        .param("deliverables", "1 reel + 2 stories"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/business/home?success=collaboration_requested"));

        assertEquals(1, collaborationRepository.count());
        Collaboration saved = collaborationRepository.findAll().get(0);
        assertEquals(CollaborationStatus.PENDING, saved.getStatus());
        assertEquals(300.0, saved.getPaymentAmount());
        assertEquals("1 reel + 2 stories", saved.getDeliverables());
        assertEquals(campaign.getId(), saved.getCampaign().getId());
        assertEquals(influencer.getId(), saved.getInfluencer().getId());
        
        // Verify budget calculations
        Campaign updatedCampaign = campaignRepository.findById(campaign.getId()).orElseThrow();
        assertEquals(1500.0, updatedCampaign.getBudget());
        assertEquals(1200.0, updatedCampaign.getRemainingBudget(), 0.01); // 1500 - 300
        
        // Verify collaboration dates
        assertEquals(LocalDate.now(), saved.getStartDate());
        assertNotNull(saved.getId());
    }

    @Test
    @WithMockUser(username = "biz1@example.com", roles = "BUSINESS")
    void submitRequestWithDuplicateActiveCollaborationRedirectsWithError() throws Exception {
        Business business = createBusiness("biz1@example.com", "biz1");
        Influencer influencer = createInfluencer("inf1@example.com", "inf1");
        Campaign campaign = createCampaign(business, "Summer Promo", 1500.0);

        Collaboration existing = new Collaboration();
        existing.setCampaign(campaign);
        existing.setInfluencer(influencer);
        existing.setStatus(CollaborationStatus.PENDING);
        existing.setPaymentAmount(250.0);
        existing.setStartDate(LocalDate.now());
        collaborationRepository.save(existing);

        mockMvc.perform(post("/collaborations/request")
                        .param("influencerId", influencer.getId().toString())
                        .param("campaignId", campaign.getId().toString())
                        .param("paymentAmount", "300"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/collaborations/request?influencerId=" + influencer.getId() + "&error=duplicate_collaboration"));

        assertEquals(1, collaborationRepository.count());
    }

    @Test
    @WithMockUser(username = "biz1@example.com", roles = "BUSINESS")
    void submitRequestWithInvalidCampaignRedirectsWithError() throws Exception {
        createBusiness("biz1@example.com", "biz1");
        Influencer influencer = createInfluencer("inf1@example.com", "inf1");

        mockMvc.perform(post("/collaborations/request")
                        .param("influencerId", influencer.getId().toString())
                        .param("campaignId", "9999")
                        .param("paymentAmount", "300"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/collaborations/request?influencerId=" + influencer.getId() + "&error=invalid_campaign"));

        assertEquals(0, collaborationRepository.count());
    }

    @Test
    @WithMockUser(username = "biz1@example.com", roles = "BUSINESS")
    void submitRequestWithoutCampaignCreatesAdHocCampaign() throws Exception {
        Business business = createBusiness("biz1@example.com", "biz1");
        Influencer influencer = createInfluencer("inf1@example.com", "inf1");

        mockMvc.perform(post("/collaborations/request")
                        .param("influencerId", influencer.getId().toString())
                        .param("paymentAmount", "420")
                        .param("deliverables", "1 short video"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/business/home?success=collaboration_requested"));

        assertEquals(1, campaignRepository.findByBusinessId(business.getId()).size());
        assertEquals(1, collaborationRepository.count());

        Collaboration saved = collaborationRepository.findAll().get(0);
        assertNotNull(saved.getCampaign());
        Campaign savedCampaign = campaignRepository.findById(saved.getCampaign().getId()).orElseThrow();
        assertEquals("Ad-hoc request from " + business.getCompanyName(), savedCampaign.getTitle());
        assertEquals(CollaborationStatus.PENDING, saved.getStatus());
    }

    @Test
    @WithMockUser(username = "inf1@example.com", roles = "INFLUENCER")
    void acceptCollaborationByOwnerChangesStatusToAccepted() throws Exception {
        Business business = createBusiness("biz1@example.com", "biz1");
        Influencer influencer = createInfluencer("inf1@example.com", "inf1");
        Campaign campaign = createCampaign(business, "Launch", 800.0);

        Collaboration collaboration = new Collaboration();
        collaboration.setCampaign(campaign);
        collaboration.setInfluencer(influencer);
        collaboration.setStatus(CollaborationStatus.PENDING);
        collaboration.setPaymentAmount(200.0);
        collaboration.setStartDate(LocalDate.now());
        collaboration = collaborationRepository.save(collaboration);

        mockMvc.perform(post("/collaborations/{id}/accept", collaboration.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/influencer/home?success=collab_accepted"));

        Collaboration updated = collaborationRepository.findById(collaboration.getId()).orElseThrow();
        assertEquals(CollaborationStatus.ACCEPTED, updated.getStatus());
    }

    @Test
    @WithMockUser(username = "other@example.com", roles = "INFLUENCER")
    void acceptCollaborationByDifferentInfluencerKeepsPending() throws Exception {
        Business business = createBusiness("biz1@example.com", "biz1");
        Influencer owner = createInfluencer("inf1@example.com", "inf1");
        createInfluencer("other@example.com", "other");
        Campaign campaign = createCampaign(business, "Launch", 800.0);

        Collaboration collaboration = new Collaboration();
        collaboration.setCampaign(campaign);
        collaboration.setInfluencer(owner);
        collaboration.setStatus(CollaborationStatus.PENDING);
        collaboration.setPaymentAmount(200.0);
        collaboration.setStartDate(LocalDate.now());
        collaboration = collaborationRepository.save(collaboration);

        mockMvc.perform(post("/collaborations/{id}/accept", collaboration.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/influencer/home?error=forbidden"));

        Collaboration unchanged = collaborationRepository.findById(collaboration.getId()).orElseThrow();
        assertEquals(CollaborationStatus.PENDING, unchanged.getStatus());
    }

    private Business createBusiness(String email, String username) {
        Business business = new Business();
        business.setEmail(email);
        business.setUsername(username);
        business.setPassword("encoded-pass");
        business.setRole(UserRole.BUSINESS);
        business.setCompanyName("ACME " + username);
        business.setCategory(Category.OTHER);
        business.setCompanySize(CompanySize.MEDIUM);
        business.setDescription("A test business for collaboration testing");
        business.setEstablishedYear("2020");
        business.setPhone("+30-123456789");
        business.setContactEmail(email);
        business.setAddress("123 Business St, Athens, Greece");
        return businessRepository.save(business);
    }

    private Influencer createInfluencer(String email, String username) {
        Influencer influencer = new Influencer();
        influencer.setEmail(email);
        influencer.setUsername(username);
        influencer.setPassword("encoded-pass");
        influencer.setRole(UserRole.INFLUENCER);
        influencer.setName(username);
        influencer.setAge("25");
        influencer.setLocation("Athens, Greece");
        influencer.setBio("A test influencer for collaboration testing");
        influencer.setCategory(Category.OTHER);
        influencer.setInfluencerType(InfluencerType.MICRO);
        influencer.setIsAvailable(true);
        influencer.setMinCollaborationBudget(100);
        influencer.setTotalFollowers(50000);
        influencer.setEngagementRate(BigDecimal.valueOf(4.5));
        influencer.setInfluencerScore(75.0);
        return influencerRepository.save(influencer);
    }

    private Campaign createCampaign(Business business, String title, Double budget) {
        Campaign campaign = Campaign.builder()
                .business(business)
                .title(title)
                .description("Campaign for testing")
                .status(CampaignStatus.DRAFT)
                .targetCategory(Category.OTHER)
                .budget(budget)
                .startDate(LocalDate.now())
                .build();
        return campaignRepository.save(campaign);
    }
}


package org.ilias.influapp.config;

import lombok.RequiredArgsConstructor;
import org.ilias.influapp.entities.*;
import org.ilias.influapp.entities.Enums.*;
import org.ilias.influapp.repository.*;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.core.io.ClassPathResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ilias.influapp.services.HybridSentimentService;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

@Component
@Order(2)
@RequiredArgsConstructor
@SuppressWarnings("null")
public class TestDataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(TestDataLoader.class);

    private final BusinessRepository businessRepository;
    private final InfluencerRepository influencerRepository;
    private final CampaignRepository campaignRepository;
    private final CollaborationRepository collaborationRepository;
    private final PostRepository postRepository;
    private final SentimentAnalysisRepository sentimentAnalysisRepository;
    private final SocialMediaRepository socialMediaRepository;
    private final ReactionRepository reactionRepository;
    private final PasswordEncoder passwordEncoder;
    private final HybridSentimentService hybridSentimentService;
    private final TransactionTemplate transactionTemplate;


    private void updateInfluencerMetricsFromSocialMedia() {
        log.info("Recalculating and persisting influencer metrics for all influencers...");
        List<Influencer> all = influencerRepository.findAll();
        for (Influencer inf : all) {
            try {
                // Reload to ensure associations are fetched lazily if necessary
                Influencer reloaded = influencerRepository.findById(inf.getId()).orElse(inf);

                // Ensure each SocialMedia has latest averages
                if (reloaded.getSocialMediaAccounts() != null) {
                    for (SocialMedia sm : reloaded.getSocialMediaAccounts()) {
                        try {
                            SocialMedia refreshed = socialMediaRepository.findById(sm.getId()).orElse(sm);
                            // If SocialMedia.updateAverages would change values, call it
                            refreshed.updateAverages();
                            socialMediaRepository.saveAndFlush(refreshed);
                        } catch (Throwable ignore) {}
                    }
                }

                // Recalculate influencer derived metrics and save
                reloaded.updateTotalFollowers();
                reloaded.updateEngagementRate();
                reloaded.updateInfluencerScore();
                influencerRepository.saveAndFlush(reloaded);
                log.info("  Persisted influencer {}: followers={}, engagement={}, score={}", reloaded.getUsername(), reloaded.getTotalFollowers(), reloaded.getEngagementRate(), reloaded.getInfluencerScore());
            } catch (Throwable ex) {
                log.warn("  Failed to persist influencer metrics for {}: {}", inf.getUsername(), ex.getMessage());
            }
        }
    }

    @Override
    public void run(String @NonNull ... args) {
        log.info("TestDataLoader starting - ensuring comprehensive test data exists...");

        // If JSON test-data files are present in classpath under /test-data, load them
        try {
            ClassPathResource check = new ClassPathResource("test-data/influencers.json");
            if (check.exists()) {
                log.info("JSON test-data detected in classpath 'test-data' - loading synthetic datasets from JSON files");
                try {
                    loadTestDataFromJson();
                } catch (Throwable ex) {
                    log.warn("Failed to load test-data from JSON: {}", ex.getMessage());
                }
                // After loading from JSON, return - skip the hardcoded seeding below
                return;
            }
        } catch (Throwable ignored) {}

        // Force recreate: delete existing reactions, sentiment analyses, posts and collaborations
        try {
            log.info("TestDataLoader force-recreate: deleting reactions, sentiment analyses, posts and collaborations...");
            transactionTemplate.execute(status -> {
                reactionRepository.deleteAll(reactionRepository.findAll());
                sentimentAnalysisRepository.deleteAll(sentimentAnalysisRepository.findAll());
                postRepository.deleteAll(postRepository.findAll());
                collaborationRepository.deleteAll(collaborationRepository.findAll());
                return null;
            });
            log.info("TestDataLoader force-recreate: deletions complete.");
        } catch (Throwable ex) {
            log.warn("TestDataLoader force-recreate deletion failed: {}", ex.getMessage());
        }
    }


    // ==================== JSON-BASED DATA LOADING ====================
    // 📂 Loads synthetic test data from JSON files in src/main/resources/test-data/
    // This keeps the system isolated from external data sources
    private void loadTestDataFromJson() {
        log.info("🚀 Starting JSON-based test data loading...");
        log.info("📋 Available repositories: businessRepository={}, influencerRepository={}, campaignRepository={}, collaborationRepository={}",
            businessRepository != null, influencerRepository != null, campaignRepository != null, collaborationRepository != null);

        // Cleanup in a separate transaction
        try {
            transactionTemplate.execute(status -> {
                log.info("🧹 Cleaning up existing test data...");
                reactionRepository.deleteAll(reactionRepository.findAll());
                sentimentAnalysisRepository.deleteAll(sentimentAnalysisRepository.findAll());
                postRepository.deleteAll(postRepository.findAll());
                collaborationRepository.deleteAll(collaborationRepository.findAll());
                campaignRepository.deleteAll(campaignRepository.findAll());
                socialMediaRepository.deleteAll(socialMediaRepository.findAll());
                influencerRepository.deleteAll(influencerRepository.findAll());
                businessRepository.deleteAll(businessRepository.findAll());
                log.info("✅ Cleanup complete");
                return null;
            });
        } catch (Throwable ex) {
            log.error("❌ Failed to cleanup: {}", ex.getMessage(), ex);
        }

        ObjectMapper mapper = new ObjectMapper();

        // 🏢 Load Businesses
        try {
            transactionTemplate.execute(status -> {
                // Load businesses
                log.info("📊 Loading businesses from JSON...");
                try {
                    ClassPathResource businessResource = new ClassPathResource("test-data/businesses.json");
                    if (businessResource.exists()) {
                        List<Map<String, Object>> businessData = mapper.readValue(businessResource.getInputStream(), new TypeReference<List<Map<String, Object>>>() {});
                        log.info("📝 Parsed {} business records from JSON", businessData.size());
                        for (Map<String, Object> bData : businessData) {
                            log.debug("Loading business: {}", bData.get("username"));
                            loadBusinessFromJson(bData);
                        }
                        log.info("✅ Loaded {} businesses", businessData.size());
                    } else {
                        log.warn("⚠️ businesses.json resource not found!");
                    }
                } catch (Throwable ex) {
                    log.error("❌ Failed to load businesses: {}", ex.getMessage(), ex);
                }
                return null;
            });
        } catch (Throwable ex) {
            log.error("❌ Transaction failed for businesses: {}", ex.getMessage(), ex);
        }

        // 👤 Load Influencers
        try {
            transactionTemplate.execute(status -> {
                // Load influencers
                log.info("👥 Loading influencers from JSON...");
                try {
                    ClassPathResource influencerResource = new ClassPathResource("test-data/influencers.json");
                    if (influencerResource.exists()) {
                        List<Map<String, Object>> infData = mapper.readValue(influencerResource.getInputStream(), new TypeReference<List<Map<String, Object>>>() {});
                        log.info("📝 Parsed {} influencer records from JSON", infData.size());
                        for (Map<String, Object> iData : infData) {
                            log.debug("Loading influencer: {}", iData.get("username"));
                            loadInfluencerFromJson(iData);
                        }
                        log.info("✅ Loaded {} influencers", infData.size());
                    } else {
                        log.warn("⚠️ influencers.json resource not found!");
                    }
                } catch (Throwable ex) {
                    log.error("❌ Failed to load influencers: {}", ex.getMessage(), ex);
                }
                return null;
            });
        } catch (Throwable ex) {
            log.error("❌ Transaction failed for influencers: {}", ex.getMessage(), ex);
        }

        // 📢 Load Campaigns
        try {
            transactionTemplate.execute(status -> {
                // Load campaigns
                log.info("📢 Loading campaigns from JSON...");
                try {
                    ClassPathResource campaignResource = new ClassPathResource("test-data/campaigns.json");
                    if (campaignResource.exists()) {
                        List<Map<String, Object>> campaignData = mapper.readValue(campaignResource.getInputStream(), new TypeReference<List<Map<String, Object>>>() {});
                        log.info("📝 Parsed {} campaign records from JSON", campaignData.size());
                        for (Map<String, Object> cData : campaignData) {
                            log.debug("Loading campaign: {}", cData.get("title"));
                            loadCampaignFromJson(cData);
                        }
                        log.info("✅ Loaded {} campaigns", campaignData.size());
                    } else {
                        log.warn("⚠️ campaigns.json resource not found!");
                    }
                } catch (Throwable ex) {
                    log.error("❌ Failed to load campaigns: {}", ex.getMessage(), ex);
                }
                return null;
            });
        } catch (Throwable ex) {
            log.error("❌ Transaction failed for campaigns: {}", ex.getMessage(), ex);
        }

        // 🤝 Load Collaborations & Posts
        try {
            transactionTemplate.execute(status -> {
                // Load collaborations and posts
                log.info("🤝 Loading collaborations and posts from JSON...");
                try {
                    ClassPathResource collabResource = new ClassPathResource("test-data/collaborations.json");
                    if (collabResource.exists()) {
                        List<Map<String, Object>> collabData = mapper.readValue(collabResource.getInputStream(), new TypeReference<List<Map<String, Object>>>() {});
                        log.info("📝 Parsed {} collaboration records from JSON", collabData.size());
                        for (Map<String, Object> coData : collabData) {
                            log.debug("Loading collaboration: {} x {}", coData.get("campaignTitle"), coData.get("influencerUsername"));
                            loadCollaborationFromJson(coData);
                        }
                        log.info("✅ Loaded {} collaborations with posts", collabData.size());
                    } else {
                        log.warn("⚠️ collaborations.json resource not found!");
                    }
                } catch (Throwable ex) {
                    log.error("❌ Failed to load collaborations: {}", ex.getMessage(), ex);
                }
                return null;
            });
        } catch (Throwable ex) {
            log.error("❌ Transaction failed for collaborations: {}", ex.getMessage(), ex);
        }

        try {
            transactionTemplate.execute(status -> {
                updateInfluencerMetricsFromSocialMedia();
                return null;
            });
        } catch (Throwable ex) {
            log.warn("⚠️ Failed to update influencer metrics: {}", ex.getMessage());
        }

        log.info("✨ TestDataLoader finished - JSON test data seeding complete");

        // 📊 Summary counts
        try {
            long bizCount = businessRepository.count();
            long infCount = influencerRepository.count();
            long campCount = campaignRepository.count();
            long collCount = collaborationRepository.count();
            long postCount = postRepository.count();
            long reactionCount = reactionRepository.count();
            long sentimentCount = sentimentAnalysisRepository.count();
            long smCount = socialMediaRepository.count();
            log.info("📈 TestDataLoader SUMMARY: businesses=✅{}, influencers=👥{}, campaigns=📢{}, collaborations=🤝{}, posts=📝{}, reactions=❤️{}, sentiments=🧠{}, socialMedia=📱{}",
                bizCount, infCount, campCount, collCount, postCount, reactionCount, sentimentCount, smCount);
        } catch (Throwable ignored) {}
    }

    private void loadBusinessFromJson(Map<String, Object> data) {
        try {
            String email = (String) data.get("email");
            String username = (String) data.get("username");
            String companyName = (String) data.get("companyName");
            String password = (String) data.getOrDefault("password", "pass");

            Business b = Business.builder()
                    .email(email)
                    .username(username)
                    .password(ensureEncoded(password))
                    .role(UserRole.BUSINESS)
                    .companyName(companyName)
                    .build();

            if (data.containsKey("category")) {
                b.setCategory(Category.valueOf((String) data.get("category")));
            }
            if (data.containsKey("companySize")) {
                b.setCompanySize(org.ilias.influapp.entities.Enums.CompanySize.valueOf((String) data.get("companySize")));
            }
            if (data.containsKey("establishedYear")) {
                b.setEstablishedYear((String) data.get("establishedYear"));
            }
            if (data.containsKey("description")) {
                b.setDescription((String) data.get("description"));
            }
            if (data.containsKey("phone")) {
                b.setPhone((String) data.get("phone"));
            }
            if (data.containsKey("contactEmail")) {
                b.setContactEmail((String) data.get("contactEmail"));
            }
            if (data.containsKey("address")) {
                b.setAddress((String) data.get("address"));
            }
            if (data.containsKey("website")) {
                b.setWebSite((String) data.get("website"));
            }

            // Optional social / media fields
            if (data.containsKey("imageUrl")) {
                b.setImageUrl((String) data.get("imageUrl"));
            }
            if (data.containsKey("linkedinUrl")) {
                b.setLinkedinUrl((String) data.get("linkedinUrl"));
            }
            if (data.containsKey("facebookUrl")) {
                b.setFacebookUrl((String) data.get("facebookUrl"));
            }
            if (data.containsKey("instagramUrl")) {
                b.setInstagramUrl((String) data.get("instagramUrl"));
            }
            if (data.containsKey("twitterUrl")) {
                b.setTwitterUrl((String) data.get("twitterUrl"));
            }

            // Recommendation / target fields
            try {
                if (data.containsKey("targetCategory") && data.get("targetCategory") != null) {
                    b.setTargetCategory(Category.valueOf((String) data.get("targetCategory")));
                }
            } catch (IllegalArgumentException ex) {
                log.warn("Invalid targetCategory '{}' for business {}", data.get("targetCategory"), username);
            }

            try {
                if (data.containsKey("targetAgeGroup") && data.get("targetAgeGroup") != null) {
                    b.setTargetAgeGroup(AgeGroup.valueOf((String) data.get("targetAgeGroup")));
                }
            } catch (IllegalArgumentException ex) {
                log.warn("Invalid targetAgeGroup '{}' for business {}", data.get("targetAgeGroup"), username);
            }

            try {
                if (data.containsKey("targetGenderGroup") && data.get("targetGenderGroup") != null) {
                    b.setTargetGenderGroup(GenderGroup.valueOf((String) data.get("targetGenderGroup")));
                }
            } catch (IllegalArgumentException ex) {
                log.warn("Invalid targetGenderGroup '{}' for business {}", data.get("targetGenderGroup"), username);
            }

            try {
                if (data.containsKey("preferredInfluencerType") && data.get("preferredInfluencerType") != null) {
                    b.setPreferredInfluencerType(InfluencerType.valueOf((String) data.get("preferredInfluencerType")));
                }
            } catch (IllegalArgumentException ex) {
                log.warn("Invalid preferredInfluencerType '{}' for business {}", data.get("preferredInfluencerType"), username);
            }

            if (data.containsKey("maxBudgetPerCollaboration") && data.get("maxBudgetPerCollaboration") instanceof Number) {
                b.setMaxBudgetPerCollaboration(((Number) data.get("maxBudgetPerCollaboration")).intValue());
            }

            // Note: JSON may contain additional fields (annualMarketingBudget, preferences) which
            // are not modelled on the Business entity and therefore are ignored here.

            businessRepository.save(b);
            log.info("Loaded business: {} ({})", companyName, email);
            log.debug("  Mapped fields -> targetCategory={}, targetAgeGroup={}, targetGenderGroup={}, preferredInfluencerType={}, maxBudgetPerCollaboration={}",
                    b.getTargetCategory(), b.getTargetAgeGroup(), b.getTargetGenderGroup(), b.getPreferredInfluencerType(), b.getMaxBudgetPerCollaboration());
        } catch (Throwable ex) {
            log.warn("Failed to load business: {}", ex.getMessage());
        }
    }

    private void loadInfluencerFromJson(Map<String, Object> data) {
        try {
            String email = (String) data.get("email");
            String username = (String) data.get("username");
            String name = (String) data.get("name");
            String password = (String) data.getOrDefault("password", "pass");

            // Handle available field - could be boolean or null
            Object availableObj = data.getOrDefault("available", true);
            boolean available = availableObj instanceof Boolean ? (Boolean) availableObj : true;

            Influencer inf = Influencer.builder()
                    .email(email)
                    .username(username)
                    .password(ensureEncoded(password))
                    .role(UserRole.INFLUENCER)
                    .name(name)
                    .isAvailable(available)
                    .build();

            if (data.containsKey("age")) {
                inf.setAge(String.valueOf(data.get("age")));
            }
            if (data.containsKey("location")) {
                inf.setLocation((String) data.get("location"));
            }
            if (data.containsKey("bio")) {
                inf.setBio((String) data.get("bio"));
            }
            if (data.containsKey("category")) {
                inf.setCategory(Category.valueOf((String) data.get("category")));
            }
            if (data.containsKey("influencerType")) {
                inf.setInfluencerType(org.ilias.influapp.entities.Enums.InfluencerType.valueOf((String) data.get("influencerType")));
            }
            if (data.containsKey("totalFollowers")) {
                inf.setTotalFollowers(((Number) data.get("totalFollowers")).intValue());
            }
            if (data.containsKey("minCollaborationBudget")) {
                inf.setMinCollaborationBudget(((Number) data.get("minCollaborationBudget")).intValue());
            }
            if (data.containsKey("ageGroup")) {
                try {
                    inf.setAgeGroup(AgeGroup.valueOf((String) data.get("ageGroup")));
                } catch (IllegalArgumentException ex) {
                    log.warn("Invalid AgeGroup value '{}': {}", data.get("ageGroup"), ex.getMessage());
                }
            }
            if (data.containsKey("gender")) {
                try {
                    inf.setGender(GenderGroup.valueOf((String) data.get("gender")));
                } catch (IllegalArgumentException ex) {
                    log.warn("Invalid GenderGroup value '{}': {}", data.get("gender"), ex.getMessage());
                }
            }
            if (data.containsKey("genderTarget")) {
                try {
                    inf.setGenderTarget(GenderGroup.valueOf((String) data.get("genderTarget")));
                } catch (IllegalArgumentException ex) {
                    log.warn("Invalid GenderGroup target value '{}': {}", data.get("genderTarget"), ex.getMessage());
                }
            }

            inf = influencerRepository.save(inf);

            // Load social media accounts
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> socialMediaList = (List<Map<String, Object>>) data.get("socialMediaAccounts");
            if (socialMediaList != null) {
                for (Map<String, Object> smData : socialMediaList) {
                    loadSocialMediaFromJson(inf, smData);
                }
            }

            log.info("Loaded influencer: {} ({})", name, email);
        } catch (Throwable ex) {
            log.error("Failed to load influencer: {}", ex.getMessage(), ex);
            ex.printStackTrace();
        }
    }

    private void loadSocialMediaFromJson(Influencer inf, Map<String, Object> data) {
        try {
            String platform = (String) data.get("platform");
            String accountUsername = (String) data.get("username");
            String url = (String) data.get("accountUrl");
            Integer followers = ((Number) data.getOrDefault("followers", 10000)).intValue();

            SocialMedia sm = new SocialMedia();
            sm.setPlatform(Platform.valueOf(platform));
            sm.setUsername(accountUsername);
            sm.setAccountUrl(url);
            sm.setFollowers(followers);
            sm.setInfluencer(inf);

            socialMediaRepository.save(sm);
            inf.addSocialMediaAccount(sm);
            influencerRepository.save(inf);
            log.info("  Loaded social media: {} ({})", platform, accountUsername);
        } catch (Throwable ex) {
            log.error("Failed to load social media: {}", ex.getMessage(), ex);
        }
    }

    private void loadCampaignFromJson(Map<String, Object> data) {
        try {
            String businessUsername = (String) data.get("businessUsername");
            String title = (String) data.get("title");
            String description = (String) data.get("description");
            String status = (String) data.getOrDefault("status", "DRAFT");
            String targetCategory = (String) data.getOrDefault("targetCategory", "OTHER");
            Double budget = ((Number) data.getOrDefault("budget", 1000.0)).doubleValue();
            String goals = (String) data.get("goals");

            Business business = businessRepository.findAll().stream()
                    .filter(b -> businessUsername.equals(b.getUsername()))
                    .findFirst()
                    .orElse(null);

            if (business == null) {
                log.warn("Business '{}' not found for campaign '{}'", businessUsername, title);
                return;
            }

            Campaign camp = Campaign.builder()
                    .business(business)
                    .title(title)
                    .description(description)
                    .status(CampaignStatus.valueOf(status))
                    .targetCategory(Category.valueOf(targetCategory))
                    .budget(budget)
                    .startDate(LocalDate.now())
                    .goals(goals)
                    .build();

            camp = campaignRepository.save(camp);
            business.addCampaign(camp);
            businessRepository.save(business);
            log.info("Loaded campaign: {} ({})", title, businessUsername);
        } catch (Throwable ex) {
            log.error("Failed to load campaign: {}", ex.getMessage(), ex);
        }
    }

    private void loadCollaborationFromJson(Map<String, Object> data) {
        try {
            String campaignTitle = (String) data.get("campaignTitle");
            String influencerUsername = (String) data.get("influencerUsername");
            String status = (String) data.getOrDefault("status", "ACCEPTED");
            Double payment = ((Number) data.getOrDefault("paymentAmount", 100.0)).doubleValue();
            String deliverables = (String) data.get("deliverables");

            // Use derived queries instead of loading all entities
            Campaign campaign = null;
            try {
                campaign = campaignRepository.findByTitle(campaignTitle);
            } catch (Exception e) {
                log.debug("Campaign query failed: {}", e.getMessage());
                // Fallback to stream search if derived query doesn't exist
                campaign = campaignRepository.findAll().stream()
                        .filter(c -> campaignTitle.equals(c.getTitle()))
                        .findFirst()
                        .orElse(null);
            }

            Influencer influencer = null;
            try {
                influencer = influencerRepository.findByUsername(influencerUsername);
            } catch (Exception e) {
                log.debug("Influencer query failed: {}", e.getMessage());
                // Fallback to stream search if derived query doesn't exist
                influencer = influencerRepository.findAll().stream()
                        .filter(i -> influencerUsername.equals(i.getUsername()))
                        .findFirst()
                        .orElse(null);
            }

            if (campaign == null || influencer == null) {
                log.warn("Campaign '{}' or Influencer '{}' not found", campaignTitle, influencerUsername);
                return;
            }

            Collaboration coll = new Collaboration();
            coll.setCampaign(campaign);
            coll.setInfluencer(influencer);
            coll.setStatus(CollaborationStatus.valueOf(status));
            coll.setPaymentAmount(payment);
            coll.setStartDate(LocalDate.now());
            coll.setDeliverables(deliverables);
            coll = collaborationRepository.save(coll);
            campaign.addCollaboration(coll);
            campaignRepository.save(campaign);
            influencer.addCollaboration(coll);
            influencerRepository.save(influencer);

            // Load posts
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> postList = (List<Map<String, Object>>) data.get("posts");
            if (postList != null) {
                for (Map<String, Object> postData : postList) {
                    loadPostFromJson(coll, postData);
                }
            }

            log.info("Loaded collaboration: {} x {} with {} posts", campaignTitle, influencerUsername, postList != null ? postList.size() : 0);
        } catch (Throwable ex) {
            log.error("Failed to load collaboration: {}", ex.getMessage(), ex);
        }
    }

    private void loadPostFromJson(Collaboration coll, Map<String, Object> data) {
        try {
            String platform = (String) data.get("platform");
            String content = (String) data.get("content");
            Integer reach = ((Number) data.getOrDefault("reach", 1000)).intValue();
            Integer impressions = ((Number) data.getOrDefault("impressions", 1000)).intValue();
            Integer shares = ((Number) data.getOrDefault("shares", 0)).intValue();
            String manualLabel = (String) data.get("manualLabel");

            // Find social media by influencer and platform
            Influencer influencer = coll.getInfluencer();
            SocialMedia sm = influencer.getSocialMediaAccounts().stream()
                    .filter(s -> platform.equals(s.getPlatform().toString()))
                    .findFirst()
                    .orElse(null);

            if (sm == null) {
                log.warn("Social media {} not found for influencer {}", platform, influencer.getUsername());
                return;
            }

            Post p = Post.builder()
                    .content(content)
                    .socialMedia(sm)
                    .reach(reach)
                    .impressionCount(impressions)
                    .shares(shares)
                    .build();

            @SuppressWarnings("unchecked")
            List<String> comments = (List<String>) data.getOrDefault("comments", new ArrayList<String>());
            p.setComments(comments);
            p.setCollaboration(coll);
            p = postRepository.save(p);

            // Load reactions
            @SuppressWarnings("unchecked")
            Map<String, Integer> reactionCounts = (Map<String, Integer>) data.get("reactions");
            if (reactionCounts != null && !reactionCounts.isEmpty()) {
                List<Reaction> reactions = new ArrayList<>();
                for (var entry : reactionCounts.entrySet()) {
                    Reaction r = new Reaction();
                    r.setPost(p);
                    r.setType(ReactionType.valueOf(entry.getKey()));
                    r.setCount(entry.getValue());
                    reactions.add(r);
                }
                reactionRepository.saveAll(reactions);
                p.setReactions(reactions);
                p = postRepository.saveAndFlush(p);
            }

            // Calculate engagement rate
            try {
                p.calculateAndSetEngagementRate();
                postRepository.save(p);
            } catch (Throwable ex) {
                log.warn("Failed to calculate engagement rate: {}", ex.getMessage());
            }

            // Analyze sentiment
            try {
                var reactions = p.getReactions();
                var reactionsList = reactions != null ? new ArrayList<Reaction>(reactions) : new ArrayList<Reaction>();
                var sentiment = hybridSentimentService.analyzeAndSave(p, reactionsList, comments);
                p.setPostSentiment(sentiment);
                postRepository.save(p);
            } catch (Throwable ex) {
                log.warn("Failed to analyze sentiment: {}", ex.getMessage());
            }

            // Set manual label if provided
            if (manualLabel != null) {
                try {
                    var saOpt = sentimentAnalysisRepository.findByPostId(p.getId());
                    if (saOpt.isPresent()) {
                        SentimentAnalysis sa = saOpt.get();
                        sa.setManualLabel(manualLabel);
                        sentimentAnalysisRepository.save(sa);
                    }
                } catch (Throwable ex) {
                    log.warn("Failed to set manual label: {}", ex.getMessage());
                }
            }

            // Link post to social media and collaboration
            try {
                if (!sm.getPosts().contains(p)) {
                    sm.addPost(p);
                    socialMediaRepository.saveAndFlush(sm);
                }
            } catch (Throwable ex) {
                log.warn("Failed to link post to social media: {}", ex.getMessage());
            }

            try {
                if (!coll.getPosts().contains(p)) {
                    coll.addPost(p);
                    collaborationRepository.save(coll);
                }
            } catch (Throwable ex) {
                log.warn("Failed to link post to collaboration: {}", ex.getMessage());
            }

            log.info("  Loaded post: {} (reach={}, impressions={})", content.substring(0, Math.min(50, content.length())), reach, impressions);
        } catch (Throwable ex) {
            log.error("Failed to load post: {}", ex.getMessage(), ex);
        }
    }

    private String ensureEncoded(String rawOrEncoded) {
         if (rawOrEncoded == null) return null;
         String lower = rawOrEncoded.toLowerCase();
         if (rawOrEncoded.startsWith("$2a$") || rawOrEncoded.startsWith("$2b$") || rawOrEncoded.startsWith("$2y$")) {
             return rawOrEncoded;
         }
         // Also if it looks already encoded (very unlikely) skip
         return passwordEncoder.encode(rawOrEncoded);
     }

    // Helper methods for seeding
    private void updateAverageLikesInDatabase() {
         log.info("Updating average_likes using entity-based approach...");
         updateAverageLikesViaEntities();
     }


    private void updateAverageLikesViaEntities() {
        try {
            log.info("=== STARTING AVERAGE_LIKES UPDATE ===");
            
            // Get all SocialMedia IDs first
            List<SocialMedia> allSm = socialMediaRepository.findAll();
            log.info("Found {} social media accounts", allSm.size());
            
            for (SocialMedia sm : allSm) {
                try {
                    // Reload to ensure fresh data
                    sm = socialMediaRepository.findById(sm.getId()).orElse(sm);
                    log.info("Processing: {} ({}) - ID: {}", sm.getUsername(), sm.getPlatform(), sm.getId());
                    
                    int totalReactions = 0;
                    List<Post> posts = postRepository.findBySocialMediaId(sm.getId());
                    int postCount = posts.size();

                    // Always query posts by social media id to avoid stale in-memory collections
                    if (!posts.isEmpty()) {
                        log.info("  Found {} posts", postCount);
                        
                        // For each post, fetch reactions directly from database
                        for (Post p : posts) {
                            if (p != null) {
                                log.info("    Post ID: {}, Reactions count: {}", p.getId(), 
                                    p.getReactions() != null ? p.getReactions().size() : "NULL");
                                
                                // Reload post to ensure reactions are loaded
                                Post reloadedPost = postRepository.findById(p.getId()).orElse(p);
                                
                                // Get reactions - access directly from database if needed
                                if (reloadedPost.getReactions() != null && !reloadedPost.getReactions().isEmpty()) {
                                    for (Reaction r : reloadedPost.getReactions()) {
                                        if (r != null && r.getCount() != null) {
                                            log.info("      Reaction type: {}, count: {}", r.getType(), r.getCount());
                                            totalReactions += r.getCount();
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        log.info("  No posts found");
                    }
                    
                    int avgLikes = postCount > 0 ? totalReactions / postCount : 0;
                    log.info("  CALCULATION: totalReactions={}, postCount={}, avgLikes={}", 
                        totalReactions, postCount, avgLikes);
                    
                    // Set and save
                    sm.setAverageLikes(avgLikes);
                    log.info("  SET: averageLikes = {}", sm.getAverageLikes());
                    
                    SocialMedia saved = socialMediaRepository.saveAndFlush(sm);
                    log.info("  SAVED: averageLikes = {} (from DB)", saved.getAverageLikes());
                    
                    if (avgLikes > 0) {
                        log.info("✓ Updated average_likes for {} ({}): {}", 
                            sm.getUsername(), sm.getPlatform(), avgLikes);
                    }
                } catch (Throwable ex) {
                    log.error("✗ ERROR processing {} ({}): {}", sm.getUsername(), sm.getPlatform(), ex.getMessage());
                    ex.printStackTrace();
                }
            }
            log.info("=== FINISHED AVERAGE_LIKES UPDATE ===");
        } catch (Throwable ex) {
            log.warn("✗ Entity-based update failed: {}", ex.getMessage());
            ex.printStackTrace();
        }
    }


    private void updateAverageCommentsInDatabase() {
        try {
            log.info("Updating average_comments in social_media");
            List<SocialMedia> allSm = socialMediaRepository.findAll();
            for (SocialMedia sm : allSm) {
                int totalComments = 0;
                List<Post> posts = postRepository.findBySocialMediaId(sm.getId());
                int postCount = posts.size();

                if (!posts.isEmpty()) {
                    for (Post p : posts) {
                        if (p != null && p.getComments() != null) {
                            totalComments += p.getComments().size();
                        }
                    }
                }
                
                sm.setAverageComments(postCount > 0 ? totalComments / postCount : 0);
                socialMediaRepository.save(sm);
            }
        } catch (Throwable ex) {
            log.warn("Failed to update average_comments: {}", ex.getMessage());
        }
    }


    private void updateEngagementRateInDatabase() {
        try {
            log.info("Updating engagement_rate in social_media");
            List<SocialMedia> allSm = socialMediaRepository.findAll();
            for (SocialMedia sm : allSm) {
                double totalEngagement = 0.0;
                List<Post> posts = postRepository.findBySocialMediaId(sm.getId());
                int postCount = posts.size();

                if (!posts.isEmpty()) {
                    for (Post p : posts) {
                        if (p != null && p.getEngagementRate() != null) {
                            totalEngagement += p.getEngagementRate();
                        }
                    }
                }
                
                double avgEngagement = postCount > 0 ? totalEngagement / postCount : 0.0;
                sm.setEngagementRate(avgEngagement);
                socialMediaRepository.save(sm);
                
                if (avgEngagement > 0) {
                    log.info("Updated engagement_rate for {} ({}): {}", 
                        sm.getUsername(), sm.getPlatform(), String.format("%.2f%%", avgEngagement));
                }
            }
        } catch (Throwable ex) {
            log.warn("Failed to update engagement_rate: {}", ex.getMessage());
        }
    }


    private void updateProfileViewsInDatabase() {
        try {
            log.info("Updating profile_views in social_media");
            List<SocialMedia> allSm = socialMediaRepository.findAll();
            for (SocialMedia sm : allSm) {
                sm.calculateProfileViews();
                socialMediaRepository.save(sm);
                
                if (sm.getProfileViews() != null && sm.getProfileViews() > 0) {
                    log.info("Updated profile_views for {} ({}): {}", 
                        sm.getUsername(), sm.getPlatform(), sm.getProfileViews());
                }
            }
        } catch (Throwable ex) {
            log.warn("Failed to update profile_views: {}", ex.getMessage());
        }
    }
}

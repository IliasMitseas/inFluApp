package org.ilias.influapp.config;

import org.ilias.influapp.entities.*;
import org.ilias.influapp.entities.Enums.Category;
import org.ilias.influapp.entities.Enums.CampaignStatus;
import org.ilias.influapp.entities.Enums.CollaborationStatus;
import org.ilias.influapp.entities.Enums.Platform;
import org.ilias.influapp.entities.Enums.UserRole;
import org.ilias.influapp.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ilias.influapp.services.HybridSentimentService;
import org.ilias.influapp.services.PostService;
import org.ilias.influapp.entities.Enums.ReactionType;
import org.springframework.security.crypto.password.PasswordEncoder;

@Component
@Order(2)
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
    private final PostService postService;
    private final PasswordEncoder passwordEncoder;
    private final HybridSentimentService hybridSentimentService;

    public TestDataLoader(BusinessRepository businessRepository,
                          InfluencerRepository influencerRepository,
                          CampaignRepository campaignRepository,
                          CollaborationRepository collaborationRepository,
                          PostRepository postRepository,
                          SentimentAnalysisRepository sentimentAnalysisRepository,
                          SocialMediaRepository socialMediaRepository,
                          ReactionRepository reactionRepository,
                          PasswordEncoder passwordEncoder,
                          HybridSentimentService hybridSentimentService,
                          PostService postService) {
        this.businessRepository = businessRepository;
        this.influencerRepository = influencerRepository;
        this.campaignRepository = campaignRepository;
        this.collaborationRepository = collaborationRepository;
        this.postRepository = postRepository;
        this.sentimentAnalysisRepository = sentimentAnalysisRepository;
        this.socialMediaRepository = socialMediaRepository;
        this.reactionRepository = reactionRepository;
        this.passwordEncoder = passwordEncoder;
        this.hybridSentimentService = hybridSentimentService;
        this.postService = postService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        log.info("TestDataLoader starting - ensuring comprehensive test data exists...");

        // Force recreate: delete existing reactions, sentiment analyses, posts and collaborations
        try {
            log.info("TestDataLoader force-recreate: deleting reactions, sentiment analyses, posts and collaborations...");
            reactionRepository.findAll().forEach(r -> reactionRepository.delete(r));
            sentimentAnalysisRepository.findAll().forEach(sa -> sentimentAnalysisRepository.delete(sa));
            postRepository.findAll().forEach(p -> postRepository.delete(p));
            collaborationRepository.findAll().forEach(c -> collaborationRepository.delete(c));
            log.info("TestDataLoader force-recreate: deletions complete.");
        } catch (Throwable ex) {
            log.warn("TestDataLoader force-recreate deletion failed: {}", ex.getMessage());
        }

        // Create 3 businesses with detailed profiles
        Business biz1 = createOrUpdateBusiness("biz1@example.com", "biz1", "ACME Corp", "pass");
        Business biz2 = createOrUpdateBusiness("biz2@example.com", "biz2", "Beta LLC", "pass");
        Business biz3 = createOrUpdateBusiness("biz3@example.com", "biz3", "Gamma Co", "pass");

        // Create campaigns for each business
        createCampaignIfNotExists(biz1, "Summer Promo", 1500.0);
        createCampaignIfNotExists(biz1, "Holiday Sale", 2000.0);
        createCampaignIfNotExists(biz2, "New Product Launch", 3000.0);
        createCampaignIfNotExists(biz2, "Q2 Marketing", 2500.0);
        createCampaignIfNotExists(biz3, "Brand Awareness", 1200.0);
        createCampaignIfNotExists(biz3, "Product Demo", 1800.0);

        // Create 6 influencers with complete profiles
        Influencer inf1 = createOrUpdateInfluencer("inf1@example.com", "inf1", "Inf One", "pass", true);
        Influencer inf2 = createOrUpdateInfluencer("inf2@example.com", "inf2", "Inf Two", "pass", true);
        Influencer inf3 = createOrUpdateInfluencer("inf3@example.com", "inf3", "Inf Three", "pass", false);
        Influencer inf4 = createOrUpdateInfluencer("inf4@example.com", "inf4", "Inf Four", "pass", true);
        Influencer inf5 = createOrUpdateInfluencer("inf5@example.com", "inf5", "Inf Five", "pass", true);
        Influencer inf6 = createOrUpdateInfluencer("inf6@example.com", "inf6", "Inf Six", "pass", false);

        // Add detailed profile information for each influencer
        try {
            inf1.setAge("29");
            inf1.setLocation("Athens, GR");
            inf1.setBio("Travel & lifestyle content creator. Loves coffee and photography.");
            inf1.setTotalFollowers(65000);
            inf1.setEngagementRate(new java.math.BigDecimal("5.8"));
            inf1.setInfluencerScore(82.5);
            influencerRepository.save(inf1);
        } catch (Throwable ignored) {}
        try {
            inf2.setAge("24");
            inf2.setLocation("Thessaloniki, GR");
            inf2.setBio("Fashion and beauty influencer focusing on sustainable brands.");
            inf2.setTotalFollowers(45000);
            inf2.setEngagementRate(new java.math.BigDecimal("6.2"));
            inf2.setInfluencerScore(78.0);
            influencerRepository.save(inf2);
        } catch (Throwable ignored) {}
        try {
            inf3.setAge("31");
            inf3.setLocation("London, UK");
            inf3.setBio("Short-form comedy and sketches. Making people laugh every day.");
            inf3.setTotalFollowers(120000);
            inf3.setEngagementRate(new java.math.BigDecimal("7.5"));
            inf3.setInfluencerScore(88.3);
            influencerRepository.save(inf3);
        } catch (Throwable ignored) {}
        try {
            inf4.setAge("27");
            inf4.setLocation("Berlin, DE");
            inf4.setBio("Tech reviewer and educator. Reviews gadgets and apps with depth.");
            inf4.setTotalFollowers(95000);
            inf4.setEngagementRate(new java.math.BigDecimal("5.4"));
            inf4.setInfluencerScore(84.7);
            influencerRepository.save(inf4);
        } catch (Throwable ignored) {}
        try {
            inf5.setAge("22");
            inf5.setLocation("Athens, GR");
            inf5.setBio("Student & lifestyle vlogger, sharing study tips and daily routines.");
            inf5.setTotalFollowers(38000);
            inf5.setEngagementRate(new java.math.BigDecimal("4.8"));
            inf5.setInfluencerScore(71.2);
            influencerRepository.save(inf5);
        } catch (Throwable ignored) {}
        try {
            inf6.setAge("35");
            inf6.setLocation("New York, US");
            inf6.setBio("Food photographer and recipe creator with culinary background.");
            inf6.setTotalFollowers(78000);
            inf6.setEngagementRate(new java.math.BigDecimal("6.7"));
            inf6.setInfluencerScore(86.1);
            influencerRepository.save(inf6);
        } catch (Throwable ignored) {}

        // Create social media accounts for influencers
        createSocialMediaIfNotExists(inf1, Platform.INSTAGRAM, "@inf1", "https://instagram.com/inf1");
        createSocialMediaIfNotExists(inf1, Platform.YOUTUBE, "InfOneChannel", "https://youtube.com/inf1");

        createSocialMediaIfNotExists(inf2, Platform.INSTAGRAM, "@inf2", "https://instagram.com/inf2");
        createSocialMediaIfNotExists(inf2, Platform.TIKTOK, "@inf2t", "https://tiktok.com/@inf2");

        createSocialMediaIfNotExists(inf3, Platform.TIKTOK, "@inf3", "https://tiktok.com/@inf3");
        createSocialMediaIfNotExists(inf3, Platform.FACEBOOK, "Inf3FB", "https://facebook.com/inf3");

        createSocialMediaIfNotExists(inf4, Platform.YOUTUBE, "InfFourYT", "https://youtube.com/inf4");
        createSocialMediaIfNotExists(inf4, Platform.INSTAGRAM, "@inf4", "https://instagram.com/inf4");

        createSocialMediaIfNotExists(inf5, Platform.INSTAGRAM, "@inf5", "https://instagram.com/inf5");
        createSocialMediaIfNotExists(inf5, Platform.FACEBOOK, "Inf5FB", "https://facebook.com/inf5");

        createSocialMediaIfNotExists(inf6, Platform.INSTAGRAM, "@inf6", "https://instagram.com/inf6");
        createSocialMediaIfNotExists(inf6, Platform.TIKTOK, "@inf6t", "https://tiktok.com/@inf6");

        // Create collaborations and posts for each collaboration
        log.info("Creating collaborations with posts...");
        
        // Biz1 -> Inf1 (Summer Promo) + post
        Campaign summer = campaignRepository.findByBusinessId(biz1.getId()).stream().filter(c -> "Summer Promo".equals(c.getTitle())).findFirst().orElse(null);
        if (summer != null) {
            Collaboration c1 = createCollaborationIfNotExists(summer, inf1, CollaborationStatus.ACCEPTED, 350.0);
            SocialMedia sm = socialMediaRepository.findByInfluencerIdAndPlatform(inf1.getId(), Platform.INSTAGRAM).orElse(null);
            if (c1 != null && sm != null) {
                createPostForCollaboration(c1, sm, "Check out ACME's Summer Promo! Amazing deals for the season 🌞", 
                    List.of("Love it!", "Great products!", "Where can I buy?", "Amazing quality!"), 
                    25000, 75000, Map.of(ReactionType.LIKE, 1200, ReactionType.LOVE, 300, ReactionType.WOW, 50, ReactionType.HAHA, 25), 300);
            }
        }

        // Biz1 -> Inf2 (Holiday Sale) + post
        Campaign holiday = campaignRepository.findByBusinessId(biz1.getId()).stream().filter(c -> "Holiday Sale".equals(c.getTitle())).findFirst().orElse(null);
        if (holiday != null) {
            Collaboration c2 = createCollaborationIfNotExists(holiday, inf2, CollaborationStatus.ACCEPTED, 400.0);
            SocialMedia sm = socialMediaRepository.findByInfluencerIdAndPlatform(inf2.getId(), Platform.TIKTOK).orElse(null);
            if (c2 != null && sm != null) {
                createPostForCollaboration(c2, sm, "Holiday deals are LIVE! 🎄✨ Don't miss these incredible offers!", 
                    List.of("Nice deals!", "Where to buy?", "Love this!", "Must have!"), 
                    15000, 45000, Map.of(ReactionType.LIKE, 800, ReactionType.LOVE, 120, ReactionType.HAHA, 40, ReactionType.ANGRY, 10), 120);
            }
        }

        // Biz2 -> Inf3 (New Product Launch) + post
        Campaign launch = campaignRepository.findByBusinessId(biz2.getId()).stream().filter(c -> "New Product Launch".equals(c.getTitle())).findFirst().orElse(null);
        if (launch != null) {
            Collaboration c3 = createCollaborationIfNotExists(launch, inf3, CollaborationStatus.ACCEPTED, 500.0);
            SocialMedia sm = socialMediaRepository.findByInfluencerIdAndPlatform(inf3.getId(), Platform.TIKTOK).orElse(null);
            if (c3 != null && sm != null) {
                createPostForCollaboration(c3, sm, "Launching the new Beta product! 🚀 This is CRAZY good. Check it out!", 
                    List.of("Congrats!", "Can't wait to try it!", "Tell us more please!", "Looks awesome!"), 
                    40000, 120000, Map.of(ReactionType.LIKE, 2000, ReactionType.WOW, 500, ReactionType.LOVE, 300, ReactionType.SAD, 5), 800);
            }
        }

        // Biz3 -> Inf4 (Brand Awareness) + post
        Campaign ba = campaignRepository.findByBusinessId(biz3.getId()).stream().filter(c -> "Brand Awareness".equals(c.getTitle())).findFirst().orElse(null);
        if (ba != null) {
            Collaboration c4 = createCollaborationIfNotExists(ba, inf4, CollaborationStatus.ACCEPTED, 250.0);
            SocialMedia sm = socialMediaRepository.findByInfluencerIdAndPlatform(inf4.getId(), Platform.YOUTUBE).orElse(null);
            if (c4 != null && sm != null) {
                createPostForCollaboration(c4, sm, "Talking about Gamma Co today - their solutions are game-changing for the tech industry", 
                    List.of("Informative review!", "Good overview!", "Nice video production!", "Very detailed!"), 
                    8000, 22000, Map.of(ReactionType.LIKE, 300, ReactionType.LOVE, 20, ReactionType.WOW, 10, ReactionType.SAD, 2), 40);
            }
        }

        // Biz2 -> Inf5 (New Product Launch) + post
        if (launch != null) {
            Collaboration c5 = createCollaborationIfNotExists(launch, inf5, CollaborationStatus.ACCEPTED, 320.0);
            SocialMedia sm = socialMediaRepository.findByInfluencerIdAndPlatform(inf5.getId(), Platform.INSTAGRAM).orElse(null);
            if (c5 != null && sm != null) {
                createPostForCollaboration(c5, sm, "Beta launch first impressions! 📸 This product is really impressive", 
                    List.of("Cool features!", "Looks good to me!", "Where to buy?", "Love the design!"), 
                    32000, 95000, Map.of(ReactionType.LIKE, 1500, ReactionType.LOVE, 200, ReactionType.WOW, 150, ReactionType.ANGRY, 15), 220);
            }
        }

        // Biz3 -> Inf6 (Product Demo) + post
        Campaign demo = campaignRepository.findByBusinessId(biz3.getId()).stream().filter(c -> "Product Demo".equals(c.getTitle())).findFirst().orElse(null);
        if (demo != null) {
            Collaboration c6 = createCollaborationIfNotExists(demo, inf6, CollaborationStatus.ACCEPTED, 280.0);
            SocialMedia sm = socialMediaRepository.findByInfluencerIdAndPlatform(inf6.getId(), Platform.INSTAGRAM).orElse(null);
            if (c6 != null && sm != null) {
                createPostForCollaboration(c6, sm, "Featuring Gamma Co's latest products 👨‍🍳 Quality you can taste and see!", 
                    List.of("Mouth-watering!", "Need this now!", "Beautiful presentation!", "Definitely interested!"), 
                    28000, 85000, Map.of(ReactionType.LIKE, 1800, ReactionType.LOVE, 450, ReactionType.WOW, 80, ReactionType.HAHA, 30), 180);
            }
        }

        // Add some PENDING collaborations (not accepted yet)
        Campaign q2 = campaignRepository.findByBusinessId(biz2.getId()).stream().filter(c -> "Q2 Marketing".equals(c.getTitle())).findFirst().orElse(null);
        if (q2 != null) {
            Collaboration pending1 = createCollaborationIfNotExists(q2, inf2, CollaborationStatus.PENDING, 275.0);
            log.info("Created PENDING collaboration c2={} (not accepted)", pending1.getId());
        }

        // Update all SocialMedia metrics using direct SQL instead of entity methods
        log.info("Updating SocialMedia metrics using database updates...");
        try {
            // Use direct SQL UPDATE to calculate average_likes from reactions
            updateAverageLikesInDatabase();
            updateEngagementRateInDatabase();
            updateAverageCommentsInDatabase();
            updateProfileViewsInDatabase();
            
            log.info("✓ All SocialMedia metrics updated successfully via SQL");
        } catch (Throwable ex) {
            log.warn("✗ Failed to update SocialMedia metrics: {}", ex.getMessage());
            ex.printStackTrace();
        }

        log.info("TestDataLoader finished - comprehensive test data seeding complete");
        
        // Summary counts to help verification
        try {
            long bizCount = businessRepository.count();
            long infCount = influencerRepository.count();
            long campCount = campaignRepository.count();
            long collCount = collaborationRepository.count();
            long postCount = postRepository.count();
            long reactionCount = reactionRepository.count();
            long sentimentCount = sentimentAnalysisRepository.count();
            long smCount = socialMediaRepository.count();
            log.info("TestDataLoader SUMMARY: businesses={}, influencers={}, campaigns={}, collaborations={}, posts={}, reactions={}, sentiments={}, socialMedia={}", 
                bizCount, infCount, campCount, collCount, postCount, reactionCount, sentimentCount, smCount);
        } catch (Throwable ignored) {}

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
    private Business createOrUpdateBusiness(String email, String username, String companyName, String rawPassword) {
        Business b = businessRepository.findAll().stream().filter(x -> email.equals(x.getEmail())).findFirst().orElse(null);
        if (b == null) {
            b = Business.builder()
                    .email(email)
                    .username(username)
                    .password(ensureEncoded(rawPassword))
                    .role(UserRole.BUSINESS)
                    .companyName(companyName)
                    .build();
            // Set comprehensive business details
            try { b.setCategory(Category.OTHER); } catch (Throwable ignored) {}
            try { b.setCompanySize(org.ilias.influapp.entities.Enums.CompanySize.MEDIUM); } catch (Throwable ignored) {}
            try { b.setEstablishedYear("2020"); } catch (Throwable ignored) {}
            try { b.setDescription("Professional " + companyName + " - Leaders in their industry"); } catch (Throwable ignored) {}
            try { b.setPhone("+30-210-" + (9000000 + (int)(Math.random()*1000000))); } catch (Throwable ignored) {}
            try { b.setContactEmail(email); } catch (Throwable ignored) {}
            try { b.setAddress((100 + (int)(Math.random()*900)) + " Syntagma Ave, Athens, 105 64"); } catch (Throwable ignored) {}
            try { b.setWebSite("https://www." + username + ".gr"); } catch (Throwable ignored) {}
            b = businessRepository.save(b);
            log.info("Created business {} id={} (category={}, companySize={}, address set)", email, b.getId(), b.getCategory(), b.getCompanySize());
        } else {
            b.setUsername(username);
            b.setPassword(ensureEncoded(rawPassword));
            b.setCompanyName(companyName);
            try { b.setCategory(Category.OTHER); } catch (Throwable ignored) {}
            try { b.setCompanySize(org.ilias.influapp.entities.Enums.CompanySize.MEDIUM); } catch (Throwable ignored) {}
            try { b.setEstablishedYear("2020"); } catch (Throwable ignored) {}
            try { b.setDescription("Professional " + companyName + " - Leaders in their industry"); } catch (Throwable ignored) {}
            try { b.setPhone("+30-210-" + (9000000 + (int)(Math.random()*1000000))); } catch (Throwable ignored) {}
            try { b.setContactEmail(email); } catch (Throwable ignored) {}
            try { b.setAddress((100 + (int)(Math.random()*900)) + " Syntagma Ave, Athens, 105 64"); } catch (Throwable ignored) {}
            try { b.setWebSite("https://www." + username + ".gr"); } catch (Throwable ignored) {}
            b = businessRepository.save(b);
            log.info("Updated business {} id={} (all fields refreshed)", email, b.getId());
        }
        return b;
    }

    private Campaign createCampaignIfNotExists(Business b, String title, Double budget) {
        Campaign existing = campaignRepository.findByBusinessId(b.getId()).stream().filter(c -> title.equals(c.getTitle())).findFirst().orElse(null);
        if (existing != null) return existing;
        Campaign camp = Campaign.builder()
                .business(b)
                .title(title)
                .description(title + " for " + b.getCompanyName())
                .status(CampaignStatus.DRAFT)
                .targetCategory(Category.OTHER)
                .budget(budget)
                .startDate(LocalDate.now())
                .build();
        camp = campaignRepository.save(camp);
        b.addCampaign(camp);
        businessRepository.save(b);
        log.info("Created campaign '{}' for business id={}", title, b.getId());
        return camp;
    }

    private Influencer createOrUpdateInfluencer(String email, String username, String name, String rawPassword, boolean available) {
        Influencer inf = influencerRepository.findAll().stream().filter(x -> email.equals(x.getEmail())).findFirst().orElse(null);
        if (inf == null) {
            Influencer newInf = Influencer.builder()
                    .email(email)
                    .username(username)
                    .password(ensureEncoded(rawPassword))
                    .role(UserRole.INFLUENCER)
                    .name(name)
                    .isAvailable(available)
                    .build();
            // Set comprehensive influencer profile details
            try { newInf.setAge(String.valueOf(20 + (int)(Math.random()*15))); } catch (Throwable ignored) {}
            try { newInf.setLocation(new String[]{"Athens, GR", "Thessaloniki, GR", "London, UK", "Berlin, DE", "New York, US"}[(int)(Math.random()*5)]); } catch (Throwable ignored) {}
            try { newInf.setBio("Professional content creator - " + name + ". Creating engaging content for brands and communities."); } catch (Throwable ignored) {}
            try { newInf.setCategory(Category.OTHER); } catch (Throwable ignored) {}
            try { newInf.setInfluencerType(org.ilias.influapp.entities.Enums.InfluencerType.MICRO); } catch (Throwable ignored) {}
            try { newInf.setTotalFollowers(10000 + (int)(Math.random()*90000)); } catch (Throwable ignored) {}
            try { newInf.setEngagementRate(new java.math.BigDecimal(String.valueOf(2.5 + (Math.random()*7.5))).setScale(2, java.math.RoundingMode.HALF_UP)); } catch (Throwable ignored) {}
            try { newInf.setInfluencerScore(60.0 + (Math.random()*35)); } catch (Throwable ignored) {}
            try { newInf.setMinCollaborationBudget(100 + (int)(Math.random()*900)); } catch (Throwable ignored) {}
            inf = influencerRepository.save(newInf);
            log.info("Created influencer {} id={} (followers={}, engagementRate={}, score={})", email, inf.getId(), inf.getTotalFollowers(), inf.getEngagementRate(), inf.getInfluencerScore());
        } else {
            inf.setUsername(username);
            inf.setPassword(ensureEncoded(rawPassword));
            inf.setName(name);
            inf.setIsAvailable(available);
            try { inf.setAge(String.valueOf(20 + (int)(Math.random()*15))); } catch (Throwable ignored) {}
            try { inf.setLocation(new String[]{"Athens, GR", "Thessaloniki, GR", "London, UK", "Berlin, DE", "New York, US"}[(int)(Math.random()*5)]); } catch (Throwable ignored) {}
            try { inf.setBio("Professional content creator - " + name + ". Creating engaging content for brands and communities."); } catch (Throwable ignored) {}
            try { inf.setCategory(Category.OTHER); } catch (Throwable ignored) {}
            try { inf.setInfluencerType(org.ilias.influapp.entities.Enums.InfluencerType.MICRO); } catch (Throwable ignored) {}
            try { inf.setTotalFollowers(10000 + (int)(Math.random()*90000)); } catch (Throwable ignored) {}
            try { inf.setEngagementRate(new java.math.BigDecimal(String.valueOf(2.5 + (Math.random()*7.5))).setScale(2, java.math.RoundingMode.HALF_UP)); } catch (Throwable ignored) {}
            try { inf.setInfluencerScore(60.0 + (Math.random()*35)); } catch (Throwable ignored) {}
            try { inf.setMinCollaborationBudget(100 + (int)(Math.random()*900)); } catch (Throwable ignored) {}
            inf = influencerRepository.save(inf);
            log.info("Updated influencer {} id={} (all profile fields refreshed)", email, inf.getId());
        }
        return inf;
    }

    private SocialMedia createSocialMediaIfNotExists(Influencer inf, Platform platform, String username, String url) {
        SocialMedia sm = socialMediaRepository.findByInfluencerIdAndPlatform(inf.getId(), platform).orElse(null);
        if (sm == null) {
            SocialMedia newSm = new SocialMedia();
            newSm.setPlatform(platform);
            newSm.setUsername(username);
            newSm.setAccountUrl(url);
            newSm.setInfluencer(inf);
            // Set realistic follower counts based on platform
            setFollowersForPlatform(newSm, platform);
            sm = socialMediaRepository.save(newSm);
            inf.addSocialMediaAccount(sm);
            influencerRepository.save(inf);
            log.info("Created social media {} for influencer id={} (followers={})", platform, inf.getId(), sm.getFollowers());
        } else {
            // Even if it exists, ensure followers are set correctly
            if (sm.getFollowers() == null || sm.getFollowers() == 0) {
                setFollowersForPlatform(sm, platform);
                socialMediaRepository.save(sm);
                log.info("Updated existing social media {} for influencer id={} (followers={})", platform, inf.getId(), sm.getFollowers());
            }
        }
        return sm;
    }

    private void setFollowersForPlatform(SocialMedia sm, Platform platform) {
        try {
            switch(platform) {
                case INSTAGRAM -> sm.setFollowers(15000 + (int)(Math.random()*50000));
                case YOUTUBE -> sm.setFollowers(40000 + (int)(Math.random()*100000));
                case TIKTOK -> sm.setFollowers(25000 + (int)(Math.random()*75000));
                case FACEBOOK -> sm.setFollowers(10000 + (int)(Math.random()*30000));
                default -> sm.setFollowers(5000 + (int)(Math.random()*20000));
            }
        } catch (Throwable ignored) {}
    }

    private Collaboration createCollaborationIfNotExists(Campaign camp, Influencer inf, CollaborationStatus status, Double payment) {
        for (Collaboration c : collaborationRepository.findAll()) {
            if (c.getCampaign() != null && c.getInfluencer() != null
                    && camp.getId() != null && c.getCampaign().getId() != null
                    && inf.getId() != null && c.getInfluencer().getId() != null
                    && c.getCampaign().getId().equals(camp.getId())
                    && c.getInfluencer().getId().equals(inf.getId())) {
                return c;
            }
        }
        Collaboration coll = new Collaboration();
        coll.setCampaign(camp);
        coll.setInfluencer(inf);
        coll.setStatus(status);
        coll.setPaymentAmount(payment);
        coll.setStartDate(LocalDate.now());
        coll = collaborationRepository.save(coll);
        camp.addCollaboration(coll);
        campaignRepository.save(camp);
        inf.addCollaboration(coll);
        influencerRepository.save(inf);
        log.info("Created collaboration id={} campaignId={} influencerId={}", coll.getId(), camp.getId(), inf.getId());
        return coll;
    }

    private Post createPostForCollaboration(Collaboration coll, SocialMedia sm, String content, List<String> comments, Integer reach, Integer impressions, Map<ReactionType, Integer> reactionCounts, Integer shares) {
        // Find existing post by content under this social media
        Post existing = postRepository.findBySocialMediaId(sm.getId()).stream().filter(p -> content.equals(p.getContent())).findFirst().orElse(null);
        if (existing != null) return existing;
        
        Post p = Post.builder()
                .content(content)
                .socialMedia(sm)
                .reach(reach)
                .impressionCount(impressions)
                .shares(shares)
                .build();
        p.setComments(comments != null ? comments : new ArrayList<>());
        p.setCollaboration(coll);
        p = postRepository.save(p);
        log.info("Created post id={} for collaborationId={} (reach={}, impressions={}, shares={})", p.getId(), coll.getId(), reach, impressions, shares);
        
        // Create and persist reactions
        List<Reaction> reactions = new ArrayList<>();
        if (reactionCounts != null && !reactionCounts.isEmpty()) {
            for (var entry : reactionCounts.entrySet()) {
                Reaction r = new Reaction();
                r.setPost(p);
                r.setType(entry.getKey());
                r.setCount(entry.getValue());
                reactions.add(r);
            }
            reactionRepository.saveAll(reactions);
            log.info("Created {} reactions for post id={}", reactions.size(), p.getId());
        }
        
        // Calculate engagement rate properly
        try {
            p.calculateAndSetEngagementRate();
            postRepository.save(p);
            log.info("Post id={} engagement rate calculated: {}", p.getId(), p.getEngagementRate());
        } catch (Throwable ex) {
            log.warn("Failed to calculate engagement rate for post {}: {}", p.getId(), ex.getMessage());
        }
        
        // Calculate sentiment using HybridSentimentService and SET postSentiment
        try {
            var sentiment = hybridSentimentService.analyzeAndSave(p, reactions, comments);
            // IMPORTANT: Set the postSentiment field on the Post entity
            p.setPostSentiment(sentiment);
            // Reload post to ensure sentiment analysis is properly linked
            postRepository.save(p);
            log.info("Post id={} sentiment analyzed and saved: {} (SentimentAnalysis linked)", p.getId(), sentiment);
        } catch (Throwable ex) {
            log.warn("Failed to calculate sentiment for post {}: {}", p.getId(), ex.getMessage());
        }
        
        // Ensure collaboration has post reference
        try {
            if (!coll.getPosts().contains(p)) {
                coll.addPost(p);
                collaborationRepository.save(coll);
            }
            log.info("Post id={} linked to collaboration id={}", p.getId(), coll.getId());
        } catch (Throwable ex) {
            log.warn("Failed to link post to collaboration: {}", ex.getMessage());
        }
        
        return p;
    }

    /**
     * Update average_likes using entity-based approach
     */
    private void updateAverageLikesInDatabase() {
        log.info("Updating average_likes using entity-based approach...");
        updateAverageLikesViaEntities();
    }

    /**
     * Fallback: Update average_likes using entity access with explicit reaction fetching
     */
    @Transactional
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
                    int postCount = 0;
                    
                    // Get posts for this social media
                    if (sm.getPosts() != null && !sm.getPosts().isEmpty()) {
                        postCount = sm.getPosts().size();
                        log.info("  Found {} posts", postCount);
                        
                        // For each post, fetch reactions directly from database
                        for (Post p : sm.getPosts()) {
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

    /**
     * Update average_comments using post comments
     */
    private void updateAverageCommentsInDatabase() {
        try {
            log.info("Updating average_comments in social_media");
            List<SocialMedia> allSm = socialMediaRepository.findAll();
            for (SocialMedia sm : allSm) {
                int totalComments = 0;
                int postCount = 0;
                
                if (sm.getPosts() != null && !sm.getPosts().isEmpty()) {
                    postCount = sm.getPosts().size();
                    for (Post p : sm.getPosts()) {
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

    /**
     * Update engagement_rate from post engagement rates
     */
    private void updateEngagementRateInDatabase() {
        try {
            log.info("Updating engagement_rate in social_media");
            List<SocialMedia> allSm = socialMediaRepository.findAll();
            for (SocialMedia sm : allSm) {
                double totalEngagement = 0.0;
                int postCount = 0;
                
                if (sm.getPosts() != null && !sm.getPosts().isEmpty()) {
                    for (Post p : sm.getPosts()) {
                        if (p != null) {
                            postCount++;
                            if (p.getEngagementRate() != null) {
                                totalEngagement += p.getEngagementRate();
                            }
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

    /**
     * Update profile_views based on followers and platform
     */
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

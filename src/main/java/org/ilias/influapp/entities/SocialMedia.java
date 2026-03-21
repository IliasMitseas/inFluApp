package org.ilias.influapp.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.ilias.influapp.entities.Enums.Platform;
import org.ilias.influapp.config.SocialMediaEntityListener;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "social_media")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(SocialMediaEntityListener.class)
public class SocialMedia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "influencer_id", nullable = false)
    private Influencer influencer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Platform platform;

    @Column(name = "account_url", nullable = false)
    private String accountUrl;

    @Column(name = "username")
    private String username;

    @Column(name = "average_comments")
    private Integer averageComments;

    @Column(name = "average_likes")
    private Integer averageLikes;

    @Column(name = "profile_views")
    private Integer profileViews;

    private Integer followers;

    private Double engagementRate;

    @OneToMany(mappedBy = "socialMedia", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Post> posts = new ArrayList<>();

    public void addPost(Post post) {
        if (post == null) {
            return;
        }
        posts.add(post);
        post.setSocialMedia(this);
        updateAverages();
    }

    public void removePost(Post post) {
        if (post == null) {
            return;
        }
        posts.remove(post);
        post.setSocialMedia(null);
        updateAverages();
    }


    public void updateAverages() {
        if (posts == null || posts.isEmpty()) {
            this.averageLikes = 0;
            this.averageComments = 0;
            return;
        }

        int totalLikes = 0;
        int totalComments = 0;

        for (Post p : posts) {
            totalLikes += p.getTotalReactions();
            totalComments += (p.getComments() != null) ? p.getComments().size() : 0;
        }

        this.averageLikes = totalLikes / posts.size();
        this.averageComments = totalComments / posts.size();
    }

    public void calculateProfileViews() {
        int baseProfileViews = followers != null ? followers / 5 : 100;
        
        int reachBasedViews = 0;
        if (posts != null) {
            for (Post p : posts) {
                if (p != null && p.getReach() != null) {
                    reachBasedViews += p.getReach() / 10;
                }
            }
        }
        
        double platformMultiplier = switch(platform) {
            case INSTAGRAM -> 1.5;
            case TIKTOK -> 2.0;
            case YOUTUBE -> 1.2;
            case FACEBOOK -> 1.0;
            default -> 1.0;
        };
        
        this.profileViews = (int) ((baseProfileViews + reachBasedViews) * platformMultiplier);
    }
}

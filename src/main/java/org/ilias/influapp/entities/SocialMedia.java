package org.ilias.influapp.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "social_media")
@Data
@NoArgsConstructor
@AllArgsConstructor
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
    }

    public void removePost(Post post) {
    }

    // TODO
    public Double calculateEngagementRate() {
        return this.engagementRate;
    }
}

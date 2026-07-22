package com.dak.backend.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "community_posts")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityPost {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false, length = 30)
    private String category;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, length = 20)
    private String status = "PUBLISHED";

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    public static CommunityPost createNew(User author, String category, String title, String content) {
        OffsetDateTime now = OffsetDateTime.now();
        CommunityPost post = new CommunityPost();
        post.author = author;
        post.category = category;
        post.title = title;
        post.content = content;
        post.createdAt = now;
        post.updatedAt = now;
        return post;
    }
}
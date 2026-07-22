package com.dak.backend.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "community_comments")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityComment {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private CommunityPost post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    // Self-referencing: a reply points back to another row in this same table.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private CommunityComment parentComment;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, length = 20)
    private String status = "PUBLISHED";

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public static CommunityComment createNew(CommunityPost post, User author,
                                              CommunityComment parentComment, String content) {
        OffsetDateTime now = OffsetDateTime.now();
        CommunityComment comment = new CommunityComment();
        comment.post = post;
        comment.author = author;
        comment.parentComment = parentComment;
        comment.content = content;
        comment.createdAt = now;
        comment.updatedAt = now;
        return comment;
    }
}
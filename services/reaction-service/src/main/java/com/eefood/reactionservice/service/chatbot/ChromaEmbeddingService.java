package com.eefood.reactionservice.service.chatbot;

import com.eefood.reactionservice.model.Post;
import com.eefood.reactionservice.model.chatbot.PostChromaEmbedding;
import com.eefood.reactionservice.repository.chatbot.PostChromaEmbeddingRepository;
import com.eefood.reactionservice.repository.post.PostRepository;
import com.eefood.reactionservice.service.chatbot.cache.EmbeddingCacheService;
import com.eefood.reactionservice.enums.PostStatus;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChromaEmbeddingService {
    private final EmbeddingStore<TextSegment> chromaStore;
    private final PostRepository postRepo;
    private final PostChromaEmbeddingRepository chromaRepo;
    private final EmbeddingCacheService embeddingCacheService;

    @Transactional
    public void syncSinglePostToChroma(Long postId) {

        Post post = postRepo.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + postId));

        syncOnePostToChroma(post);
    }

    @Transactional
    public Map<String, Long> syncApprovedPostsToChroma() {
        List<Post> posts = postRepo.findByStatusAndIsDeletedFalse(PostStatus.APPROVED).stream()
                .filter(post -> post.getRecipeId() != null)
                .toList();
        long failed = 0;

        for (Post post : posts) {
            try {
                syncOnePostToChroma(post);
            } catch (Exception e) {
                failed++;
                log.error("Failed to sync post {} to ChromaDB", post.getId(), e);
            }
        }

        long stored = chromaRepo.count();
        log.info("Chroma post backfill completed: eligiblePosts={}, failedPosts={}, totalStoredPosts={}",
                posts.size(), failed, stored);
        return Map.of(
                "eligiblePosts", (long) posts.size(),
                "failedPosts", failed,
                "totalStoredPosts", stored
        );
    }

    private void syncOnePostToChroma(Post post) {

        String content = buildEmbeddingContent(post);
        String newHash  = hash(content);

        Optional<PostChromaEmbedding> existingSegment = chromaRepo.findById(post.getId());

        if (existingSegment.isPresent()) {

            String oldHash = existingSegment.get().getContentHash();

            // Nếu nội dung không đổi → bỏ qua
            if (newHash.equals(oldHash)) {
                log.info("Post {} unchanged. Skipping embedding.", post.getId());
                return;
            }

            // Nếu nội dung đÃ đổi → xóa vector cũ
            log.info("Post {} changed. Deleting old vector and updating...", post.getId());
            deleteByPostId(post.getId());
        }

        // Tạo embedding (CHỈ 1 LẦN)
        float[] vector = embeddingCacheService.getOrCreateSafe(content);
        Embedding embedding = Embedding.from(vector);

        // 2) Tạo metadata
        Metadata metadata = Metadata.from(
                Map.of(
                        "postId", post.getId().toString(),
                        "contentHash", newHash,
                        "updatedAt", post.getUpdatedAt().toString()
                )
        );

        // Tạo TextSegment
        TextSegment segment = TextSegment.from(content, metadata);

        // Upsert vào Chroma (add = upsert)
        String embeddingId = chromaStore.add(embedding, segment);

        PostChromaEmbedding record = PostChromaEmbedding.builder()
                .postId(post.getId())
                .chromaEmbeddingId(embeddingId)
                .contentHash(newHash)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        chromaRepo.save(record);

    }

    private void deleteByPostId(Long postId) {
        Optional<PostChromaEmbedding> existing =
                chromaRepo.findById(postId);

        if (existing.isEmpty()) {
            log.info("No embedding record found for post {}", postId);
            return;
        }

        String embeddingId = existing.get().getChromaEmbeddingId();

        chromaStore.remove(embeddingId);
        chromaRepo.deleteById(postId);
    }


    private String buildEmbeddingContent(Post p) {
        return String.format(
                "%s. %s. %s",
                p.getTitle() == null ? "" : p.getTitle(),
                p.getDescription() == null ? "" : p.getDescription(),
                String.join(", ", p.getRecipeIngredientKeywords() == null ? List.of() : p.getRecipeIngredientKeywords())
        );
    }

    private String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encoded = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : encoded) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error hashing content", e);
        }
    }
}

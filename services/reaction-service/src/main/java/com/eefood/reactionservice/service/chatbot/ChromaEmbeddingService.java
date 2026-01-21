package com.eefood.reactionservice.service.chatbot;

import com.eefood.reactionservice.model.Post;
import com.eefood.reactionservice.model.chatbot.PostChromaEmbedding;
import com.eefood.reactionservice.repository.chatbot.PostChromaEmbeddingRepository;
import com.eefood.reactionservice.repository.post.PostRepository;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChromaEmbeddingService {

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> chromaStore;
    private final PostRepository postRepo;
    private final PostChromaEmbeddingRepository chromaRepo;


    @Transactional
    public void ensureEmbeddingsExist(List<Long> candidatePostIds) {

        if (candidatePostIds == null || candidatePostIds.isEmpty()) {
            return;
        }

        final int BATCH_SIZE = 20;
        final long BASE_DELAY_MS = 3000;
        final int MAX_RETRY = 3;

        log.info("Ensuring embeddings for {} candidate posts", candidatePostIds.size());

        int processed = 0;
        int retryCount = 0;

        for (int i = 0; i < candidatePostIds.size(); i++) {

            Long postId = candidatePostIds.get(i);

            // Chỉ tạo nếu chưa có embedding
            if (!chromaRepo.existsById(postId)) {

                try {
                    syncSinglePostToChroma(postId);
                    processed++;
                    retryCount = 0; // reset retry khi thành công

                } catch (Exception e) {
                    retryCount++;
                    log.error("Error creating embedding for post {}", postId, e);

                    if (retryCount >= MAX_RETRY) {
                        log.warn("Skipping post {} after {} retries", postId, MAX_RETRY);
                        retryCount = 0;
                    }
                }
            }

            // Sau mỗi batch → nghỉ một chút để tránh rate limit
            if (processed > 0 && processed % BATCH_SIZE == 0) {
                long delay = BASE_DELAY_MS + (processed / BATCH_SIZE) * 1500; // tăng dần delay
                log.info("Sleeping {} ms to avoid rate limit...", delay);

                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ignored) {}
            }
        }

        log.info("Finished ensuring embeddings. Total created: {}", processed);
    }


    @Transactional
    public void syncSinglePostToChroma(Long postId) {

        Post post = postRepo.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + postId));

        syncOnePostToChroma(post);
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
        Embedding embedding = embeddingModel.embed(content).content();

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

        log.info("Upserted post {} to Chroma (embeddingId={})",
                post.getId(), embeddingId);
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

        log.info("Deleted vector for post {} (embeddingId={})",
                postId, embeddingId);
    }


    private String buildEmbeddingContent(Post p) {
        return String.format(
                "%s. %s. %s",
                p.getTitle(),
                p.getDescription(),
                String.join(", ", p.getRecipeIngredientKeywords())
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

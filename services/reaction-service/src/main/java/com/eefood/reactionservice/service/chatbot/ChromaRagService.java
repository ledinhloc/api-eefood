package com.eefood.reactionservice.service.chatbot;

import com.eefood.reactionservice.dto.response.PostResponse;
import com.eefood.reactionservice.mapper.PostMapper;
import com.eefood.reactionservice.model.Post;
import com.eefood.reactionservice.repository.post.PostRepository;
import com.eefood.reactionservice.service.chatbot.cache.EmbeddingCacheService;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.comparison.IsIn;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChromaRagService {
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> chromaStore;
    private final PostRepository postRepo;
    private final PostMapper postMapper;
    private final EmbeddingCacheService embeddingCacheService;

    @Transactional(readOnly = true)
    public List<PostResponse> retrieveTopKSimilarPosts(
            List<Long> candidatePostIds,
            String query,
            List<String> ingredients,
            int k
    ) {
        if (candidatePostIds == null || candidatePostIds.isEmpty()) {
            return List.of();
        }

        String enhancedQuery = buildRagQuery(
                query == null || query.isBlank() ? String.join(" ", ingredients != null ? ingredients : List.of()) : query,
                ingredients
        );

        CompletableFuture<float[]> embeddingFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return embeddingCacheService.getOrCreateSafe(enhancedQuery);
            } catch (Exception e) {
                log.error("Embedding failed", e);
                return null;
            }
        });

        CompletableFuture<List<PostResponse>> fallbackFuture = CompletableFuture.supplyAsync(() ->
                fallbackToFirstKPosts(candidatePostIds, k)
        );

        float[] vector = embeddingFuture.join();
        if (vector == null) {
            return fallbackFuture.join();
        }


        // 2. ChromaDB search với filter
        Embedding queryEmbedding = Embedding.from(vector);
        Filter postIdFilter = createPostIdFilter(candidatePostIds);

        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(k)
                .minScore(0.15)
                .filter(postIdFilter)
                .build();

        EmbeddingSearchResult<TextSegment> result;
        try {
            result = chromaStore.search(searchRequest);
        } catch (Exception e) {
            log.error("ChromaDB search failed, using fallback", e);
            return fallbackToFirstKPosts(candidatePostIds, k);
        }

        if (result.matches().isEmpty()) {
            return fallbackToFirstKPosts(candidatePostIds, k);
        }

        List<Long> matchedIds = result.matches().stream()
                .map(m -> Long.valueOf(m.embedded().metadata().getString("postId")))
                .distinct()
                .limit(k)
                .toList();

        log.info("ChromaDB search result: {}", matchedIds);


        return postRepo.findAllById(matchedIds).stream()
                .sorted(Comparator.comparingInt(p -> matchedIds.indexOf(p.getId())))
                .map(postMapper::toResponse)
                .toList();
    }

    private Filter createPostIdFilter(List<Long> candidatePostIds) {
        Collection<String> postIdStrings = candidatePostIds.stream()
                .map(String::valueOf)
                .toList();
        return new IsIn("postId", postIdStrings);
    }

    private String buildRagQuery(String userQuery, List<String> ingredients) {
        if (ingredients == null || ingredients.isEmpty()) {
            return userQuery;
        }
        return userQuery + " " + String.join(", ", ingredients);
    }

    private List<PostResponse> fallbackToFirstKPosts(List<Long> candidatePostIds, int k) {

        List<Long> selectedIds = candidatePostIds.stream().limit(k).toList();

        Map<Long, Post> postMap = postRepo.findAllById(selectedIds).stream()
                .collect(Collectors.toMap(Post::getId, p -> p));

        return selectedIds.stream()
                .map(postMap::get)
                .filter(Objects::nonNull)
                .map(postMapper::toResponse)
                .toList();
    }
}

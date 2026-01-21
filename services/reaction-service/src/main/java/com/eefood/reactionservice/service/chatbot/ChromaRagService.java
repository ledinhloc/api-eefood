package com.eefood.reactionservice.service.chatbot;

import com.eefood.reactionservice.dto.response.PostResponse;
import com.eefood.reactionservice.mapper.PostMapper;
import com.eefood.reactionservice.model.Post;
import com.eefood.reactionservice.repository.post.PostRepository;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import dev.langchain4j.store.embedding.filter.comparison.IsIn;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChromaRagService {
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> chromaStore;
    private final PostRepository postRepo;
    private final PostMapper postMapper;

    public List<PostResponse> retrieveTopKSimilarPosts(
            List<Long> candidatePostIds,
            String query,
            List<String> ingredients,
            int k
    ) {
        if (candidatePostIds == null || candidatePostIds.isEmpty()) {
            log.warn("candidatePostIds is empty, returning empty result");
            return List.of();
        }

        // Build enhanced query with ingredients
        String enhancedQuery = buildRagQuery(query, ingredients);
        log.info("Enhanced query: {}", enhancedQuery);

        // Create embedding for the query
        Embedding queryEmbedding = embeddingModel.embed(enhancedQuery).content();

        // Create filter to only search within candidatePostIds
        Filter postIdFilter = createPostIdFilter(candidatePostIds);

        // Build search request with filter
        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(Math.min(k * 3, candidatePostIds.size())) // Lấy nhiều hơn để có buffer
                .minScore(0.3) // Threshold để loại bỏ kết quả quá không liên quan
                .filter(postIdFilter)
                .build();

        // Execute search
        EmbeddingSearchResult<TextSegment> result = chromaStore.search(searchRequest);

        log.info("Chroma search returned {} matches for query: {}",
                result.matches().size(), query);

        if (result.matches().isEmpty()) {
            log.warn("No matches found in ChromaDB for candidatePostIds: {}", candidatePostIds);
            // Fallback: trả về k bài post đầu tiên từ candidatePostIds
            return fallbackToFirstKPosts(candidatePostIds, k);
        }

        // Extract matched post IDs and preserve order by similarity score
        List<Long> matchedIds = result.matches().stream()
                .peek(m -> log.debug("Match - PostId: {}, Score: {}",
                        m.embedded().metadata().getString("postId"), m.score()))
                .map(m -> Long.valueOf(m.embedded().metadata().getString("postId")))
                .distinct()
                .limit(k)
                .toList();

        log.info("Final matched postIds: {}", matchedIds);

        // Fetch posts and maintain order
        Map<Long, Post> postMap = postRepo.findAllById(matchedIds)
                .stream()
                .collect(Collectors.toMap(Post::getId, p -> p));

        // Preserve the order from Chroma results
        return matchedIds.stream()
                .map(postMap::get)
                .filter(Objects::nonNull)
                .map(postMapper::toResponse)
                .toList();
    }

    /**
     * Tạo filter để chỉ search trong danh sách candidatePostIds
     */
    private Filter createPostIdFilter(List<Long> candidatePostIds) {
        // Convert Long to String vì metadata lưu dưới dạng String
        Collection<String> postIdStrings = candidatePostIds.stream()
                .map(String::valueOf)
                .collect(Collectors.toList());

        // Tạo filter: postId IN (candidatePostIds)
        return new IsIn("postId", postIdStrings);
    }

    /**
     * Build enhanced query với ingredients và context
     */
    private String buildRagQuery(String userQuery, List<String> ingredients) {
        if (ingredients == null || ingredients.isEmpty()) {
            return userQuery;
        }

        // Tăng cường query với thông tin nguyên liệu
        return userQuery + "\nNguyên liệu tôi có: " +
                String.join(", ", ingredients);
    }

    /**
     * Fallback khi ChromaDB không tìm thấy kết quả nào
     * Trả về k bài post đầu tiên từ candidatePostIds (đã được filter bởi Elasticsearch)
     */
    private List<PostResponse> fallbackToFirstKPosts(List<Long> candidatePostIds, int k) {
        log.info("Using fallback: returning first {} posts from candidates", k);

        List<Long> selectedIds = candidatePostIds.stream()
                .limit(k)
                .toList();

        return postRepo.findAllById(selectedIds)
                .stream()
                .map(postMapper::toResponse)
                .toList();
    }
}

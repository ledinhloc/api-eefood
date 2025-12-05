package com.eefood.reactionservice.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.eefood.reactionservice.dto.SearchResult;
import com.eefood.reactionservice.model.PostDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostAdminSearchService {
    private final ElasticsearchClient client;

    public SearchResult searchPostIds(
            String keyword,
            Long userId,
            String region,
            String difficulty,
            String category,
            Integer minPrepTime,
            Integer maxPrepTime,
            Integer minCookTime,
            Integer maxCookTime,
            Integer minReactionCount,
            Integer minTotalShares,
            String status,
            String sortBy,
            Pageable pageable
    ) {
        try {
            int page = pageable.getPageNumber() + 1;
            int size = pageable.getPageSize();

            var response = client.search(s -> {
                        var search = s.index("posts")
                                .from((page - 1) * size)
                                .size(size);

                        search.query(q -> q.bool(b -> {

                            if (keyword != null && !keyword.isBlank()) {
                                b.must(m -> m.multiMatch(mm -> mm
                                        .fields("title", "description", "content")
                                        .query(keyword)
                                ));
                            }

                            if (userId != null) {
                                b.filter(f -> f.term(t -> t.field("userId").value(userId)));
                            }

                            if (status != null && !status.isBlank()) {
                                b.filter(f -> f.term(t -> t.field("status.keyword").value(status)));
                            }

                            if (region != null) {
                                b.filter(f -> f.term(t -> t.field("region.keyword").value(region)));
                            }

                            if (difficulty != null) {
                                b.filter(f -> f.term(t -> t.field("difficulty.keyword").value(difficulty)));
                            }

                            if (category != null) {
                                b.filter(f -> f.term(t -> t.field("recipeCategories.keyword").value(category)));
                            }

                            if (minPrepTime != null || maxPrepTime != null) {
                                b.filter(f -> f.range(rq -> rq.number(nrq -> {
                                    nrq.field("prepTime");
                                    if (minPrepTime != null) nrq.gte((double) minPrepTime);
                                    if (maxPrepTime != null) nrq.lte((double) maxPrepTime);

                                    return nrq;
                                }
                                )));
                            }

                            if (minCookTime != null || maxCookTime != null) {
                                b.filter(f -> f.range(rq -> rq.number(nrq -> {
                                    nrq.field("cookTime");
                                    if (minCookTime != null) nrq.gte((double) minCookTime);
                                    if (maxCookTime != null) nrq.lte((double) maxCookTime);
                                    return nrq;
                                }
                                )));
                            }

                            if (minReactionCount != null) {
                                b.filter(f -> f.range(r -> r.number(rq -> {
                                    rq.field("totalReactionCount");
                                    rq.gte((double) minReactionCount);
                                    return rq;
                                })));
                            }

                            if (minTotalShares != null) {
                                b.filter(f -> f.range(rq -> rq.number(nrq -> {
                                    nrq.field("totalShares");
                                    nrq.gte((double) minTotalShares);
                                    return nrq;
                                }
                                )));
                            }

                            return b;
                        }));

                        // Sorting
                        switch (sortBy.toLowerCase()) {
                            case "popular" ->
                                    search.sort(srt -> srt.field(f -> f.field("totalShares").order(SortOrder.Desc)));
                            case "toprated" ->
                                    search.sort(srt -> srt.field(f -> f.field("totalReactionCount").order(SortOrder.Desc)));
                            case "oldest" ->
                                    search.sort(srt -> srt.field(f -> f.field("createdAt").order(SortOrder.Asc)));
                            default ->
                                    search.sort(srt -> srt.field(f -> f.field("createdAt").order(SortOrder.Desc)));
                        }

                        return search;
                    }, PostDocument.class);
            List<Long> ids = response.hits()
                    .hits()
                    .stream()
                    .map(Hit::source)
                    .map(PostDocument::getId)
                    .toList();

            long total = response.hits().total() != null
                    ? response.hits().total().value()
                    : 0;

            return new SearchResult(ids, total);

        } catch (Exception e) {
            e.printStackTrace();
            return new SearchResult(List.of(), 0L);
        }
    }
}

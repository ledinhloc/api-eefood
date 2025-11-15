package com.eefood.reactionservice.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.search.Hit;
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

    public Page<Long> searchPostIds(
            String keyword,
            String region,
            String difficulty,
            String category,
            Integer minPrepTime,
            Integer maxPrepTime,
            Integer minCookTime,
            Integer maxCookTime,
            Integer minReactionCount,
            Integer minTotalShares,
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
                                b.filter(f -> f.range(rq -> rq.number(nrq -> nrq
                                        .field("prepTime")
                                        .from(minPrepTime != null ? (double) minPrepTime : null)
                                        .to(maxPrepTime != null ? (double) maxPrepTime : null)
                                )));
                            }

                            if (minCookTime != null || maxCookTime != null) {
                                b.filter(f -> f.range(rq -> rq.number(nrq -> nrq
                                        .field("cookTime")
                                        .from(minCookTime != null ? (double) minCookTime : null)
                                        .to(maxCookTime != null ? (double) maxCookTime : null)
                                )));
                            }

                            if (minReactionCount != null) {
                                b.filter(f -> f.range(rq -> rq.number(nrq -> nrq
                                        .field("totalReactionCount")
                                        .from((double) minReactionCount)
                                )));
                            }

                            if (minTotalShares != null) {
                                b.filter(f -> f.range(rq -> rq.number(nrq -> nrq
                                        .field("totalShares")
                                        .from((double) minTotalShares)
                                )));
                            }

                            return b;
                        }));

                        // Sorting
                        switch (sortBy.toLowerCase()) {
                            case "popular" ->
                                    search.sort(srt -> srt.field(f -> f.field("totalShares").order(SortOrder.Desc)));
                            case "toprated" ->
                                    search.sort(srt -> srt.field(f -> f.field("reactionCounts.LIKE").order(SortOrder.Desc)));
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

            return new PageImpl<>(ids, pageable, total);

        } catch (Exception e) {
            e.printStackTrace();
            return new PageImpl<>(List.of(), pageable, 0);
        }
    }
}

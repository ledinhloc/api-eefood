package com.eefood.reactionservice.service.chatbot.tools;

import com.eefood.reactionservice.dto.request.UserContext;
import com.eefood.reactionservice.dto.response.CollectionResponse;
import com.eefood.reactionservice.dto.response.PostResponse;
import com.eefood.reactionservice.dto.response.UserResponse;
import com.eefood.reactionservice.dto.response.chatbot.ChatbotResponse;
import com.eefood.reactionservice.model.chatbot.ChatMessage;
import com.eefood.reactionservice.repository.chatbot.ChatbotRepository;
import com.eefood.reactionservice.repository.httpclient.IamClient;
import com.eefood.reactionservice.service.chatbot.ChromaRagService;
import com.eefood.reactionservice.service.collection.CollectionService;
import com.eefood.reactionservice.service.follow.FollowService;
import com.eefood.reactionservice.service.post.PostScrollSearchService;
import com.eefood.reactionservice.util.SecurityUtil;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class ChatbotTools {
    private final ChatbotRepository chatbotRepository;
    private final PostScrollSearchService postScrollSearchService;
    private final ChromaRagService chromaRagService;
    private final CollectionService collectionService;
    private final IamClient iamClient;
    private final FollowService followService;
    private final SecurityUtil securityUtil;

    // Tool: Đề xuất bài viết
    @Tool("SUGGEST_POST")
    public ChatbotResponse suggestPost(
            @P("""
                Từ khóa chính của món ăn (1–3 từ).
                Lấy trực tiếp từ câu người dùng hoặc nhận diện từ ảnh.
                Ví dụ: phở, bún bò, gà rán.
                """) String keyword,
            @P("""
                Khu vực / địa điểm người dùng muốn ăn.
                Lấy từ địa điểm đã cung cấp ở đầu vào
                Không suy đoán.
                """) String location,
            @P("""
                Độ khó chế biến.
                Chỉ nhận: EASY | MEDIUM | HARD.
                Nếu người dùng không đề cập → null.
                """) String difficulty,
            @P("""
                Danh mục món ăn.
                Chỉ truyền khi người dùng hoặc ngữ cảnh có căn cứ rõ ràng.
                Nếu không chắc → truyền null hoặc [].
                """) List<String> category,
            @P("""
                Thời gian nấu tối đa (phút).
                Chỉ truyền nếu người dùng đề cập món nhanh, ít thời gian.
                """) Integer maxCookTime,
            @P("""
                Danh sách nguyên liệu người dùng đề cập.
                Phải trích xuất trực tiếp từ câu hỏi gốc của người dùng hoặc ảnh.
                Không tự thêm nguyên liệu.
                """) List<String> ingredient,
            @P("""
                Câu hỏi gốc của người dùng.
                Dùng để semantic search / RAG.
                KHÔNG tách, KHÔNG rút gọn.
                """)  String originalQuery,
            @P("""
                Id người dùng
                Giữ nguyên không thay đổi hoặc bịa
                """)  Long userId
    ) {

        log.info("Keyword: " + keyword);
        log.info("Difficult: " +difficulty);
        log.info("Location: " +location);
        log.info("Category: " + category);
        log.info("Max Cook: " + maxCookTime);
        log.info("Ingredient: " + ingredient);
        log.info("Query: " + originalQuery);

        try {
            UserContext ctx = loadUserContext(userId);

            List<Long> candidateIds = postScrollSearchService.searchAllPostIds(
                    keyword,
                    location,
                    difficulty,
                    category,
                    maxCookTime,
                    ctx.user(),
                    ctx.newFollowings(),
                    ctx.oldFollowings(),
                    10
            );

            log.info("CandidateIds size={}", candidateIds.size());

            log.info("Candidate ids:{}", candidateIds);

            if(candidateIds.isEmpty()) {
                return ChatbotResponse.builder()
                        .data(new ArrayList<>())
                        .meta(Map.of("total", 0, "tool", "SUGGEST_POST"))
                        .build();
            }
            List<PostResponse> posts = chromaRagService.retrieveTopKSimilarPosts(candidateIds, originalQuery, ingredient, 5);
            return ChatbotResponse.builder()
                    .data(new ArrayList<>(posts))
                    .meta(Map.of(
                            "total", posts.size(),
                            "tool", "SUGGEST_POST"
                    ))
                    .build();
        }
        catch(Exception e) {
            log.error("[TOOL] SUGGEST_POST called with exception", e);
            return ChatbotResponse.builder()
                    .data(new ArrayList<>())
                    .meta(Map.of("total", 0, "tool", "SUGGEST_POST"))
                    .build();

        }
    }

    // Tool: Tạo collection từ lich su gần nhất
    @Tool("GENERATE_COLLECTION")
    public ChatbotResponse generateCollection(Long userId, String collectionName) {

        log.info("[TOOL] GENERATE_COLLECTION called");

        // Lấy 5 message gần nhất có output data
        List<ChatMessage> recentChats =
                chatbotRepository.findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId);

        List<Long> postIds = recentChats.stream()
                .filter(c -> c.getOutputJson() != null)
                .flatMap(c -> c.getOutputJson()
                        .path("data")
                        .findValues("id")
                        .stream())
                .map(j -> j.asLong())
                .distinct()
                .limit(5)
                .toList();

        if (postIds.isEmpty()) {
            return ChatbotResponse.builder()
                    .data(List.of())
                    .meta(Map.of(
                            "total", 0,
                            "tool", "GENERATE_COLLECTION"
                    ))
                    .build();
        }


        CollectionResponse collectionResponse = collectionService.create(userId, collectionName);

        postIds.forEach(postId ->
                collectionService.addPost(collectionResponse.getId(), postId)
        );

        return ChatbotResponse.builder()
                .data(List.of(collectionResponse))
                .meta(Map.of(
                        "total", 1,
                        "tool", "GENERATE_COLLECTION"
                ))
                .build();
    }

    private UserContext loadUserContext(Long userId) {
        if (userId == null) {
            return UserContext.guest();
        }

        try {
            UserResponse user =
                    iamClient.getUserById(userId).getData();

            List<Long> newF = followService.getNewFollowings(userId);
            List<Long> oldF = followService.getOldFollowings(userId);

            return new UserContext(user, newF, oldF);
        } catch (Exception e) {
            log.info("Guest user or IAM failed");
            return UserContext.guest();
        }
    }

}

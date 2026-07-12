package com.eefood.reactionservice.service.chatbot.tools;

import com.eefood.reactionservice.dto.request.UserContext;
import com.eefood.reactionservice.dto.response.*;
import com.eefood.reactionservice.dto.response.chatbot.ChatbotResponse;
import com.eefood.reactionservice.enums.ChatRole;
import com.eefood.reactionservice.enums.ChatTool;
import com.eefood.reactionservice.enums.ErrorMessage;
import com.eefood.reactionservice.mealplan.dto.response.NutritionAnalysisResponse;
import com.eefood.reactionservice.repository.chatbot.ChatbotRepository;
import com.eefood.reactionservice.repository.httpclient.IamClient;
import com.eefood.reactionservice.repository.httpclient.RecipeClient;
import com.eefood.reactionservice.service.chatbot.ChatbotShoppingListService;
import com.eefood.reactionservice.service.chatbot.ChromaRagService;
import com.eefood.reactionservice.service.collection.CollectionService;
import com.eefood.reactionservice.service.follow.FollowService;
import com.eefood.reactionservice.service.post.PostScrollSearchService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

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
    private final ChatbotShoppingListService chatbotShoppingListService;
    private static final AtomicInteger atomicInteger = new AtomicInteger(1);
    private final RecipeClient recipeClient;

    @Tool("ANALYSTS_NUTRITION")
    public ChatbotResponse analyzeNutrition(
            @P("""
        Id công thức cần phân tích dinh dưỡng.
        Lấy từ THÔNG TIN BÀI VIẾT → ID CÔNG THỨC.
        Nếu người dùng không chỉ định cụ thể → lấy từ LỊCH SỬ AI gần nhất.
        KHÔNG tự bịa recipeId.
        """) Long recipeId,
            @P("""
        Buộc phân tích lại từ đầu, bỏ qua cache.
        Chỉ true khi user yêu cầu "phân tích lại", "cập nhật", "refresh".
        Mặc định: false.
        """) boolean forceRefresh
    ) {
        log.info("[TOOL] ANALYSTS_NUTRITION recipeId={} forceRefresh={}", recipeId, forceRefresh);

        try {
            if (recipeId == null) {
                return buildEmptyResponse(ChatTool.ANALYSTS_NUTRITION.name(), ChatRole.AI.name());
            }

            ResponseData<NutritionAnalysisResponse> response =
                    recipeClient.getNutritionByRecipeIdForChatbot(recipeId, forceRefresh);

            NutritionAnalysisResponse nutrition = response.getData();

            if (nutrition == null) {
                return buildEmptyResponse(ChatTool.ANALYSTS_NUTRITION.name(), ChatRole.AI.name());
            }

            return buildResponse(
                    List.of(nutrition),
                    ChatTool.ANALYSTS_NUTRITION.name(),
                    ChatRole.AI.name()
            );
        } catch (Exception e) {
            log.error("[TOOL] ANALYSTS_NUTRITION error recipeId={}", recipeId, e);
            return buildEmptyResponse(ChatTool.ANALYSTS_NUTRITION.name(), ChatRole.AI.name());
        }
    }

    // Tool: Đề xuất bài viết
    @Tool("SUGGEST_POST")
    public ChatbotResponse suggestPost(
            @P("""
                Từ khóa chính của món ăn (1–3 từ).
                Lấy trực tiếp từ câu người dùng (đã chuẩn hóa dấu) hoặc nhận diện từ ảnh.
                Ví dụ: phở, bún bò, gà rán.
                """) String keyword,
            @P("""
                Khu vực / địa điểm người dùng muốn ăn.
                Nếu người dùng không đề cập → null.
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
                Nếu có category hint nhưng người không không đề cập → không dùng category hint
                Nếu người dùng không đề cập → truyền null hoặc [].
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
        log.info("UserId: " + userId);

        try {
            UserContext ctx = loadUserContext(userId);

            log.info("UserContext: " + ctx.user().getId());

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
                return buildEmptyResponse(ChatTool.SUGGEST_POST.name(), ChatRole.AI.name());
            }
            List<PostResponse> posts = chromaRagService.retrieveTopKSimilarPosts(candidateIds, originalQuery, ingredient, 5);
            //List<PostResponse> posts = chromaRagService.retrieveTopKSimilarPosts(null, originalQuery, ingredient, 5);
            return buildResponse(posts,ChatTool.SUGGEST_POST.name(), ChatRole.AI.name());
        }
        catch(Exception e) {
            log.error("[TOOL] SUGGEST_POST called with exception", e);
            return buildEmptyResponse(ChatTool.SUGGEST_POST.name(), ChatRole.AI.name());
        }
    }

    // Tool: Tạo favourite collection
    @Tool("GENERATE_COLLECTION")
    public ChatbotResponse generateCollection(
    @P("""
        Id người dùng, lấy từ THÔNG TIN NGƯỜI DÙNG
        """)
        Long userId,
        @P("""
        Danh sách Id bài viết, lấy từ THÔNG TIN BÀI VIẾT:
        """)
        List<Long> listPostIds,
        @P("""
        Id danh mục yêu thích, lấy từ THÔNG TIN DANH SÁCH YÊU THÍCH
        """)
        String collectionName)
    {
        try {
            // Trường hợp chọn collection có sẵn
            CollectionResponse collectionResponse = collectionService.getCollectionByName(collectionName);
            if(collectionResponse!= null) {
                // Trường hợp chọn collection có sẵn và chỉ định lưu 1 post vào collection đó
                if(!listPostIds.isEmpty()) {
                    listPostIds.forEach(id -> collectionService.addPost(collectionResponse.getId(), id, userId));
                    return buildResponse(
                            List.of(collectionService.getByIdForChatbot(collectionResponse.getId(), userId)),
                            ChatTool.GENERATE_COLLECTION.name(),
                            ChatRole.AI.name()
                    );
                }
                // Trường hợp chọn collection có sẵn và không chỉ định => lấy tất cả lưu vào collection
                else {
                    List<Long> postIds = getRecentPostIds(userId);
                    if (postIds.isEmpty()) {
                        return buildEmptyResponse(ChatTool.GENERATE_COLLECTION.name(), ChatRole.AI.name());
                    }
                    postIds.forEach(id -> collectionService.addPost(collectionResponse.getId(), id, userId));
                    return buildResponse(
                            List.of(collectionService.getByIdForChatbot(collectionResponse.getId(), userId)),
                            ChatTool.GENERATE_COLLECTION.name(),
                            ChatRole.AI.name()
                    );
                }
            }
            // Trường hợp tạo collection mới
            else {
                String nameOfClt = generateCollectionName(collectionName);
                CollectionResponse newCollection = collectionService.create(userId, nameOfClt);
                // Trường hợp collection mới và chỉ định lưu 1 post vào collection đó
                if(!listPostIds.isEmpty()) {
                    listPostIds.forEach(id -> collectionService.addPost(newCollection.getId(), id, userId));
                    return buildResponse(
                            List.of(collectionService.getByIdForChatbot(newCollection.getId(), userId)),
                            ChatTool.GENERATE_COLLECTION.name(),
                            ChatRole.AI.name()
                    );
                }
                else {
                    List<Long> postIds = getRecentPostIds(userId);
                    if (postIds.isEmpty()) {
                        return buildEmptyResponse(ChatTool.GENERATE_COLLECTION.name(),ChatRole.AI.name());
                    }
                    postIds.forEach(id -> collectionService.addPost(newCollection.getId(), id, userId));
                    return buildResponse(
                            List.of(collectionService.getByIdForChatbot(newCollection.getId(), userId)),
                            ChatTool.GENERATE_COLLECTION.name(),
                            ChatRole.AI.name()
                    );
                }
            }
        }
        catch(Exception e) {
            if(e.getMessage().contains(ErrorMessage.DUPLICATE_COLLECTION_NAME.getMessage())) {
                return buildErrorResponse(ChatTool.GENERATE_COLLECTION.name(),ChatRole.AI.name(), "DUPLICATE_NAME");
            }
            if(e.getMessage().contains(ErrorMessage.ALREADY_EXISTS.getMessage())) {
                return buildErrorResponse(ChatTool.GENERATE_COLLECTION.name(),ChatRole.AI.name(), "ALREADY_EXISTS");
            }
            log.error("[TOOL] GENERATE_COLLECTION error", e);
            return buildEmptyResponse(ChatTool.GENERATE_COLLECTION.name(),ChatRole.AI.name());
        }

    }

    @Tool("GENERATE_SHOPPING_LIST")
    public ChatbotResponse generateShoppingList(
        @P("""
        Id người dùng, lấy từ THÔNG TIN NGƯỜI DÙNG
        """)
        Long userId,
        @P("""
        Danh sách Id công thức/món ăn, lấy từ ID CÔNG THỨC
        """)
        List<Long> recipeId
    )
    {
        try {
            List<ShoppingItemDto> results = new ArrayList<>();
            if(!recipeId.isEmpty()) {
                recipeId.forEach(id -> results.add(
                        chatbotShoppingListService.addItem(id, userId)
                ));
            }
            else {
                List<Long> recipeIds = getRecentRecipeIds(userId);

                if(recipeIds.isEmpty()) {
                    return buildEmptyResponse(
                            ChatTool.GENERATE_SHOPPING_LIST.name(),
                            ChatRole.AI.name()
                    );
                }

                results.addAll(
                        recipeIds.stream()
                                .map(id -> {
                                    try {
                                        return chatbotShoppingListService.addItem(id, userId);
                                    } catch (Exception ex) {
                                        log.warn("Skip recipeId={} due to error: {}", id, ex.getMessage());
                                        return null;
                                    }
                                })
                                .filter(Objects::nonNull)
                                .toList()
                );
            }

            return buildResponse(
                    results,
                    ChatTool.GENERATE_SHOPPING_LIST.name(),
                    ChatRole.AI.name()
            );
        }
        catch(Exception e) {
            log.error("[TOOL] GENERATE_SHOPPING_LIST called with exception", e);
            return buildEmptyResponse(
                    ChatTool.GENERATE_SHOPPING_LIST.name(),
                    ChatRole.AI.name()
            );
        }
        finally {
            SecurityContextHolder.clearContext();
        }
    }

    private String generateCollectionName(String userProvidedName) {
        if (userProvidedName != null && !userProvidedName.isBlank()) {
            return userProvidedName;
        }
        int index = atomicInteger.getAndIncrement();
        return "collection " + index;
    }

    private List<Long> getRecentPostIds(Long userId) {
        return chatbotRepository
                .findTop1ByUserIdAndRoleAndChatToolAndIsDeletedFalseOrderByCreatedAtDesc(
                        userId,
                        ChatRole.AI,
                        ChatTool.SUGGEST_POST
                )
                .map(c -> c.getData())                     // JsonNode
                .filter(data -> data != null && !data.isNull())
                .map(data -> data.path("data")             // JsonNode array
                        .findValues("id")
                        .stream()
                        .map(j -> j.asLong())
                        .distinct()
                        .limit(5)
                        .toList()
                )
                .orElse(List.of());
    }

    private List<Long> getRecentRecipeIds(Long userId) {
        return chatbotRepository
                .findTop1ByUserIdAndRoleAndChatToolAndIsDeletedFalseOrderByCreatedAtDesc(
                        userId,
                        ChatRole.AI,
                        ChatTool.SUGGEST_POST
                )
                .map(c -> c.getData())                     // JsonNode
                .filter(data -> data != null && !data.isNull())
                .map(data -> data.path("data")             // JsonNode array
                        .findValues("recipeId")
                        .stream()
                        .map(j -> j.asLong())
                        .distinct()
                        .limit(5)
                        .toList()
                )
                .orElse(List.of());
    }

    private <T> ChatbotResponse buildResponse(List<T> data, String tool, String role) {
        return ChatbotResponse.builder()
                .data(new ArrayList<>(data))
                .role(role)
                .meta(Map.of("total", data.size(), "tool", tool))
                .build();
    }

    private ChatbotResponse buildEmptyResponse(String tool, String role) {
        return buildResponse(List.of(), tool, role);
    }

    private ChatbotResponse buildErrorResponse(String tool, String role, String error) {
        return ChatbotResponse.builder()
                .data(List.of())
                .role(role)
                .meta(Map.of("error", error, "tool", tool))
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

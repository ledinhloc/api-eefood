package com.eefood.reactionservice.service.chatbot;

import com.eefood.reactionservice.dto.response.CategoryResponse;
import com.eefood.reactionservice.repository.httpclient.RecipeClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CategoryHintService {

    private final RecipeClient recipeClient;

    // Mẫu
    private static final Map<Pattern, List<String>> TEXT_PATTERNS = Map.ofEntries(
            Map.entry(Pattern.compile("món chính|chính|cơm|bữa chính", Pattern.CASE_INSENSITIVE),
                    List.of("Món chính")),
            Map.entry(Pattern.compile("món nước|nước|soup|canh|phở|bún|mì|hủ tiếu", Pattern.CASE_INSENSITIVE),
                    List.of("Món nước")),
            Map.entry(Pattern.compile("món ăn sáng|ăn sáng|sáng|breakfast", Pattern.CASE_INSENSITIVE),
                    List.of("Món ăn sáng")),
            Map.entry(Pattern.compile("đường phố|street food|vỉa hè", Pattern.CASE_INSENSITIVE),
                    List.of("Món đường phố")),
            Map.entry(Pattern.compile("tráng miệng|dessert|ngọt", Pattern.CASE_INSENSITIVE),
                    List.of("Món tráng miệng")),
            Map.entry(Pattern.compile("món cuốn|cuốn|nem cuốn|gỏi cuốn", Pattern.CASE_INSENSITIVE),
                    List.of("Món cuốn")),
            Map.entry(Pattern.compile("tết|mùng|năm mới|đầu năm|xuân", Pattern.CASE_INSENSITIVE),
                    List.of("Món tết")),
            Map.entry(Pattern.compile("món chay|chay|thuần chay|vegetarian", Pattern.CASE_INSENSITIVE),
                    List.of("Món chay")),
            Map.entry(Pattern.compile("bánh|cake|bread", Pattern.CASE_INSENSITIVE),
                    List.of("Các loại bánh")),
            Map.entry(Pattern.compile("ăn vặt|snack|quà|vặt", Pattern.CASE_INSENSITIVE),
                    List.of("Quà - Món ăn vặt")),
            Map.entry(Pattern.compile("giải nhiệt|giải khát|mát|làm mát|uống|đá|nóng|nực|smoothie", Pattern.CASE_INSENSITIVE),
                    List.of("Món tráng miệng, giải khát", "Ngày nắng nóng")),
            Map.entry(Pattern.compile("ấm|nóng hổi|ấm áp", Pattern.CASE_INSENSITIVE),
                    List.of("Món ngon ngày lạnh", "Món nước"))
    );

    // Thời tiết
    private static final Map<String, List<String>> WEATHER_MAPPING = Map.of(
            "HOT", List.of("Ngày nắng nóng", "Món tráng miệng, giải khát"),
            "COLD", List.of("Món ngon ngày lạnh", "Món nước"),
            "RAINY", List.of("Món ngon ngày lạnh", "Món nước"),
            "COOL", List.of("Món ngon cho cuối tuần"),
            "HUMID", List.of("Món ngon hàng ngày", "Món đường phố")
    );

    // Thời gian
    private static final Map<String, List<String>> TIME_MAPPING = Map.of(
            "BREAKFAST", List.of("Món ăn sáng", "Bữa sáng đơn giản"),
            "MORNING", List.of("Món ăn sáng", "Bữa sáng đơn giản"),
            "LUNCH", List.of("Món chính", "Thực đơn hàng ngày"),
            "AFTERNOON", List.of("Quà - Món ăn vặt"),
            "DINNER", List.of("Món chính", "Thực đơn hàng ngày")
    );

    public String generateCategoryHint(String userText, String timePresent, String weather) {
        Set<String> categoryHints = new LinkedHashSet<>();

        // Ưu tiên 1: Từ yêu cầu người dùng
        if (userText != null && !userText.trim().isEmpty()) {
            categoryHints.addAll(extractCategoriesFromText(userText));
        }

        // Ưu tiên 2: Thời tiết
        if (weather != null && !weather.trim().isEmpty()) {
            categoryHints.addAll(extractCategoriesFromWeather(weather, userText));
        }

        // Ưu tiên 3: Thời gian
        if (timePresent != null && !timePresent.trim().isEmpty()) {
            categoryHints.addAll(extractCategoriesFromTime(timePresent, userText));
        }

        // Kiểm tra tồn tại trong category
        List<String> validatedCategories = validateCategories(categoryHints);

        // Return formatted hint or empty string
        return formatCategoryHint(validatedCategories);
    }

    //Trả về category từ yêu cầu người dùng
    private List<String> extractCategoriesFromText(String text) {
        List<String> categories = new ArrayList<>();

        for (Map.Entry<Pattern, List<String>> entry : TEXT_PATTERNS.entrySet()) {
            if (entry.getKey().matcher(text).find()) {
                categories.addAll(entry.getValue());
            }
        }

        return categories;
    }

    private List<String> extractCategoriesFromWeather(String weather, String userText) {
        List<String> categories = new ArrayList<>();

        if (!isWeatherMentioned(userText)) {
            return categories;
        }

        String normalizedWeather = normalizeWeather(weather);

        if (WEATHER_MAPPING.containsKey(normalizedWeather)) {
            categories.addAll(WEATHER_MAPPING.get(normalizedWeather));
        }

        return categories;
    }

    private List<String> extractCategoriesFromTime(String timePresent, String userText) {
        List<String> categories = new ArrayList<>();

        if (!isTimeMentioned(userText)) {
            return categories;
        }

        String normalizedTime = normalizeTime(timePresent);

        if (TIME_MAPPING.containsKey(normalizedTime)) {
            categories.addAll(TIME_MAPPING.get(normalizedTime));
        }

        return categories;
    }

    private boolean isWeatherMentioned(String text) {
        if (text == null) return false;

        Pattern weatherPattern = Pattern.compile(
                "nắng|nóng|lạnh|mưa|mát|oi|ẩm|se lạnh|ấm|nhiệt độ|°c|độ c|thời tiết|weather",
                Pattern.CASE_INSENSITIVE
        );

        return weatherPattern.matcher(text).find();
    }

    private boolean isTimeMentioned(String text) {
        if (text == null) return false;

        Pattern timePattern = Pattern.compile(
                "sáng|trưa|chiều|tối|buổi|bữa|breakfast|lunch|dinner|morning|afternoon|evening",
                Pattern.CASE_INSENSITIVE
        );

        return timePattern.matcher(text).find();
    }

    private String normalizeWeather(String weather) {
        if (weather == null) return "";

        String lower = weather.toLowerCase();

        // Kiểm tra chuỗi chứa số
        if (lower.matches(".*\\d+.*")) {
            int temp = extractTemperature(lower);
            if (temp > 30) return "HOT";
            if (temp < 20) return "COLD";
        }

        // Check weather conditions
        if (lower.matches(".*(nắng|nóng|oi|hot|sunny).*")) return "HOT";
        if (lower.matches(".*(lạnh|cold|se lạnh).*")) return "COLD";
        if (lower.matches(".*(mưa|rain).*")) return "RAINY";
        if (lower.matches(".*(mát|cool|dễ chịu).*")) return "COOL";
        if (lower.matches(".*(ẩm|humid).*")) return "HUMID";

        return "";
    }

    // Lấy số nhiệt độ
    private int extractTemperature(String weather) {
        try {
            return Integer.parseInt(weather.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 25;
        }
    }

    private String normalizeTime(String time) {
        if (time == null) return "";

        String lower = time.toLowerCase();

        // Check for time keywords
        if (lower.matches(".*(breakfast|sáng|buổi sáng|morning).*")) return "BREAKFAST";
        if (lower.matches(".*(lunch|trưa|buổi trưa).*")) return "LUNCH";
        if (lower.matches(".*(afternoon|chiều|xế chiều).*")) return "AFTERNOON";
        if (lower.matches(".*(dinner|tối|buổi tối|evening).*")) return "DINNER";

        try {
            LocalTime localTime = LocalTime.parse(time);
            int hour = localTime.getHour();

            if (hour >= 5 && hour < 10) return "BREAKFAST";
            if (hour >= 10 && hour < 14) return "LUNCH";
            if (hour >= 14 && hour < 17) return "AFTERNOON";
            if (hour >= 17 || hour < 5) return "DINNER";
        } catch (Exception e) {
            log.error("Error parsing time: " + time, e);
        }

        return "";
    }

    private List<String> validateCategories(Set<String> categories) {
        try {
            List<CategoryResponse> dbCategories = recipeClient.getListOfCategories()
                    .getData();

            log.info("DB Categories: {}", dbCategories.size());

            Set<String> validCategoryNames = dbCategories.stream()
                    .map(CategoryResponse::getDescription)
                    .collect(Collectors.toSet());

            return categories.stream()
                    .filter(validCategoryNames::contains)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error validating categories", e);
            return new ArrayList<>(categories);
        }
    }

    private String formatCategoryHint(List<String> categories) {
        if (categories.isEmpty()) {
            return "";
        }
        log.info("List categories hint: " + String.join(", ", categories));
        return "Gợi ý category phù hợp: " + String.join(", ", categories);
    }
}

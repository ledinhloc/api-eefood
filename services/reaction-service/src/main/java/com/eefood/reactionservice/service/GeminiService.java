package com.eefood.reactionservice.service;

import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;


@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiService {

    private final GoogleAiGeminiChatModel geminiModel;

    @Cacheable(value = "gemini-keyword-cache", key = "T(org.apache.commons.codec.digest.DigestUtils).md5Hex(#imageFile.bytes)")
    public String extractKeywordsFromImage(MultipartFile imageFile) {
        return extractKeywordsFromImageInternal(imageFile);
    }

    // Hàm xử lý logic phân tích ảnh
    private String extractKeywordsFromImageInternal(MultipartFile imageFile) {
        try {
            byte[] resizedBytes = resizeImageIfNeeded(imageFile);
            String base64Image = Base64.getEncoder().withoutPadding().encodeToString(resizedBytes);

            String prompt = """
                    Bạn là chuyên gia nhận diện món ăn Việt Nam.
                    Phân tích hình ảnh và trả về chính xác tên món ăn hoặc nguyên liệu chính trong ảnh, bằng tiếng Việt.
                    Chỉ trả về một cụm từ ngắn, không giải thích.
                    Nếu không chắc chắn, trả về unknown.
                    """;

            UserMessage userMessage = UserMessage.from(
                    TextContent.from(prompt),
                    ImageContent.from(base64Image, imageFile.getContentType())
            );

            ChatRequest chatRequest = ChatRequest.builder()
                    .messages(userMessage)
                    .build();

            long start = System.currentTimeMillis();
            ChatResponse chatResponse = geminiModel.chat(chatRequest);
            long duration = System.currentTimeMillis() - start;

            String result = chatResponse.aiMessage().text();
            log.info("Gemini response ({} ms): {}", duration, result);

            return cleanAiResponse(result);

        } catch (Exception e) {
            log.error("Lỗi khi phân tích ảnh bằng Gemini: {}", e.getMessage(), e);
            return null;
        }
    }

    // Resize ảnh nếu >512px để giảm độ trễ xử lý
    private byte[] resizeImageIfNeeded(MultipartFile imageFile) {
        try {
            BufferedImage originalImage = ImageIO.read(imageFile.getInputStream());
            if (originalImage == null) return imageFile.getBytes();

            int maxSize = 512;
            int width = originalImage.getWidth();
            int height = originalImage.getHeight();

            if (width <= maxSize && height <= maxSize) {
                return imageFile.getBytes();
            }

            double scale = Math.min((double) maxSize / width, (double) maxSize / height);
            int newWidth = (int) (width * scale);
            int newHeight = (int) (height * scale);

            BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
            resizedImage.getGraphics().drawImage(originalImage, 0, 0, newWidth, newHeight, null);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
            ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
            writer.setOutput(ios);
            ImageWriteParam params = writer.getDefaultWriteParam();
            params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            params.setCompressionQuality(0.7f);
            writer.write(null, new IIOImage(resizedImage, null, null), params);
            ios.close();
            writer.dispose();

            return baos.toByteArray();
        } catch (Exception e) {
            log.warn("Không thể resize ảnh, dùng ảnh gốc. {}", e.getMessage());
            try {
                return imageFile.getBytes();
            } catch (Exception ex) {
                return new byte[0];
            }
        }
    }

    //Làm sạch kết quả AI
    private String cleanAiResponse(String text) {
        if (text == null || text.isBlank() || text.equalsIgnoreCase("unknown")) return null;

        String cleaned = text
                .trim()
                .replaceAll("\\r|\\n", " ")
                .replaceAll("\"", "")
                .replaceAll("[\\[\\]\\(\\)]", "")
                .replaceAll("(?i)(món ăn|nguyên liệu|đây là|có thể là|là món|tôi nghĩ là|chắc là)[:]?\\s*", "")
                .replaceAll("\\s{2,}", " ")
                .trim();

        if (!cleaned.isEmpty()) {
            cleaned = cleaned.substring(0, 1).toUpperCase() + cleaned.substring(1);
        }
        cleaned = cleaned.replaceAll("\\.$", "");

        return cleaned;
    }
}

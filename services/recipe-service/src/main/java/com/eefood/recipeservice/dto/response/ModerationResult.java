package com.eefood.recipeservice.dto.response;

import com.eefood.recipeservice.enums.ModerationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModerationResult {
  private ModerationStatus status;
  private String summary;
  private Double totalScore;

  // Từng tiêu chí với điểm từ 0-10
  private Integer recipeCompleteness;      // Công thức đầy đủ
  private Integer ingredientSafety;        // Nguyên liệu an toàn
  private Integer stepClarity;             // Bước làm rõ ràng
  private Integer contentAppropriate;      // Nội dung phù hợp
  private Integer contentRelevance;        // Nội dung liên quan
  private Integer mediaQuality;            // Chất lượng hình ảnh/video

  // Ghi chú cho từng tiêu chí
  private String completenessNote;
  private String safetyNote;
  private String clarityNote;
  private String appropriatenessNote;
  private String relevanceNote;
  private String mediaQualityNote;
}

package com.eefood.recipeservice.util;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UnitUtils {
    public static double toGrams(Double quantity, String unit) {
        if (quantity == null || quantity <= 0) return 0;
        if (unit == null) return quantity;

        return switch (unit.trim().toLowerCase()) {

            //  Khối lượng
            case "g", "gram", "gam"             -> quantity;
            case "kg"                            -> quantity * 1000;
            case "lạng"                          -> quantity * 37.5;

            //  Thể tích
            case "ml", "cc"                     -> quantity;
            case "l", "lít"                     -> quantity * 1000;

            //  Thìa / muỗng
            case "muỗng canh", "muống canh",
                 "thìa canh", "tbsp"            -> quantity * 15;
            case "muỗng cà phê", "muống cà phê",
                 "thìa cà phê", "tsp"           -> quantity * 5;
            case "muỗng", "muống", "thìa"       -> quantity * 5;   // mặc định = cà phê

            //  Bát / chén / tô / cốc / cup
            case "cup", "cốc", "ly"             -> quantity * 240;
            case "chén"                          -> quantity * 150;
            case "bát"                           -> quantity * 200;
            case "tô"                            -> quantity * 400;

            //  Đơn vị đếm (không quy đổi chuẩn xác)
            case "quả", "trái", "củ", "con",
                 "cái", "chiếc", "viên",
                 "miếng", "lát", "khúc",
                 "thanh", "thỏi", "tảng",
                 "mẩu", "đốt", "nụ", "búp",
                 "bông", "hoa", "hạt",
                 "móng", "đùi", "lườn", "ức",
                 "cánh", "gói", "túi", "hộp",
                 "lon", "chai", "lọ", "hũ",
                 "bìa", "tờ", "xấp", "tập",
                 "khuôn", "vắt", "ổ",
                 "bắp", "cùi", "cây", "cọng",
                 "nhánh", "lá", "bẹ", "sả",
                 "đầu", "tép", "tỏi", "bó",
                 "mớ", "nắm", "nhúm", "nấm",
                 "lọn", "que", "bộ", "cặp",
                 "tai", "thảo", "lạp",
                 "chút", "ít", "một ít",
                 "đế", "ống", "thịt",
                 "gà", "hoặc"                   -> {
                log.warn("[UnitConverter] Countable unit '{}' — cannot convert accurately, keeping quantity as-is", unit);
                yield quantity;
            }

            default -> {
                log.warn("[UnitConverter] Unknown unit '{}', treating as grams", unit);
                yield quantity;
            }
        };
    }
}

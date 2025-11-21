import re

def parse_ingredient(text: str):
    """
    Parse ingredient text với hỗ trợ:
    - Số nguyên: "2 quả"
    - Số thập phân: "1.5 thìa"
    - Phân số: "1/2 củ hành"
    - Range: "2-3 tô", "1,5 - 1,7 kg"
    - Ghi chú: "(tùy chọn)", "(tuỳ ý)"
    - Dấu ":" phân cách: "Tôm tươi: 200 gr"
    - Đơn vị phức hợp: "5-6 thìa canh mẻ"
    - Dấu "," trong số: "1,5 kg" → 1.5
    - Trường hợp không có số: "Tỏi, sả bằm nhuyễn"
    
    Ví dụ:
    - "Cơm nguội: 1 tô" → qty=1, unit="tô", name="Cơm nguội"
    - "Tôm tươi: 200 gr" → qty=200, unit="g", name="Tôm tươi"
    - "1,5 - 1,7 kg thịt vịt" → qty=1.6, unit="kg", name="thịt vịt"
    - "5-6 thìa canh mẻ" → qty=5.5, unit="thìa canh", name="mẻ"
    - "1/2 củ hành tây (tùy chọn)" → qty=0.5, unit="củ", name="hành tây"
    - "1 quả dừa tươi để lấy nước dừa" → qty=1, unit="quả", name="dừa tươi để lấy nước dừa"
    - "Tỏi, sả bằm nhuyễn" → qty=1, unit="", name="Tỏi, sả bằm nhuyễn"
    """
    text = text.strip()
    
    # ===== TÁCH NAME VÀ PHẦN SỐ LƯỢNG NẾU CÓ DẤU ":" =====
    if ':' in text:
        parts = text.split(':', 1)
        name_part = parts[0].strip()
        quantity_part = parts[1].strip()
    else:
        name_part = None
        quantity_part = text
    
    # ===== LOẠI BỎ GHI CHÚ TRONG NGOẶC =====
    # quantity_part_clean = re.sub(r'\s*\([^)]*\)\s*', ' ', quantity_part).strip()
    
    # ===== PATTERN CẢI TIẾN =====
    # Pattern mới xử lý:
    # - Số đầu (có thể có dấu phẩy hoặc phân số): [\d,\.]+(?:/\d+)?
    # - Dấu gạch ngang (tùy chọn): \s*-\s*
    # - Số thứ 2 (tùy chọn): ([\d,\.]+(?:/\d+)?)?
    # - Đơn vị đặc biệt "thìa canh" hoặc "thìa cà phê": (thìa\s+(?:canh|cà\s+phê)|muỗng\s+(?:canh|cà\s+phê))?
    # - Đơn vị thường (1 từ): ([a-zA-ZÀ-ỹ]+)?
    # - Phần còn lại là tên nguyên liệu
    
    # Pattern ưu tiên bắt "thìa canh/cà phê" trước, sau đó mới đến đơn vị 1 từ
    pattern = r"^([\d,\.]+(?:/\d+)?)\s*-?\s*([\d,\.]+(?:/\d+)?)?\s*(?:(thìa\s+canh|thìa\s+cà\s+phê|muỗng\s+canh|muỗng\s+cà\s+phê)|([a-zA-ZÀ-ỹ]+))?\s*(.*?)$"
    
    match = re.match(pattern, quantity_part)
    
    if match:
        qty1_str = match.group(1).replace(',', '.')  # Chuyển "1,5" → "1.5"
        qty2_str = match.group(2).replace(',', '.') if match.group(2) else None
        unit_multi = match.group(3)  # Đơn vị nhiều từ (thìa canh, thìa cà phê)
        unit_single = match.group(4)  # Đơn vị 1 từ
        name_from_qty = match.group(5).strip()
        
        # Chọn đơn vị (ưu tiên đơn vị nhiều từ)
        if unit_multi:
            unit = unit_multi.strip().lower()
        elif unit_single:
            unit = unit_single.strip().lower()
        else:
            unit = ""
        
        # ===== XÁC ĐỊNH TÊN NGUYÊN LIỆU =====
        if name_part:
            name = name_part
        else:
            name = name_from_qty
        
        # Bỏ dấu "." cuối cùng trong name nếu có
        name = name.rstrip('.')
        
        # ===== PARSE SỐ LƯỢNG ĐẦU TIÊN =====
        try:
            if "/" in qty1_str:
                numerator, denominator = qty1_str.split("/")
                qty1 = float(numerator) / float(denominator)
            else:
                qty1 = float(qty1_str)
        except:
            qty1 = 0
        
        # ===== PARSE SỐ LƯỢNG THỨ 2 (NẾU CÓ) =====
        qty2 = qty1
        if qty2_str:
            try:
                qty2_str = qty2_str.replace(',', '.')
                if "/" in qty2_str:
                    numerator, denominator = qty2_str.split("/")
                    qty2 = float(numerator) / float(denominator)
                else:
                    qty2 = float(qty2_str)
            except:
                qty2 = qty1
        
        # ===== TÍNH TRUNG BÌNH NẾU LÀ RANGE =====
        qty = (qty1 + qty2) / 2
        
        # ===== MAPPING ĐƠN VỊ =====
        unit_map = {
            "gr": "g",
            "gram": "g",
            "grams": "g",
            "kg": "kg",
            "ml": "ml",
            "l": "l",
            "thìa": "thìa",
            "thìa canh": "thìa canh",
            "thìa cà phê": "thìa cà phê",
            "muỗng canh": "thìa canh",
            "muỗng cà phê": "thìa cà phê",
            "cái": "cái",
            "quả": "quả",
            "củ": "củ",
            "bó": "bó",
            "tô": "tô",
            "chén": "chén",
            "nắm": "nắm",
            "lát": "lát",
            "cây": "cây",
            "nhánh": "nhánh",
            "tép": "tép",
        }
        
        unit = unit_map.get(unit, unit)
        
        # ===== LÀM TRÒN QUANTITY VỀ INT NẾU CÓ PHẦN THẬP PHÂN LÀ 0 =====
        if qty == int(qty):
            qty = int(qty)
        else:
            qty = round(qty, 2)
        
        return {
            "name": name,
            "qty": qty,
            "unit": unit
        }
    
    # ===== TRƯỜNG HỢP KHÔNG CÓ SỐ (VÍ DỤ: "Tỏi, sả bằm nhuyễn") =====
    if name_part:
        # Nếu có dấu ":", lấy phần trước dấu ":"
        name = name_part
    else:
        # Không có số và không có dấu ":", lấy toàn bộ text
        name = quantity_part
    
    return {"name": name.rstrip('.'), "qty": 1, "unit": ""}


# ===== TEST CASES =====
if __name__ == "__main__":
    test_cases = [
        "Rượu trắng: nửa chén",
        "1/5 - 1/7 kg",
        "Phần thịt:",
        "a) Phần thịt và gia vị ướp nướng:"
    ]
    # "1,5 - 1,7 kg thịt vịt",
    # "1 quả dừa tươi để lấy nước dừa",
    # "8-10 quả sấu (tùy theo khẩu vị chua mỗi người)",
    # "Tỏi, sả bằm nhuyễn",
    # "Cơm nguội: 1 tô",
    # "Tôm tươi: 200 gr",
    # "Cà rốt: 1/2 củ",
    # "5-6 thìa canh mẻ",
    # "1/2 củ hành tây (tùy chọn)",
    # "2 thìa cà phê muối",
    
    print("=" * 80)
    print("KẾT QUẢ PARSE NGUYÊN LIỆU")
    print("=" * 80)
    
    for test in test_cases:
        result = parse_ingredient(test)
        print(f"\nInput:  {test}")
        print(f"Output: name='{result['name']}', qty={result['qty']}, unit='{result['unit']}'")
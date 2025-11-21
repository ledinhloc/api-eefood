import re

def split_ingredients(text: str):
    """
    Tách một chuỗi chứa nhiều nguyên liệu thành list các nguyên liệu riêng biệt.
    
    Logic:
    1. Tách theo dấu phẩy ","
    2. Nhưng KHÔNG tách nếu dấu phẩy nằm trong:
       - Số thập phân: "1,5 kg"
       - Khoảng range: "1,5 - 1,7 kg"
       - Trong ngoặc đơn: "(1,5 kg)"
    
    Ví dụ:
    - "3 củ tỏi, 1 củ gừng, hành tây" 
      → ["3 củ tỏi", "1 củ gừng", "hành tây"]
    
    - "1,5 - 1,7 kg thịt, 2 củ hành, muối"
      → ["1,5 - 1,7 kg thịt", "2 củ hành", "muối"]
    
    - "Tỏi (1,5 củ), gừng, hành"
      → ["Tỏi (1,5 củ)", "gừng", "hành"]
    """
    text = text.strip()
    
    # ===== STRATEGY: Tách theo dấu phẩy AN TOÀN =====
    # Pattern để tìm dấu phẩy KHÔNG nằm trong:
    # - Số: \d,\d (1,5)
    # - Ngoặc đơn: (...)
    
    result = []
    current = ""
    paren_depth = 0  # Đếm số ngoặc đơn lồng nhau
    i = 0
    
    while i < len(text):
        char = text[i]
        
        # Track ngoặc đơn
        if char == '(':
            paren_depth += 1
            current += char
        elif char == ')':
            paren_depth -= 1
            current += char
        
        # Gặp dấu phẩy
        elif char == ',':
            # Kiểm tra xem có phải dấu phẩy trong số không (1,5)
            is_decimal = (
                i > 0 and i < len(text) - 1 and
                text[i-1].isdigit() and text[i+1].isdigit()
            )
            
            # Nếu KHÔNG phải số thập phân VÀ KHÔNG trong ngoặc → Tách!
            if not is_decimal and paren_depth == 0:
                if current.strip():
                    result.append(current.strip())
                current = ""
            else:
                # Giữ dấu phẩy
                current += char
        
        else:
            current += char
        
        i += 1
    
    # Thêm phần cuối cùng
    if current.strip():
        result.append(current.strip())
    
    return result


def parse_ingredient(text: str):
    """
    Parse một nguyên liệu đơn lẻ.
    (Giữ nguyên code parse_ingredient như trước)
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
    
    # ===== PATTERN =====
    pattern = r"^([\d,\.]+(?:/\d+)?)\s*-?\s*([\d,\.]+(?:/\d+)?)?\s*(?:(thìa\s+canh|thìa\s+cà\s+phê|muỗng\s+canh|muỗng\s+cà\s+phê)|([a-zA-ZÀ-ỹ]+))?\s*(.*?)$"
    
    match = re.match(pattern, quantity_part)
    
    if match:
        qty1_str = match.group(1).replace(',', '.')
        qty2_str = match.group(2).replace(',', '.') if match.group(2) else None
        unit_multi = match.group(3)
        unit_single = match.group(4)
        name_from_qty = match.group(5).strip()
        
        if unit_multi:
            unit = unit_multi.strip().lower()
        elif unit_single:
            unit = unit_single.strip().lower()
        else:
            unit = ""
        
        if name_part:
            name = name_part
        else:
            name = name_from_qty
        
        name = name.rstrip('.')
        
        try:
            if "/" in qty1_str:
                numerator, denominator = qty1_str.split("/")
                qty1 = float(numerator) / float(denominator)
            else:
                qty1 = float(qty1_str)
        except:
            qty1 = 0
        
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
        
        qty = (qty1 + qty2) / 2
        
        unit_map = {
            "gr": "g", "gram": "g", "grams": "g",
            "kg": "kg", "ml": "ml", "l": "l",
            "thìa": "thìa", "thìa canh": "thìa canh", 
            "thìa cà phê": "thìa cà phê",
            "muỗng canh": "thìa canh", "muỗng cà phê": "thìa cà phê",
            "cái": "cái", "quả": "quả", "củ": "củ", 
            "bó": "bó", "tô": "tô", "chén": "chén",
            "nắm": "nắm", "lát": "lát", "cây": "cây",
            "nhánh": "nhánh", "tép": "tép",
        }
        
        unit = unit_map.get(unit, unit)
        
        if qty == int(qty):
            qty = int(qty)
        else:
            qty = round(qty, 2)
        
        return {
            "name": name,
            "qty": qty,
            "unit": unit
        }
    
    # Trường hợp không có số
    if name_part:
        name = name_part
    else:
        name = quantity_part
    
    return {"name": name.rstrip('.'), "qty": 1, "unit": ""}


def parse_multiple_ingredients(text: str):
    """
    Parse một chuỗi có thể chứa nhiều nguyên liệu.
    
    Returns:
        List[dict]: Danh sách các nguyên liệu đã parse
    """
    # Bước 1: Tách thành nhiều nguyên liệu
    ingredient_texts = split_ingredients(text)
    
    # Bước 2: Parse từng nguyên liệu
    results = []
    for ing_text in ingredient_texts:
        parsed = parse_ingredient(ing_text)
        results.append(parsed)
    
    return results

# ===== TEST CASES =====
if __name__ == "__main__":
    test_cases = [
        "3 củ tỏi, 1 củ gừng, đầu hành baro (tỏi tây), hành khô, hạt tiêu",
        "1,5 - 1,7 kg thịt vịt, 2 củ hành, muối, tiêu",
        "Tỏi (1,5 củ), gừng (2 củ), hành tây",
        "200 gr tôm, 1/2 củ hành, sả, tỏi",
        "Chanh tươi: 1/2 quả",
        "1 quả dừa tươi, 2-3 thìa canh đường, muối vừa đủ",
    ]
    
    print("=" * 80)
    print("TEST SPLIT VÀ PARSE MULTIPLE INGREDIENTS")
    print("=" * 80)
    
    for test in test_cases:
        print(f"\n📝 Input: {test}")
        print("-" * 80)
        
        results = parse_multiple_ingredients(test)
        
        for idx, result in enumerate(results, 1):
            print(f"  {idx}. name='{result['name']}' | qty={result['qty']} | unit='{result['unit']}'")
    
    print("\n" + "=" * 80)
    
    # ===== TEST SPLIT FUNCTION RIÊNG =====
    print("\n🔍 TEST SPLIT FUNCTION:")
    print("=" * 80)
    
    test_split = "3 củ tỏi, 1,5 kg thịt, hành (1,2 củ), muối"
    print(f"Input: {test_split}")
    print(f"Output: {split_ingredients(test_split)}")
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.chrome.service import Service
from selenium.webdriver.chrome.options import Options
from webdriver_manager.chrome import ChromeDriverManager

from bs4 import BeautifulSoup
import json
import re
import os
import logging
import time
import requests
from enum import Enum

logging.basicConfig(level=logging.INFO, format="%(asctime)s - %(levelname)s - %(message)s")
logger = logging.getLogger(__name__)


class Difficulty(Enum):
    EASY = "EASY"
    MEDIUM = "MEDIUM"
    HARD = "HARD"


class VnExpressRecipeScraper:
    def __init__(self, use_selenium=False):
        """
        use_selenium=False: Dùng requests (nhanh, không cần Chrome)
        use_selenium=True: Dùng Selenium (chậm, cần khi trang load bằng JS)
        """
        self.use_selenium = use_selenium
        self.driver = None
        
        if use_selenium:
            options = Options()
            options.add_argument("--headless=new")
            options.add_argument("--disable-gpu")
            options.add_argument("--no-sandbox")
            options.add_argument("--disable-dev-shm-usage")
            options.add_argument("--start-maximized")
            options.add_argument("--window-size=1920,1080")
            options.add_argument("--disable-blink-features=AutomationControlled")
            options.add_argument(
                "user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                "AppleWebKit/537.36 (KHTML, like Gecko) "
                "Chrome/120.0.0.0 Safari/537.36"
            )
            
            prefs = {
                "profile.managed_default_content_settings.images": 2,
            }
            options.add_experimental_option("prefs", prefs)

            self.driver = webdriver.Chrome(
                service=Service(ChromeDriverManager().install()),
                options=options
            )
            self.driver.set_page_load_timeout(5)
            self.driver.set_script_timeout(5)

    def scrape_recipe(self, url: str, category: str = "N/A"):
        """
        Scrape với tùy chọn Selenium hoặc Requests
        ✅ Thêm tham số category
        """
        if self.use_selenium:
            soup = self._load_with_selenium(url)
        else:
            soup = self._load_with_requests(url)
        
        if not soup:
            return None

        recipe = self._parse_recipe(soup, category)  # ✅ Truyền category vào parse
        return recipe

    def _load_with_requests(self, url: str):
        """NHANH: Dùng requests (không cần Chrome)"""
        try:
            headers = {
                "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            }
            response = requests.get(url, headers=headers, timeout=10)
            response.raise_for_status()
            return BeautifulSoup(response.content, "html.parser")
        except Exception as e:
            logger.error(f"❌ Requests failed: {e}. Falling back to Selenium...")
            self.use_selenium = True
            self.__init__(use_selenium=True)
            return self._load_with_selenium(url)

    def _load_with_selenium(self, url: str):
        """CHẬM: Dùng Selenium (khi requests không được)"""
        try:
            self.driver.get(url)
            
            try:
                WebDriverWait(self.driver, 3).until(
                    EC.presence_of_element_located((By.CLASS_NAME, "title-detail"))
                )
            except:
                logger.warning("⏱ Page load timeout, continuing anyway...")
            
            return BeautifulSoup(self.driver.page_source, "html.parser")
        except Exception as e:
            logger.error(f"❌ Selenium failed: {e}")
            return None

    def _parse_recipe(self, soup, category: str = "N/A"):
        """
        Parse recipe từ soup
        ✅ Thêm tham số category
        """
        recipe = {
            "title": "",
            "description": "",
            "region": "Việt Nam",
            "imageUrl": "",
            "videoUrl": "",
            "categories": [category] if category != "N/A" else [],  # ✅ Thêm vào categories
            "prepTime": 0,
            "cookTime": 0,
            "difficulty": "MEDIUM",
            "ingredients": [],
            "steps": []
        }

        # ===== TITLE =====
        title_elem = soup.find("h1", class_="title-detail")
        if title_elem:
            recipe["title"] = title_elem.text.strip()

        # ===== DESCRIPTION =====
        desc_elem = soup.find("p", class_="description")
        if desc_elem:
            recipe["description"] = desc_elem.text.strip()

        # ===== IMAGE =====
        try:
            col3 = soup.find("div", class_="col-3")
            if col3:
                img = col3.find("img")
                if img and img.get("src"):
                    recipe["imageUrl"] = img["src"]
        except:
            pass

        # ===== TIME =====
        try:
            status_div = soup.find("div", class_="author-flex")
            if status_div:
                spans = status_div.find_all("span")
                for s in spans:
                    if "phút" in s.text:
                        total = int(re.search(r"\d+", s.text).group())
                        recipe["prepTime"] = total // 2
                        recipe["cookTime"] = total - recipe["prepTime"]
                        break
        except:
            recipe["prepTime"] = 15
            recipe["cookTime"] = 30

        # ===== INGREDIENTS =====
        try:
            ing_section = soup.find("ul", class_="choose-ingredients")
            if ing_section:
                for li in ing_section.find_all("li"):
                    name_block = li.find("div", class_="name")
                    if not name_block:
                        continue
                    raw = name_block.text.strip()
                    parsed = self.parse_ingredient(raw)

                    recipe["ingredients"].append({
                        "name": parsed["name"],
                        "quantity": parsed["qty"],
                        "unit": parsed["unit"]
                    })
        except Exception as e:
            logger.warning(f"Ingredient error: {e}")

        # ===== STEPS =====
        try:
            steps_section = soup.find("div", class_="fck_detail")  

            if steps_section:
                # ===== FORMAT 1: <div class="steep"> với <div>, <p>, <ul> =====
                for steep in steps_section.find_all("div", class_="steep", recursive=False):
                    
                    current_step_title = ""
                    children = steep.find_all(['p', 'ul'], recursive=False)

                    for element in children:
                        if element.name == 'p':
                            strong = element.find("strong")
                            if strong:
                                current_step_title = strong.get_text(strip=True)
                                # Xóa số đầu trước dấu chấm: "1. Tiêu đề" → "Tiêu đề"
                                current_step_title = re.sub(r'^\d+\.\s*', '', current_step_title).strip()
                            else:
                                text = element.get_text(strip=True)
                                if text and len(text) > 0 and text[0].isdigit() and '.' in text[:3]:
                                    # Xóa số đầu: "1. Tiêu đề" → "Tiêu đề"
                                    current_step_title = re.sub(r'^\d+\.\s*', '', text).strip()
                                elif text:
                                    full_instruction = (
                                        f"{current_step_title}\n{text}"
                                        if current_step_title else text
                                    )
                                    recipe["steps"].append({
                                        "stepNumber": len(recipe["steps"]) + 1,
                                        "instruction": full_instruction,
                                        "imageUrls": [],
                                        "videoUrls": [],
                                        "stepTime": 10
                                    })
                                    current_step_title = ""

                        elif element.name == 'ul':
                            instruction_parts = []
                            img_urls = []

                            for li in element.find_all("li", recursive=False):
                                p_tag = li.find("p", recursive=False)
                                if p_tag:
                                    text = p_tag.get_text(strip=True)
                                    if text:
                                        instruction_parts.append(text)
                                else:
                                    text = li.get_text(strip=True)
                                    if text:
                                        instruction_parts.append(text)

                                #  LẤY TẤT CẢ HÌNH ẢNH từ các figure
                                for figure in li.find_all("figure"):
                                    img_url = self.extract_image(figure)
                                    if img_url:
                                        img_urls.append(img_url)

                            instruction_text = "\n".join(instruction_parts)

                            if not instruction_text.strip():
                                continue

                            full_instruction = (
                                f"{current_step_title}\n{instruction_text}"
                                if current_step_title else instruction_text
                            )

                            recipe["steps"].append({
                                "stepNumber": len(recipe["steps"]) + 1,
                                "instruction": full_instruction,
                                "imageUrls": img_urls,
                                "videoUrls": [],
                                "stepTime": 10
                            })

                            current_step_title = ""

                # ===== FORMAT 2: <ol class="ol-list"> với mỗi <li> chứa <p> + <figure> =====
                ol_list = steps_section.find("ol", class_="ol-list")
                if ol_list:
                    for idx, li in enumerate(ol_list.find_all("li", recursive=False), 1):
                        # Lấy text từ <p> đầu tiên
                        p_tag = li.find("p", recursive=False)
                        instruction_text = ""
                        img_url = ""
                        
                        if p_tag:
                            instruction_text = p_tag.get_text(strip=True)
                        else:
                            # Nếu không có <p>, lấy text trực tiếp
                            instruction_text = li.get_text(strip=True)
                        
                        # Lấy ảnh từ <figure>
                        img_url = self.extract_image(li)
                        
                        # Tránh lưu step rỗng
                        if not instruction_text.strip():
                            continue
                        
                        recipe["steps"].append({
                            "stepNumber": len(recipe["steps"]) + 1,
                            "instruction": f"{instruction_text}",
                            "imageUrls": [img_url] if img_url else [],
                            "videoUrls": [],
                            "stepTime": 10
                        })

        except Exception as e:
            logger.warning(f"Steps error: {e}")

        # ===== DIFFICULTY =====
        recipe["difficulty"] = self.estimate_difficulty(recipe["ingredients"])

        return recipe

    def extract_image(self, parent):
        """Trích xuất URL ảnh từ thẻ <li>"""
        img = parent.find("img")
        if img:
            if img.get("data-src"):
                return img["data-src"]
            
            if img.get("data-srcset"):
                urls = img["data-srcset"].split(",")
                if urls:
                    return urls[0].strip().split(" ")[0]
            
            if img.get("src") and img["src"].startswith("http"):
                return img["src"]
        
        figure = parent.find("figure")
        if figure:
            meta = figure.find("meta", attrs={"itemprop": "url"})
            if meta and meta.get("content"):
                return meta["content"]
        
        return ""

    def parse_ingredient(self, text: str):
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
            name = quantity_part
        
        return {"name": name.rstrip('.'), "qty": 1, "unit": ""}

    def estimate_difficulty(self, ingredients):
        c = len(ingredients)
        if c <= 5: return "EASY"
        if c <= 12: return "MEDIUM"
        return "HARD"

    def export_to_json(self, recipe):
        if not recipe:
            return
        
        output_dir = "recipe"
        os.makedirs(output_dir, exist_ok=True)

        safe_name = recipe["title"].replace(" ", "_")[:40]
        filename = f"recipe_{safe_name}.json"

        filepath = os.path.join(output_dir, filename)

        with open(filepath, "w", encoding="utf-8") as f:
            json.dump(recipe, f, indent=2, ensure_ascii=False)

        logger.info(f"Saved: {filepath}")

    def close(self):
        if self.driver:
            try:
                self.driver.quit()
            except:
                pass


def main():
    url = "https://vnexpress.net/doi-song-cooking-banh-nuong-nhan-sua-dua-4508524.html"

    scraper = VnExpressRecipeScraper(use_selenium=False)
    
    try:
        start = time.time()
        recipe = scraper.scrape_recipe(url)
        elapsed = time.time() - start
        
        if recipe:
            scraper.export_to_json(recipe)
            logger.info(f"⏱️ Thời gian: {elapsed:.2f}s")
            logger.info(f"📂 Category: {recipe['category']}")
        else:
            logger.error("❌ Failed to scrape")
    finally:
        scraper.close()


# if __name__ == "__main__":
#     main()
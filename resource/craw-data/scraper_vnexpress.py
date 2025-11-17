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
            options.add_argument("--headless=new")  # ✅ Chạy gầm (không mở Chrome)
            options.add_argument("--disable-gpu")
            options.add_argument("--no-sandbox")
            options.add_argument("--disable-dev-shm-usage")  # ✅ Tiết kiệm RAM
            options.add_argument("--start-maximized")
            options.add_argument("--window-size=1920,1080")
            options.add_argument("--disable-blink-features=AutomationControlled")
            options.add_argument(
                "user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                "AppleWebKit/537.36 (KHTML, like Gecko) "
                "Chrome/120.0.0.0 Safari/537.36"
            )
            
            # Tắt các extension không cần
            prefs = {
                "profile.managed_default_content_settings.images": 2,  # Tắt ảnh (tăng tốc)
            }
            options.add_experimental_option("prefs", prefs)

            self.driver = webdriver.Chrome(
                service=Service(ChromeDriverManager().install()),
                options=options
            )
            #  Giảm timeout từ 10s xuống 5s
            self.driver.set_page_load_timeout(5)
            self.driver.set_script_timeout(5)

    def scrape_recipe(self, url: str):
        """Scrape với tùy chọn Selenium hoặc Requests"""
        logger.info(f" Loading: {url}")
        
        if self.use_selenium:
            soup = self._load_with_selenium(url)
        else:
            soup = self._load_with_requests(url)
        
        if not soup:
            return None

        recipe = self._parse_recipe(soup)
        logger.info("Done scraping.")
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
            logger.error(f" Requests failed: {e}. Falling back to Selenium...")
            self.use_selenium = True
            self.__init__(use_selenium=True)
            return self._load_with_selenium(url)

    def _load_with_selenium(self, url: str):
        """CHẬM: Dùng Selenium (khi requests không được)"""
        try:
            self.driver.get(url)
            
            #  Giảm wait time từ 10s xuống 3s
            try:
                WebDriverWait(self.driver, 3).until(
                    EC.presence_of_element_located((By.CLASS_NAME, "title-detail"))
                )
            except:
                logger.warning("⏱ Page load timeout, continuing anyway...")
            
            return BeautifulSoup(self.driver.page_source, "html.parser")
        except Exception as e:
            logger.error(f" Selenium failed: {e}")
            return None

    def _parse_recipe(self, soup):
        """Parse recipe từ soup"""
        recipe = {
            "title": "",
            "description": "",
            "region": "Việt Nam",
            "imageUrl": "",
            "videoUrl": "",
            "categories": [],
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

        # ===== CATEGORIES =====
        recipe["categories"] = ["Cơm Hàng Ngày", "Việt Nam"]

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
        # ===== STEPS - HANDLE UL, OL, P + FIGURE FORMATS =====
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
                        
                        # Tự động thêm tiêu đề nếu chưa có
                        # step_title = f"Bước {idx}. "
                        
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
        - Range: "2-3 tô"
        - Ghi chú: "(tùy chọn)", "(tuỳ ý)"
        
        Ví dụ:
        - "1/2 củ hành tây (tùy chọn)" → qty=0.5, unit="củ", name="hành tây"
        - "1.5 thìa nước mắm" → qty=1.5, unit="thìa", name="nước mắm"
        - "2-3 quả trứng" → qty=2.5, unit="quả", name="trứng"
        - "200 gr thịt lợn" → qty=200, unit="g", name="thịt lợn"
        """
        text = text.strip()
        
        # ===== LOẠI BỎ GHI CHÚ TRONG NGOẶC =====
        # "1/2 củ hành (tùy chọn)" → "1/2 củ hành"
        # name_with_note = re.sub(r'\s*\([^)]*\)\s*', '', text).strip()
        
        # ===== PATTERN  =====
        # Nhóm 1: Số đầu (có thể phân số như 1/2 hoặc thập phân như 1.5)
        # Nhóm 2: Số thứ 2 (nếu là range như 2-3)
        # Nhóm 3: Đơn vị
        # Nhóm 4: Tên nguyên liệu
        
        pattern = r"([\d\.]+(?:/\d+)?)\s*-?\s*([\d\.]+)?\s*([a-zA-ZÀ-ỹ]+)?\s*(.*)"
        match = re.match(pattern, text)
        
        if match:
            qty1_str = match.group(1)
            qty2_str = match.group(2)
            unit = (match.group(3) or "").lower()
            name = match.group(4).strip()
            
            # ===== PARSE SỐ LƯỢNG ĐẦU TIÊN =====
            try:
                # Nếu là phân số (1/2, 3/4, ...)
                if "/" in qty1_str:
                    numerator, denominator = qty1_str.split("/")
                    qty1 = float(numerator) / float(denominator)
                else:
                    # Là số thập phân (1.5) hoặc số nguyên (2)
                    qty1 = float(qty1_str)
            except:
                qty1 = 0
            
            # ===== PARSE SỐ LƯỢNG THỨ 2 (NẾU CÓ) =====
            qty2 = qty1  # Default: bằng qty1
            if qty2_str:
                try:
                    if "/" in qty2_str:
                        numerator, denominator = qty2_str.split("/")
                        qty2 = float(numerator) / float(denominator)
                    else:
                        qty2 = float(qty2_str)
                except:
                    qty2 = qty1
            
            # ===== TÍNH TRUNG BÌNH NẾULÀ RANGE =====
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
                "cái": "cái",
                "quả": "quả",
                "củ": "củ",
                "bó": "bó",
                "tô": "tô",
                "chén": "chén",
                "nắm": "nắm",
                "lát": "lát",
            }
            
            unit = unit_map.get(unit, unit)
            
            # ===== LÀNG TRỤ QUANTITY VỀ INT NẾU CÓ PHẦN THẬP PHÂN LÀ 0 =====
            if qty == int(qty):
                qty = int(qty)
            else:
                qty = round(qty, 2)

            
            return {
                "name": name,
                "qty": qty,
                "unit": unit
            }
        
        return {"name": text, "qty": 1, "unit": ""}

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
    url = "https://vnexpress.net/doi-song-cooking-bun-gao-xao-chay-4497090.html"

    scraper = VnExpressRecipeScraper(use_selenium=False)
    
    try:
        start = time.time()
        recipe = scraper.scrape_recipe(url)
        elapsed = time.time() - start
        
        if recipe:
            scraper.export_to_json(recipe)
            logger.info(f" Thời gian: {elapsed:.2f}s")
        else:
            logger.error(" Failed to scrape")
    finally:
        scraper.close()


# if __name__ == "__main__":
#     main()
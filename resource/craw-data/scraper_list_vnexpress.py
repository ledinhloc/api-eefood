import requests
from bs4 import BeautifulSoup
import logging
import time
import json
from pathlib import Path
from typing import List, Dict
from datetime import datetime

from scraper_vnexpress import VnExpressRecipeScraper

logging.basicConfig(level=logging.INFO, format="%(asctime)s - %(levelname)s - %(message)s")
logger = logging.getLogger(__name__)


class VnExpressFullScraper:
    def __init__(self):
        self.headers = {
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        }
        self.output_dir = Path("recipes_data")
        self.output_dir.mkdir(exist_ok=True)
        
        self.scraped_recipes = []  # Danh sách recipe đã crawl
        self.failed_urls = []      # URL không crawl được
        self.success_count = 0
        self.fail_count = 0
    
    def get_recipe_urls_from_page(self, url: str) -> List[Dict]:
        """
        Lấy danh sách URL recipe từ một trang
        
        Returns:
            list: [{'title': '...', 'url': '...', 'image': '...'}, ...]
        """
        logger.info(f"📄 Fetching page: {url}")
        
        try:
            response = requests.get(url, headers=self.headers, timeout=10)
            response.raise_for_status()
            soup = BeautifulSoup(response.content, "html.parser")
            
            recipes = []
            list_dish = soup.find("div", class_="list-dish")
            
            if not list_dish:
                logger.warning("⚠️ Không tìm thấy div.list-dish")
                return recipes
            
            articles = list_dish.find_all("article", class_="art_item")
            logger.info(f"   📦 Tìm thấy {len(articles)} bài viết")
            
            for article in articles:
                try:
                    # Lấy link
                    link_elem = article.find("a", class_="thumb_img", href=True)
                    if not link_elem:
                        continue
                    
                    recipe_url = link_elem.get("href", "").strip()
                    # if not recipe_url.startswith("http"):
                    #     recipe_url = "https://vnexpress.net" + recipe_url
                    
                    # Lấy tiêu đề
                    h2_elem = article.find("h2", class_="title_news")
                    title = h2_elem.get_text(strip=True) if h2_elem else "N/A"
                    
                    # Lấy ảnh
                    img_elem = article.find("img")
                    image = img_elem.get("data-src") or img_elem.get("src") if img_elem else ""
                    
                    recipes.append({
                        "title": title,
                        "url": recipe_url,
                        "image": image
                    })
                    
                except Exception as e:
                    logger.warning(f"   ⚠️ Error parsing article: {e}")
                    continue
            
            return recipes
        
        except Exception as e:
            logger.error(f"❌ Error fetching page: {e}")
            return []
    
    def get_all_recipe_urls(self, start_url: str, max_pages: int = 31) -> List[Dict]:
        """
        Lấy URL recipe từ tất cả các trang (1-31)
        
        Args:
            start_url: URL trang 1
            max_pages: Số trang tối đa (mặc định 31)
        
        Returns:
            list: Danh sách tất cả URL
        """
        all_urls = []
        
        for page in range(1, max_pages + 1):
            # Xây dựng URL trang
            if page == 1:
                page_url = start_url
            else:
                page_url = f"{start_url}-p{page}"
            
            logger.info(f"\n🔗 ===== TRANG {page}/{max_pages} =====")
            recipes = self.get_recipe_urls_from_page(page_url)
            
            if not recipes:
                logger.warning(f"⚠️ Trang {page} không có dữ liệu")
                break
            
            all_urls.extend(recipes)
            
            # Delay giữa các trang
            if page < max_pages:
                time.sleep(1)
        
        logger.info(f"\n✅ Tổng lấy được {len(all_urls)} URL recipe")
        return all_urls
    
    def crawl_recipe_details(self, recipe_urls: List[Dict]):
        """
        Crawl chi tiết từng recipe từ URL
        
        Args:
            recipe_urls: Danh sách URL recipe
        """
        logger.info(f"\n🚀 ===== BẮT ĐẦU CRAWL CHI TIẾT {len(recipe_urls)} RECIPE =====\n")
        
        detail_scraper = VnExpressRecipeScraper(use_selenium=False)
        
        for idx, recipe_info in enumerate(recipe_urls, 1):
            recipe_url = recipe_info['url']
            recipe_title = recipe_info['title']
            
            logger.info(f"[{idx}/{len(recipe_urls)}] ⏳ Crawling: {recipe_title}")
        
            try:
                # Crawl chi tiết
                detail = detail_scraper.scrape_recipe(recipe_url)
                
                if not detail:
                    logger.error(f"     ❌ Failed to get detail")
                    self.failed_urls.append({
                        "title": recipe_title,
                        "url": recipe_url,
                        "reason": "Không lấy được dữ liệu"
                    })
                    self.fail_count += 1
                    continue
                
                # Validate recipe
                if not self._validate_recipe(detail):
                    logger.warning(f"     ⚠️ Recipe không hợp lệ")
                    self.failed_urls.append({
                        "title": recipe_title,
                        "url": recipe_url,
                        "reason": "Định dạng không hợp lệ (thiếu ingredients hoặc steps)"
                    })
                    self.fail_count += 1
                    continue
                
                logger.info(f"     ✅ Success! ({len(detail['ingredients'])} nguyên liệu, {len(detail['steps'])} bước)")
                
                self.scraped_recipes.append(detail)
                self.success_count += 1
                
            except Exception as e:
                logger.error(f"     ❌ Exception: {e}")
                self.failed_urls.append({
                    "title": recipe_title,
                    "url": recipe_url,
                    "reason": str(e)
                })
                self.fail_count += 1
            
            # Delay giữa các request
            time.sleep(2)
        
        detail_scraper.close()
    
    def _validate_recipe(self, recipe: dict) -> bool:
        """Kiểm tra recipe có đủ dữ liệu không"""
        # Phải có tiêu đề
        if not recipe.get('title'):
            return False
        
        # Phải có ít nhất 3 nguyên liệu
        if len(recipe.get('ingredients', [])) < 3:
            return False
        
        # Phải có ít nhất 1 bước
        if len(recipe.get('steps', [])) < 1:
            return False
        
        return True
    
    def export_recipes(self):
        """
        Export recipes thành 1 file JSON duy nhất
        """
        if not self.scraped_recipes:
            logger.warning("⚠️ Không có recipe để export")
            return
        
        output_file = self.output_dir / "all_recipes.json"
        
        with open(output_file, "w", encoding="utf-8") as f:
            json.dump(self.scraped_recipes, f, indent=2, ensure_ascii=False)
        
        logger.info(f"✅ Exported {len(self.scraped_recipes)} recipes to: {output_file}")
    
    def export_statistics(self):
        """
        Export thống kê crawl
        """
        stats = {
            "timestamp": datetime.now().isoformat(),
            "total_found": len(self.scraped_recipes) + len(self.failed_urls),
            "total_success": self.success_count,
            "total_failed": self.fail_count,
            "success_rate": f"{(self.success_count / (self.success_count + self.fail_count) * 100):.1f}%" if (self.success_count + self.fail_count) > 0 else "0%",
            
            "recipes_list": [
                {
                    "id": idx,
                    "title": r["title"],
                    "ingredients_count": len(r.get("ingredients", [])),
                    "steps_count": len(r.get("steps", [])),
                    "difficulty": r.get("difficulty", "N/A"),
                    "cook_time": r.get("cookTime", 0)
                }
                for idx, r in enumerate(self.scraped_recipes, 1)
            ],
            
            "failed_urls": self.failed_urls
        }
        
        output_file = self.output_dir / "statistics.json"
        
        with open(output_file, "w", encoding="utf-8") as f:
            json.dump(stats, f, indent=2, ensure_ascii=False)
        
        logger.info(f"✅ Exported statistics to: {output_file}")
        
        # In thống kê ra console
        logger.info(f"\n📊 ===== THỐNG KÊ CRAWL =====")
        logger.info(f"⏰ Thời gian: {stats['timestamp']}")
        logger.info(f"✅ Thành công: {self.success_count}/{stats['total_found']} ({stats['success_rate']})")
        logger.info(f"❌ Thất bại: {self.fail_count}")
        logger.info(f"📋 Chi tiết: xem file {output_file}")


def main():
    start_url = "https://vnexpress.net/doi-song/cooking/mon-ngon-hang-ngay-25532"
    
    # ===== BƯỚC 1: Lấy danh sách URL từ 31 trang =====
    logger.info("===== BƯỚC 1: LẤY DANH SÁCH URL =====\n")
    scraper = VnExpressFullScraper()
    recipe_urls = scraper.get_all_recipe_urls(start_url, max_pages=5)
    
    if not recipe_urls:
        logger.error("❌ Không lấy được danh sách URL")
        return
    
    # ===== BƯỚC 2: Crawl chi tiết từng recipe =====
    logger.info("\n\n===== BƯỚC 2: CRAWL CHI TIẾT =====")
    scraper.crawl_recipe_details(recipe_urls)
    
    # ===== BƯỚC 3: Export kết quả =====
    logger.info("\n\n===== BƯỚC 3: EXPORT KẾT QUẢ =====\n")
    scraper.export_recipes()
    scraper.export_statistics()
    
    logger.info(f"\n✅ Hoàn thành! Kết quả lưu trong thư mục: {scraper.output_dir}")


if __name__ == "__main__":
    main()
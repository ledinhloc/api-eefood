import requests
from bs4 import BeautifulSoup
import logging
import time
import json
from pathlib import Path
from typing import List, Dict
from datetime import datetime
import asyncio
import aiohttp
import threading

from scraper_vnexpress import VnExpressRecipeScraper

logging.basicConfig(level=logging.INFO, format="%(asctime)s - %(levelname)s - %(message)s")
logger = logging.getLogger(__name__)


class VnExpressFullScraper:
    def __init__(self, max_concurrent: int = 5):
        self.headers = {
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        }
        self.output_dir = Path("recipes_data")
        self.output_dir.mkdir(exist_ok=True)
        
        self.scraped_recipes = []
        self.failed_urls = []
        self.success_count = 0
        self.fail_count = 0
        self.max_concurrent = max_concurrent  #  Số request đồng thời
    
    def get_recipe_urls_from_page(self, url: str) -> List[Dict]:
        """Lấy danh sách URL recipe từ một trang"""
        logger.info(f" Fetching page: {url}")
        
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
                    link_elem = article.find("a", class_="thumb_img", href=True)
                    if not link_elem:
                        continue
                    
                    recipe_url = link_elem.get("href", "").strip()
                    
                    h2_elem = article.find("h2", class_="title_news")
                    title = h2_elem.get_text(strip=True) if h2_elem else "N/A"
                    
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
        """Lấy URL recipe từ tất cả các trang"""
        all_urls = []
        
        for page in range(25, 32):
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
            
            if page < max_pages:
                time.sleep(0.5)  # Giảm delay từ 1s xuống 0.5s
        
        logger.info(f"\n✅ Tổng lấy được {len(all_urls)} URL recipe")
        return all_urls
    
    async def crawl_recipe_async(self, recipe_info: Dict, detail_scraper: VnExpressRecipeScraper, semaphore: asyncio.Semaphore):
        """ Crawl một recipe bất đồng bộ"""
        async with semaphore:  # Giới hạn số request đồng thời
            recipe_url = recipe_info['url']
            recipe_title = recipe_info['title']
            
            try:
                # Chạy crawl trong thread riêng để tránh block
                loop = asyncio.get_event_loop()
                detail = await loop.run_in_executor(None, detail_scraper.scrape_recipe, recipe_url)
                
                if not detail:
                    logger.error(f"[FAIL] {recipe_title}: Không lấy được dữ liệu")
                    self.failed_urls.append({
                        "title": recipe_title,
                        "url": recipe_url,
                        "reason": "Không lấy được dữ liệu"
                    })
                    self.fail_count += 1
                    return
                
                # Validate recipe
                if not self._validate_recipe(detail):
                    logger.warning(f"[INVALID] {recipe_title}: Định dạng không hợp lệ")
                    self.failed_urls.append({
                        "title": recipe_title,
                        "url": recipe_url,
                        "reason": "Định dạng không hợp lệ"
                    })
                    self.fail_count += 1
                    return
                
                logger.info(f"[OK] {recipe_title} ({len(detail['ingredients'])} ingredients, {len(detail['steps'])} steps)")
                
                self.scraped_recipes.append(detail)
                self.success_count += 1
                
            except Exception as e:
                logger.error(f"[ERROR] {recipe_title}: {str(e)[:50]}")
                self.failed_urls.append({
                    "title": recipe_title,
                    "url": recipe_url,
                    "reason": str(e)[:100]
                })
                self.fail_count += 1
    
    async def crawl_recipe_details_async(self, recipe_urls: List[Dict]):
        """ Crawl tất cả recipe bất đồng bộ"""
        logger.info(f"\n ===== BẮT ĐẦU CRAWL BẤT ĐỒNG BỘ {len(recipe_urls)} RECIPE =====\n")
        logger.info(f" Số request đồng thời: {self.max_concurrent}\n")
        
        detail_scraper = VnExpressRecipeScraper(use_selenium=False)
        semaphore = asyncio.Semaphore(self.max_concurrent)  # Giới hạn đồng thời
        
        # Tạo danh sách task
        tasks = [
            self.crawl_recipe_async(recipe_info, detail_scraper, semaphore)
            for recipe_info in recipe_urls
        ]
        
        # Chạy tất cả task đồng thời
        start_time = time.time()
        await asyncio.gather(*tasks)
        elapsed = time.time() - start_time
        
        detail_scraper.close()
        
        logger.info(f"\n⏱️ Thời gian crawl: {elapsed:.2f}s")
    
    def crawl_recipe_details(self, recipe_urls: List[Dict]):
        """Wrapper để chạy async code"""
        asyncio.run(self.crawl_recipe_details_async(recipe_urls))
    
    def _validate_recipe(self, recipe: dict) -> bool:
        """Kiểm tra recipe có đủ dữ liệu không"""
        if not recipe.get('title'):
            return False
        
        if len(recipe.get('ingredients', [])) < 3:
            return False
        
        if len(recipe.get('steps', [])) < 1:
            return False
        
        return True
    
    def export_recipes(self):
        """Export recipes thành 1 file JSON"""
        if not self.scraped_recipes:
            logger.warning(" Không có recipe để export")
            return
        
        output_file = self.output_dir / "all_recipes.json"
        
        with open(output_file, "w", encoding="utf-8") as f:
            json.dump(self.scraped_recipes, f, indent=2, ensure_ascii=False)
        
        logger.info(f"✅ Exported {len(self.scraped_recipes)} recipes to: {output_file}")
    
    def export_statistics(self):
        """Export thống kê crawl"""
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
        
        logger.info(f"\n📊 ===== THỐNG KÊ CRAWL =====")
        logger.info(f"⏰ Thời gian: {stats['timestamp']}")
        logger.info(f"✅ Thành công: {self.success_count}/{stats['total_found']} ({stats['success_rate']})")
        logger.info(f"❌ Thất bại: {self.fail_count}")
        logger.info(f"📋 Chi tiết: xem file {output_file}")


def main():
    start_url = "https://vnexpress.net/doi-song/cooking/mon-ngon-hang-ngay-25532"
    
    # ===== BƯỚC 1: Lấy danh sách URL =====
    logger.info("===== BƯỚC 1: LẤY DANH SÁCH URL =====\n")
    scraper = VnExpressFullScraper(max_concurrent=5)  # 5 request cùng lúc
    recipe_urls = scraper.get_all_recipe_urls(start_url, max_pages=31)
    
    if not recipe_urls:
        logger.error("❌ Không lấy được danh sách URL")
        return
    
    # ===== BƯỚC 2: Crawl chi tiết (BẤT ĐỒNG BỘ) =====
    logger.info("\n\n===== BƯỚC 2: CRAWL CHI TIẾT (BẤT ĐỒNG BỘ) =====")
    start = time.time()
    scraper.crawl_recipe_details(recipe_urls)
    elapsed = time.time() - start
    
    # ===== BƯỚC 3: Export kết quả =====
    logger.info("\n\n===== BƯỚC 3: EXPORT KẾT QUẢ =====\n")
    scraper.export_recipes()
    scraper.export_statistics()
    
    logger.info(f"\n✅ Hoàn thành trong {elapsed:.2f}s!")
    logger.info(f"📁 Kết quả lưu trong: {scraper.output_dir}")


if __name__ == "__main__":
    main()
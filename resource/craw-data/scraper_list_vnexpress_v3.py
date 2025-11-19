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

from scraper_vnexpress import VnExpressRecipeScraper

logging.basicConfig(level=logging.INFO, format="%(asctime)s - %(levelname)s - %(message)s")
logger = logging.getLogger(__name__)


class VnExpressFullScraper:
    def __init__(self, max_concurrent: int = 10):
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
        self.max_concurrent = max_concurrent

    async def get_recipe_urls_from_page_async(self, url: str, category: str, session: aiohttp.ClientSession) -> tuple[List[Dict], bool, int]:
        """
        Lấy danh sách URL recipe từ một trang (async)
        Returns: (recipes, has_next_page, max_page_number)
        """
        logger.info(f"📄 Fetching page: {url}")
        
        try:
            async with session.get(url, headers=self.headers, timeout=aiohttp.ClientTimeout(total=10)) as response:
                response.raise_for_status()
                content = await response.text()
                soup = BeautifulSoup(content, "html.parser")
            
            recipes = []
            list_dish = soup.find("div", class_="list-dish")
            
            if not list_dish:
                logger.warning("⚠️ Không tìm thấy div.list-dish")
                return recipes, False, 1
            
            articles = list_dish.find_all("article", class_="art_item")
            logger.info(f"   📦 Tìm thấy {len(articles)} bài viết")
            
            # ✅ Tìm số trang tối đa và kiểm tra nút "Next"
            max_page = 1
            has_next = False
            
            pagination = soup.find("div", id="pagination")
            if pagination:
                button_page = pagination.find("div", class_="button-page")
                if button_page:
                    # Tìm tất cả các link trang
                    page_links = button_page.find_all("a", href=True)
                    
                    for link in page_links:
                        href = link.get("href", "")
                        
                        # Kiểm tra nút Next (có class="pagination_btn pa_next")
                        if "pa_next" in link.get("class", []):
                            # Nếu href không phải "javascript:;" → còn trang tiếp theo
                            if "javascript" not in href:
                                has_next = True
                        
                        # Tìm số trang từ URL (format: -p3, -p10, etc.)
                        if "-p" in href:
                            try:
                                page_num = int(href.split("-p")[-1].split("?")[0])
                                max_page = max(max_page, page_num)
                            except:
                                pass
                        
                        # Hoặc tìm từ text của link (1, 2, 3, ...)
                        text = link.get_text(strip=True)
                        if text.isdigit():
                            page_num = int(text)
                            max_page = max(max_page, page_num)
            
            logger.info(f"   📊 Pagination: max_page={max_page}, has_next={has_next}")
            
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
                        "image": image,
                        "category": category
                    })
                    
                except Exception as e:
                    logger.warning(f"   ⚠️ Error parsing article: {e}")
                    continue
            
            return recipes, has_next, max_page
        
        except Exception as e:
            logger.error(f"❌ Error fetching page: {e}")
            return [], False, 1


    async def get_all_recipe_urls_async(self, list_url: List[Dict], max_pages: int = 100) -> List[Dict]:
        """Lấy URL recipe từ tất cả các category (tự động dừng khi hết trang)"""
        all_urls = []
        seen_urls = set()  # ✅ Tránh duplicate
        
        async with aiohttp.ClientSession() as session:
            for url_info in list_url:
                base_url = url_info["url"]
                category = url_info["category"]
                
                logger.info(f"\n🔍 Đang crawl category: {category}")
                logger.info(f"   URL: {base_url}")
                
                page = 1
                category_urls = []
                detected_max_page = max_pages  # Mặc định là tham số truyền vào
                
                while page <= detected_max_page:
                    page_url = base_url if page == 1 else f"{base_url}-p{page}"
                    
                    recipes, has_next, found_max_page = await self.get_recipe_urls_from_page_async(
                        page_url, category, session
                    )
                    
                    # ✅ Cập nhật số trang tối đa từ pagination (chỉ lần đầu)
                    if page == 1 and found_max_page > 1:
                        detected_max_page = min(found_max_page, max_pages)  # Không vượt quá max_pages
                        logger.info(f"   🎯 Phát hiện category có {detected_max_page} trang")
                    
                    # ✅ Kiểm tra duplicate
                    new_recipes = []
                    duplicate_count = 0
                    for recipe in recipes:
                        if recipe['url'] not in seen_urls:
                            seen_urls.add(recipe['url'])
                            new_recipes.append(recipe)
                        else:
                            duplicate_count += 1
                    
                    if not new_recipes and duplicate_count == 0:
                        logger.info(f"   ⛔ Trang {page}: Không có recipe → Dừng crawl")
                        break
                    
                    if new_recipes:
                        category_urls.extend(new_recipes)
                        logger.info(f"   ✅ Trang {page}/{detected_max_page}: +{len(new_recipes)} mới, {duplicate_count} trùng (tổng: {len(category_urls)})")
                    else:
                        logger.info(f"   ⚠️ Trang {page}/{detected_max_page}: Tất cả đã trùng ({duplicate_count} recipes)")
                    
                    # ✅ Dừng nếu không có nút "Next" hoặc đã đến trang cuối
                    if not has_next or page >= detected_max_page:
                        logger.info(f"   ⛔ Đã đến trang cuối ({page}/{detected_max_page})")
                        break
                    
                    page += 1
                    await asyncio.sleep(0.5)  # ✅ Delay nhẹ tránh spam
                
                all_urls.extend(category_urls)
                logger.info(f"   📊 Tổng {category}: {len(category_urls)} recipes từ {page} trang\n")
        
        logger.info(f"\n✅ Tổng lấy được {len(all_urls)} URL recipe từ {len(list_url)} categories")
        return all_urls

    def get_all_recipe_urls(self, list_url: List[Dict], max_pages: int = 100) -> List[Dict]:
        """Wrapper để chạy async code"""
        return asyncio.run(self.get_all_recipe_urls_async(list_url, max_pages))
    
    async def crawl_recipe_async(self, recipe_info: Dict, detail_scraper: VnExpressRecipeScraper, semaphore: asyncio.Semaphore):
        """Crawl một recipe bất đồng bộ"""
        async with semaphore:
            recipe_url = recipe_info['url']
            recipe_title = recipe_info['title']
            recipe_category = recipe_info.get('category', 'N/A')
            
            try:
                # Chạy crawl trong thread riêng để tránh block
                loop = asyncio.get_event_loop()
                detail = await loop.run_in_executor(
                    None, 
                    detail_scraper.scrape_recipe, 
                    recipe_url,
                    recipe_category  # ✅ Truyền category vào scraper
                )
                
                if not detail:
                    logger.error(f"[FAIL] {recipe_title}: Không lấy được dữ liệu")
                    self.failed_urls.append({
                        "title": recipe_title,
                        "url": recipe_url,
                        "category": recipe_category,
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
                        "category": recipe_category,
                        "reason": "Định dạng không hợp lệ"
                    })
                    self.fail_count += 1
                    return
                
                logger.info(f"[OK] {recipe_title} - {recipe_category} ({len(detail['ingredients'])} ingredients, {len(detail['steps'])} steps)")
                
                self.scraped_recipes.append(detail)
                self.success_count += 1
                
            except Exception as e:
                logger.error(f"[ERROR] {recipe_title}: {str(e)[:50]}")
                self.failed_urls.append({
                    "title": recipe_title,
                    "url": recipe_url,
                    "category": recipe_category,
                    "reason": str(e)[:100]
                })
                self.fail_count += 1
    
    async def crawl_recipe_details_async(self, recipe_urls: List[Dict]):
        """Crawl tất cả recipe bất đồng bộ"""
        logger.info(f"\n🚀 ===== BẮT ĐẦU CRAWL BẤT ĐỒNG BỘ {len(recipe_urls)} RECIPE =====\n")
        logger.info(f"⚡ Số request đồng thời: {self.max_concurrent}\n")
        
        detail_scraper = VnExpressRecipeScraper(use_selenium=False)
        semaphore = asyncio.Semaphore(self.max_concurrent)
        
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
            logger.warning("⚠️ Không có recipe để export")
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
            
            # Thống kê theo category
            "categories_stats": self._get_category_stats(),
            
            "recipes_list": [
                {
                    "id": idx,
                    "title": r["title"],
                    "categories": r.get("categories", ["N/A"]),
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
        
        # In thống kê theo category
        logger.info(f"\n📂 Thống kê theo category:")
        for cat, count in stats['categories_stats'].items():
            logger.info(f"   - {cat}: {count} recipes")
        
        logger.info(f"\n📋 Chi tiết: xem file {output_file}")
    
    def _get_category_stats(self) -> Dict[str, int]:
        """Thống kê số lượng recipe theo category"""
        category_count = {}
        for recipe in self.scraped_recipes:
            categories = recipe.get("categories", [])
            for cat in categories:
                category_count[cat] = category_count.get(cat, 0) + 1
            
            # Nếu không có category nào, đếm vào "N/A"
            if not categories:
                category_count["N/A"] = category_count.get("N/A", 0) + 1
        
        return category_count


def main():
    list_url = [
        {"url": "https://vnexpress.net/doi-song/cooking/mon-tet-25905", "category": "Món tết"},
        {"url": "https://vnexpress.net/doi-song/cooking/mon-ngon-hang-ngay-25532", "category": "Món ngon hàng ngày"},
        {"url": "https://vnexpress.net/doi-song/cooking/mon-ngon-ngay-lanh-25839", "category": "Món ngon ngày lạnh"},
        {"url": "https://vnexpress.net/doi-song/cooking/mon-ngon-cho-cuoi-tuan-25533", "category": "Món ngon cho cuối tuần"},
        {"url": "https://vnexpress.net/doi-song/cooking/mon-ngon-theo-vung-mien-25534", "category": "Món ngon theo vùng miền"},
        {"url": "https://vnexpress.net/doi-song/cooking/mon-trang-mieng-giai-khat-25536", "category": "Món tráng miệng, giải khát"},
        {"url": "https://vnexpress.net/doi-song/cooking/thuc-don-cho-ngay-nang-nong-25535", "category": "Ngày nắng nóng"},
        {"url": "https://vnexpress.net/doi-song/cooking/thuc-don-hang-ngay-25531", "category": "Thực đơn hàng ngày"},
        {"url": "https://vnexpress.net/doi-song/cooking/qua-mon-an-vat-25570", "category": "Quà - Món ăn vặt"},
        {"url": "https://vnexpress.net/doi-song/cooking/mon-chay-26342", "category": "Món chay"},
        {"url": "https://vnexpress.net/doi-song/cooking/bua-sang-don-gian-25574", "category": "Bữa sáng đơn giản"},
        {"url": "https://vnexpress.net/doi-song/cooking/cac-loai-banh-26379", "category": "Các loại bánh"},
    ]
    
    logger.info("===== BƯỚC 1: LẤY DANH SÁCH URL (TỰ ĐỘNG PHÁT HIỆN HẾT TRANG) =====\n")
    scraper = VnExpressFullScraper(max_concurrent=10)
    
    start = time.time()
    recipe_urls = scraper.get_all_recipe_urls(list_url, max_pages=50)
    elapsed = time.time() - start
    logger.info(f"⏱️ Thời gian lấy URL: {elapsed:.2f}s")
    
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
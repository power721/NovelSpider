package cn.har01d.novel.spider

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/novels")
class NovelController(
    private val novelService: NovelService
) {

    @PostMapping("/crawl")
    fun startCrawl(
        @RequestParam(value = "start", defaultValue = "0") start: Int,
        @RequestParam(value = "pages", defaultValue = "5") pages: Int
    ): ResponseEntity<String> {
        novelService.crawlNovels(start, pages)
        return ResponseEntity.ok("爬虫任务已启动，正在爬取 $pages 页数据")
    }

    @GetMapping("/status")
    fun getStatus(): ResponseEntity<String> {
        return ResponseEntity.ok("爬虫服务运行正常")
    }

    @GetMapping("/search")
    fun searchNovels(request: SearchRequest, pageable: Pageable): Page<Novel> {
        return novelService.searchNovels(request, pageable)
    }
}

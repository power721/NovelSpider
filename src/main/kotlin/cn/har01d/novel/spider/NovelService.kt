package cn.har01d.novel.spider

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.CompletableFuture

@Service
class NovelService(
    private val novelRepository: NovelRepository
) {

    companion object {
        private val logger = LoggerFactory.getLogger(NovelService::class.java)
    }

    @Value("\${spider.base-url:http://www.999xiaoshuo.cc}")
    private lateinit var baseUrl: String

    @Value("\${spider.timeout:10000}")
    private var timeout: Int = 10000

    @Value("\${spider.max-pages:5}")
    private var maxPages: Int = 5

    fun parseNovelInfo(novelItem: Element): Novel? {
        return try {
            val novel = Novel()

            // 提取标题
            val titleTag = novelItem.selectFirst("h2")
            novel.title = titleTag?.text()?.trim() ?: "未知标题"

            // 提取链接
            val linkTag = novelItem.selectFirst("a[href]")
            var novelUrl = linkTag?.attr("href") ?: return null
            if (novelUrl.isNotEmpty() && !novelUrl.startsWith("http")) {
                novelUrl = baseUrl + novelUrl
            }
            novel.novelUrl = novelUrl
            novel.id = novelUrl.split("/").last().replace(".html", "").toLong()

            // 提取分类和状态
            val categoryStatus = novelItem.selectFirst("span")
            if (categoryStatus != null) {
                val parts = categoryStatus.text().trim().split("/")
                novel.category = parts.getOrElse(0) { "未知分类" }.trim()
                novel.status = parts.getOrElse(1) { "未知状态" }.trim()
            } else {
                novel.category = "未知分类"
                novel.status = "未知状态"
            }

            // 提取作者
            val authorTag = novelItem.selectFirst("i.fa-user-circle-o")
            novel.author = authorTag?.text()
                ?.replace("&nbsp;", "")
                ?.replace(" ", "")
                ?.trim() ?: "未知作者"

            // 提取字数
            val wordCountTag = novelItem.selectFirst("em.orange")
            val wordCount = wordCountTag?.text()?.trim() ?: "0万字"
            novel.wordCount = wordCount.replace("万字", "").toLong() * 10000

            // 提取更新时间
            val updateTimeTag = novelItem.selectFirst("em.blue")
            val latestUpdate = updateTimeTag?.text()?.trim() ?: "未知时间"
            if (latestUpdate == "刚刚") {
                novel.updatedAt = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)
            } else if (latestUpdate.contains("分钟前")) {
                val minutes = latestUpdate.replace("分钟前", "").toLong()
                novel.updatedAt = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES).minusMinutes(minutes)
            } else if (latestUpdate.contains("小时前")) {
                val hours = latestUpdate.replace("小时前", "").toLong()
                novel.updatedAt = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES).minusHours(hours)
            } else if (latestUpdate.contains("天前")) {
                val days = latestUpdate.replace("天前", "").toLong()
                novel.updatedAt = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS).minusDays(days)
            }

            // 提取描述
            val descTag = novelItem.selectFirst("p.indent")
            novel.description = descTag?.text()?.trim() ?: ""

            novel
        } catch (e: Exception) {
            logger.error("解析小说信息失败", e)
            null
        }
    }

    fun getNovelList(page: Int): List<Novel> {
        val novels = mutableListOf<Novel>()
        var exception: Exception? = null

        for (i in 1..3) {
            try {
                val url = "$baseUrl/html/$page.html"
                logger.info("开始爬取第 {} 页 {}", page, url)

                val doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(timeout)
                    .get()

                val novelItems = doc.select("ul.flex li")
                    .filter { it -> it.selectFirst("h2") != null && it.selectFirst("p.indent") != null }

                novelItems.forEach { item ->
                    parseNovelInfo(item)?.let { novels.add(it) }
                }

                logger.info("第 {} 页获取到 {} 本小说", page, novels.size)

                return novels
            } catch (e: Exception) {
                exception = e
                logger.warn("获取第 {} 页小说列表失败", page, e)
                Thread.sleep(5000)
            }
        }
        throw exception ?: IllegalStateException("获取第${page}页小说列表失败")
    }

    @Transactional
    fun saveOrUpdateNovel(novel: Novel) {
        novelRepository.save(novel)
        logger.info("保存小说: {} {} - {}", novel.novelUrl, novel.title, novel.author)
    }

    @Async
    fun crawlNovels(pages: Int): CompletableFuture<Void> {
        return CompletableFuture.runAsync {
            for (page in 1..pages) {
                try {
                    val novels = getNovelList(page)

                    novels.forEach { saveOrUpdateNovel(it) }

                    logger.info("第 {} 页爬取完成，处理 {} 本小说", page, novels.size)

                    // 延迟避免频繁请求
                    Thread.sleep(5000)

                } catch (e: Exception) {
                    logger.error("爬取第 {} 页失败", page, e)
                }
            }
        }
    }

    fun searchNovels(request: SearchRequest, pageable: Pageable): Page<Novel> {
        return novelRepository.search(request.q, request.author, request.status, request.category, pageable)
    }

    fun startCrawling() {
        crawlNovels(maxPages)
    }
}

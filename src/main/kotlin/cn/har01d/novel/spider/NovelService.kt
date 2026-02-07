package cn.har01d.novel.spider

import jakarta.annotation.PostConstruct
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicBoolean

@Service
class NovelService(
    private val novelRepository: NovelRepository
) {

    companion object {
        private val logger = LoggerFactory.getLogger(NovelService::class.java)
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val INITIAL_RETRY_DELAY_MS = 5000L
        private const val CRAWL_DELAY_MS = 10000L
        private const val CRAWL_DELAY_VARIATION_MS = 1000L
        private const val WORD_COUNT_MULTIPLIER = 10000L
        private const val USER_AGENT =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Safari/537.36"
        private const val COOKIE_FILE = "cookies.txt"
    }

    @Value("\${spider.base-url:http://www.999xiaoshuo.cc}")
    private lateinit var baseUrl: String

    @Value("\${spider.timeout:10000}")
    private var timeout: Int = 10000

    @Value("\${spider.max-pages:10}")
    private var maxPages: Int = 10

    private val working = AtomicBoolean(false)

    private var cookie =
        "fontSize=20px; ismini=1; isnight=1; server_name_session=c570e5ab596085fde0ac25c25e6b570f; 21b687374f9f2d27e97e76ebcbed1570=692740f22aa1e357e1043b306172f70f"

    private var errors = 0;

    @PostConstruct
    fun init() {
        loadCookiesFromFile()
    }

    private fun loadCookiesFromFile() {
        try {
            val cookieFile = File(COOKIE_FILE)
            if (cookieFile.exists()) {
                val savedCookie = cookieFile.readText().trim()
                if (savedCookie.isNotEmpty()) {
                    cookie = savedCookie
                    logger.info("从文件加载 cookie: {}", cookie)
                }
            } else {
                logger.info("Cookie 文件不存在，使用默认 cookie")
            }
        } catch (e: Exception) {
            logger.warn("加载 cookie 文件失败，使用默认 cookie", e)
        }
    }

    private fun saveCookiesToFile() {
        try {
            val cookieFile = File(COOKIE_FILE)
            cookieFile.writeText(cookie)
            logger.debug("Cookie 已保存到文件")
        } catch (e: Exception) {
            logger.warn("保存 cookie 到文件失败", e)
        }
    }

    private fun parseNovelInfo(novelItem: Element): Novel? {
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

            val idStr = novelUrl.split("/").last().replace(".html", "")
            novel.id = try {
                idStr.toLong()
            } catch (e: NumberFormatException) {
                logger.warn("无法解析小说ID: {}", idStr)
                return null
            }

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
            novel.wordCount = wordCount.replace("万字", "").toLong() * WORD_COUNT_MULTIPLIER

            // 提取更新时间
            val updateTimeTag = novelItem.selectFirst("em.blue")
            val latestUpdate = updateTimeTag?.text()?.trim() ?: "未知时间"
            novel.updatedAt = parseUpdateTime(latestUpdate)

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
        var sleep = INITIAL_RETRY_DELAY_MS

        repeat(MAX_RETRY_ATTEMPTS) {
            try {
                val url = "$baseUrl/html/$page.html"
                logger.info("开始爬取第 {} 页 {}", page, url)

                val connection = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .referrer(baseUrl)
                    .header("cookie", cookie)
                    .timeout(timeout)
                    .ignoreHttpErrors(true)

                val response = connection.execute()

                if (!updateCookiesFromResponse(response)) {
                    val doc = response.parse()

                    val novelItems = doc.select("ul.flex li")
                        .filter { n -> n.selectFirst("h2") != null && n.selectFirst("p.indent") != null }

                    novelItems.forEach { item ->
                        parseNovelInfo(item)?.let { novels.add(it) }
                    }

                    errors = 0
                    logger.info("第 {} 页获取到 {} 本小说", page, novels.size)
                    return novels
                }
            } catch (e: Exception) {
                exception = e
                logger.warn("获取第 {} 页小说列表失败 (尝试 {}/{})", page, it + 1, MAX_RETRY_ATTEMPTS, e)
                if (e.message != null && e.message!!.contains("HTTP/1.1 header parser received no bytes")) {
                    Thread.sleep(300_000L)
                } else {
                    Thread.sleep(sleep)
                    sleep *= 2
                }
                errors++
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
    fun crawlNovels(start: Int, pages: Int): CompletableFuture<Void> {
        return CompletableFuture.runAsync {
            if (!working.compareAndSet(false, true)) {
                logger.info("爬虫任务已在运行中，跳过本次执行")
                return@runAsync
            }

            try {
                for (page in start + 1..start + pages) {
                    try {
                        val novels = getNovelList(page)

                        novels.forEach { saveOrUpdateNovel(it) }

                        logger.info("第 {} 页爬取完成，处理 {} 本小说", page, novels.size)

                        if (novels.isEmpty() || errors > 10) {
                            break
                        }

                        // 延迟避免频繁请求
                        Thread.sleep(CRAWL_DELAY_MS + page * 10 + ThreadLocalRandom.current().nextLong(CRAWL_DELAY_VARIATION_MS))
                    } catch (e: Exception) {
                        logger.error("爬取第 {} 页失败", page, e)
                    }
                }
            } finally {
                working.set(false)
                logger.info("爬虫任务完成，working 标志已重置")
            }
        }
    }

    fun searchNovels(request: SearchRequest, pageable: Pageable): Page<Novel> {
        return novelRepository.search(request.q, request.author, request.status, request.category, pageable)
    }

    fun startCrawling() {
        if (working.get()) {
            return
        }
        crawlNovels(0, maxPages)
    }

    private fun parseUpdateTime(timeStr: String): LocalDateTime {
        return when {
            timeStr == "刚刚" -> LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
            timeStr.contains("分钟前") -> {
                val minutes = timeStr.replace("分钟前", "").toLong()
                LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES).minusMinutes(minutes)
            }

            timeStr.contains("小时前") -> {
                val hours = timeStr.replace("小时前", "").toLong()
                LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES).minusHours(hours)
            }

            timeStr.contains("天前") -> {
                val days = timeStr.replace("天前", "").toLong()
                LocalDateTime.now().truncatedTo(ChronoUnit.DAYS).minusDays(days)
            }

            timeStr.contains("个月前") -> {
                val months = timeStr.replace("个月前", "").toLong()
                LocalDateTime.now().truncatedTo(ChronoUnit.DAYS).minusMonths(months)
            }

            else -> LocalDate.parse(timeStr).atStartOfDay()
        }
    }

    private fun updateCookiesFromResponse(response: org.jsoup.Connection.Response): Boolean {
        val setCookies = response.headers("set-cookie")
        if (setCookies.isEmpty()) return false

        val cookieMap = cookie.split("; ")
            .mapNotNull { cookiePair ->
                val parts = cookiePair.split("=", limit = 2)
                if (parts.size == 2) parts[0] to parts[1] else null
            }
            .toMap()
            .toMutableMap()

        setCookies.forEach { setCookie ->
            val cookiePair = setCookie.split(";")[0]
            val parts = cookiePair.split("=", limit = 2)
            if (parts.isNotEmpty()) {
                val name = parts[0]
                val value = if (parts.size > 1) parts[1] else ""
                cookieMap[name] = value
            }
        }

        cookie = cookieMap.entries
            .filter { it.value.isNotEmpty() }
            .joinToString("; ") { "${it.key}=${it.value}" }

        logger.info("更新 cookie: {}", cookie)
        saveCookiesToFile()
        return true
    }
}

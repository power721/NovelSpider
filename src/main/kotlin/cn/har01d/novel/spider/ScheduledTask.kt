package cn.har01d.novel.spider

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ScheduledTask(
    private val novelService: NovelService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(ScheduledTask::class.java)
    }

    @Scheduled(cron = "0 0 * * * ?")
    fun scheduledCrawl() {
        logger.info("开始定时爬虫任务")
        novelService.startCrawling()
    }
}

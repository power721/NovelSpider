package cn.har01d.novel.spider

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication
class NovelSpiderApplication

fun main(args: Array<String>) {
    runApplication<NovelSpiderApplication>(*args)
}

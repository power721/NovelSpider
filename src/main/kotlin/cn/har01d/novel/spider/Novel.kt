package cn.har01d.novel.spider

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "novels")
data class Novel(
    @Id
    var id: Long = 0,

    @Column(nullable = false, length = 255)
    var title: String = "",

    @Column(length = 100)
    var author: String = "",

    @Column(length = 50)
    var category: String = "",

    @Column(length = 20)
    var status: String = "",

    var wordCount: Long = 0,

    @Column(columnDefinition = "TEXT")
    var description: String = "",

    @Column(length = 500)
    var novelUrl: String = "",

    @Column(updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    var updatedAt: LocalDateTime = LocalDateTime.now()
)

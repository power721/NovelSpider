package cn.har01d.novelspider

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
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

    @Column(name = "word_count", length = 50)
    var wordCount: String = "",

    @Column(name = "latest_update", length = 100)
    var latestUpdate: String = "",

    @Column(columnDefinition = "TEXT")
    var description: String = "",

    @Column(name = "novel_url", length = 500)
    var novelUrl: String = "",

    @Column(name = "created_at", updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    @PrePersist
    fun onCreate() {
        createdAt = LocalDateTime.now()
        updatedAt = LocalDateTime.now()
    }

    @PreUpdate
    fun onUpdate() {
        updatedAt = LocalDateTime.now()
    }
}

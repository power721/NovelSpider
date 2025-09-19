package cn.har01d.novel.spider

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface NovelRepository : JpaRepository<Novel, Long> {
    @Query(
        """
        SELECT n FROM Novel n
        WHERE (:query IS NULL OR :query = '' OR n.title LIKE %:query%)
          AND (:author IS NULL OR :author = '' OR n.author LIKE %:author%)
          AND (:status IS NULL OR :status = '' OR n.status = :status)
          AND (:category IS NULL OR :category = '' OR n.category = :category)
        """
    )
    fun search(
        query: String?,
        author: String?,
        status: String?,
        category: String?,
        pageable: Pageable
    ): Page<Novel>
}

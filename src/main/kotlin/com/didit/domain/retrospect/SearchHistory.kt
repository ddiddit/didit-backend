package com.didit.domain.retrospect

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime
import java.util.UUID

@Table(
    name = "search_histories",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["user_id", "keyword"]),
    ],
)
@Entity
class SearchHistory(
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val id: UUID = UUID.randomUUID(),
    @Column(columnDefinition = "BINARY(16)", nullable = false)
    val userId: UUID,
    @Column(nullable = false)
    val keyword: String,
    @Column(nullable = false)
    var searchedAt: LocalDateTime,
) {
    companion object {
        fun create(
            userId: UUID,
            keyword: String,
        ): SearchHistory =
            SearchHistory(
                id = UUID.randomUUID(),
                userId = userId,
                keyword = keyword,
                searchedAt = LocalDateTime.now(),
            )
    }

    fun updateSearchedAt() {
        this.searchedAt = LocalDateTime.now()
    }
}

package com.didit.application.inquiry.required

import com.didit.domain.inquiry.Inquiry
import com.didit.domain.inquiry.InquiryRegisterRequest
import com.didit.domain.inquiry.InquiryType
import com.didit.support.RepositoryTestSupport
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test

class InquiryRepositoryTest : RepositoryTestSupport() {
    @Autowired
    lateinit var inquiryRepository: InquiryRepository

    @Autowired
    lateinit var entityManager: EntityManager

    @Test
    fun `save`() {
        val inquiry = createInquiry()

        val saved = inquiryRepository.save(inquiry)

        assertThat(saved.id).isEqualTo(inquiry.id)
        assertThat(saved.userId).isEqualTo(inquiry.userId)
    }

    @Test
    fun `findByIdAndDeletedAtIsNull`() {
        val inquiry = inquiryRepository.save(createInquiry())

        val found = inquiryRepository.findByIdAndDeletedAtIsNull(inquiry.id)

        assertThat(found).isNotNull
        assertThat(found!!.id).isEqualTo(inquiry.id)
    }

    @Test
    fun `findByIdAndDeletedAtIsNull - 삭제된 문의는 제외된다`() {
        val inquiry = inquiryRepository.save(createInquiry())
        inquiry.delete(inquiry.userId)
        inquiryRepository.save(inquiry)

        val found =
            inquiryRepository.findByIdAndDeletedAtIsNull(inquiry.id)

        assertThat(found).isNull()
    }

    @Test
    fun `findAllByDeletedAtIsNullOrderByCreatedAtDesc`() {
        val older = inquiryRepository.save(createInquiry())
        val newer = inquiryRepository.save(createInquiry())

        entityManager.flush()

        updateCreatedAt(older.id, LocalDateTime.now().minusDays(2))
        updateCreatedAt(newer.id, LocalDateTime.now().minusDays(1))

        val found = inquiryRepository.findAllByDeletedAtIsNullOrderByCreatedAtDesc()

        assertThat(found.map { it.id }).containsSubsequence(
            newer.id,
            older.id,
        )
    }

    @Test
    fun `findAllByUserIdAndDeletedAtIsNullAndCreatedAtAfterOrderByCreatedAtDesc - 기준일 이전 문의는 제외된다`() {
        val userId = UUID.randomUUID()
        val cutoff = LocalDateTime.now().minusYears(1)

        val recent = inquiryRepository.save(createInquiry(userId))
        val expired = inquiryRepository.save(createInquiry(userId))
        val otherUserRecent = inquiryRepository.save(createInquiry(UUID.randomUUID()))

        entityManager.flush()

        updateCreatedAt(recent.id, LocalDateTime.now().minusMonths(6))
        updateCreatedAt(expired.id, LocalDateTime.now().minusYears(1).minusDays(1))
        updateCreatedAt(otherUserRecent.id, LocalDateTime.now().minusMonths(3))

        val found =
            inquiryRepository.findAllByUserIdAndDeletedAtIsNullAndCreatedAtAfterOrderByCreatedAtDesc(
                userId,
                cutoff,
            )

        assertThat(found.map { it.id }).containsExactly(recent.id)
    }

    private fun updateCreatedAt(
        inquiryId: UUID,
        createdAt: LocalDateTime,
    ) {
        entityManager
            .createQuery("UPDATE Inquiry i SET i.createdAt = :createdAt WHERE i.id = :id")
            .setParameter("createdAt", createdAt)
            .setParameter("id", inquiryId)
            .executeUpdate()

        entityManager.flush()
        entityManager.clear()
    }

    private fun createInquiry(userId: UUID = UUID.randomUUID()): Inquiry {
        val request =
            InquiryRegisterRequest(
                userId = userId,
                email = "test@test.com",
                type = InquiryType.BUG,
                typeEtc = null,
                content = "문의 내용입니다.",
                isAgreed = true,
            )

        return Inquiry.register(
            request = request,
            userId = userId,
            email = request.email,
        )
    }
}

package uk.gov.communities.prsdb.webapp.services

import jakarta.transaction.Transactional
import org.springframework.security.crypto.password.PasswordEncoder
import uk.gov.communities.prsdb.webapp.annotations.webAnnotations.PrsdbWebService
import uk.gov.communities.prsdb.webapp.database.entity.LettingAgentAccess
import uk.gov.communities.prsdb.webapp.database.repository.LettingAgentAccessRepository
import uk.gov.communities.prsdb.webapp.exceptions.PrsdbWebException

@PrsdbWebService
class LettingAgentPasswordService(
    private val lettingAgentAccessRepository: LettingAgentAccessRepository,
    private val passwordEncoder: PasswordEncoder,
) {
    @Transactional
    fun setPassword(
        lettingAgentAccessId: Long,
        rawPassword: String,
    ) {
        if (rawPassword.isBlank()) {
            throw PrsdbWebException("Password must not be blank")
        }

        val lettingAgentAccess =
            lettingAgentAccessRepository.findById(lettingAgentAccessId).orElseThrow {
                PrsdbWebException("Letting agent access $lettingAgentAccessId not found")
            }

        if (lettingAgentAccess.encodedPassword != null) {
            throw PrsdbWebException("Password has already been set for letting agent access $lettingAgentAccessId")
        }

        val encoded = passwordEncoder.encode(rawPassword)

        val updatedRows = lettingAgentAccessRepository.setEncodedPasswordIfAbsent(lettingAgentAccessId, encoded)
        if (updatedRows == 0) {
            throw PrsdbWebException("Password has already been set for letting agent access $lettingAgentAccessId")
        }
    }

    fun isPasswordCorrect(
        lettingAgentAccess: LettingAgentAccess,
        rawPassword: String,
    ): Boolean {
        val stored =
            lettingAgentAccess.encodedPassword
                ?: throw PrsdbWebException("No password has been set for letting agent access ${lettingAgentAccess.id}")
        return passwordEncoder.matches(rawPassword, stored)
    }

    fun hasPasswordBeenSet(lettingAgentAccess: LettingAgentAccess): Boolean = lettingAgentAccess.encodedPassword != null
}

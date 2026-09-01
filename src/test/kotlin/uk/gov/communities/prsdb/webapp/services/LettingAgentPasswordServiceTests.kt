package uk.gov.communities.prsdb.webapp.services

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.crypto.password.PasswordEncoder
import uk.gov.communities.prsdb.webapp.database.repository.LettingAgentAccessRepository
import uk.gov.communities.prsdb.webapp.exceptions.PrsdbWebException
import uk.gov.communities.prsdb.webapp.testHelpers.mockObjects.MockLettingAgentData
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class LettingAgentPasswordServiceTests {
    @Mock
    private lateinit var lettingAgentAccessRepository: LettingAgentAccessRepository

    @Mock
    private lateinit var passwordEncoder: PasswordEncoder

    @InjectMocks
    private lateinit var lettingAgentPasswordService: LettingAgentPasswordService

    private val defaultId = MockLettingAgentData.DEFAULT_LETTING_AGENT_ACCESS_ID

    @Test
    fun `setPassword rejects a blank password`() {
        assertThrows<PrsdbWebException> {
            lettingAgentPasswordService.setPassword(defaultId, "   ")
        }

        verify(passwordEncoder, never()).encode(any())
    }

    @Test
    fun `setPassword throws when letting agent access not found`() {
        whenever(lettingAgentAccessRepository.findById(defaultId)).thenReturn(Optional.empty())

        assertThrows<PrsdbWebException> {
            lettingAgentPasswordService.setPassword(defaultId, "newPassword")
        }
    }

    @Test
    fun `setPassword rejects when password already set on entity`() {
        val access = MockLettingAgentData.createLettingAgentAccess(encodedPassword = "{bcrypt}existing")
        whenever(lettingAgentAccessRepository.findById(defaultId)).thenReturn(Optional.of(access))

        assertThrows<PrsdbWebException> {
            lettingAgentPasswordService.setPassword(defaultId, "newPassword")
        }

        verify(passwordEncoder, never()).encode(any())
    }

    @Test
    fun `setPassword encodes and persists the password`() {
        val access = MockLettingAgentData.createLettingAgentAccessWithoutPassword()
        whenever(lettingAgentAccessRepository.findById(defaultId)).thenReturn(Optional.of(access))
        whenever(passwordEncoder.encode("myPassword")).thenReturn("{bcrypt}encoded")
        whenever(
            lettingAgentAccessRepository.setEncodedPasswordIfAbsent(eq(defaultId), eq("{bcrypt}encoded")),
        ).thenReturn(1)

        lettingAgentPasswordService.setPassword(defaultId, "myPassword")

        verify(passwordEncoder).encode("myPassword")
        verify(lettingAgentAccessRepository).setEncodedPasswordIfAbsent(defaultId, "{bcrypt}encoded")
    }

    @Test
    fun `setPassword throws when atomic update changes zero rows`() {
        val access = MockLettingAgentData.createLettingAgentAccessWithoutPassword()
        whenever(lettingAgentAccessRepository.findById(defaultId)).thenReturn(Optional.of(access))
        whenever(passwordEncoder.encode("myPassword")).thenReturn("{bcrypt}encoded")
        whenever(lettingAgentAccessRepository.setEncodedPasswordIfAbsent(any(), any())).thenReturn(0)

        assertThrows<PrsdbWebException> {
            lettingAgentPasswordService.setPassword(defaultId, "myPassword")
        }
    }

    @Test
    fun `isPasswordCorrect returns true for matching password`() {
        val access = MockLettingAgentData.createLettingAgentAccess(encodedPassword = "{bcrypt}stored")
        whenever(passwordEncoder.matches("candidate", "{bcrypt}stored")).thenReturn(true)

        assertTrue(lettingAgentPasswordService.isPasswordCorrect(access, "candidate"))
    }

    @Test
    fun `isPasswordCorrect returns false for non-matching password`() {
        val access = MockLettingAgentData.createLettingAgentAccess(encodedPassword = "{bcrypt}stored")
        whenever(passwordEncoder.matches("wrong", "{bcrypt}stored")).thenReturn(false)

        assertFalse(lettingAgentPasswordService.isPasswordCorrect(access, "wrong"))
    }

    @Test
    fun `isPasswordCorrect throws when no password has been set`() {
        val access = MockLettingAgentData.createLettingAgentAccessWithoutPassword()

        assertThrows<PrsdbWebException> {
            lettingAgentPasswordService.isPasswordCorrect(access, "candidate")
        }
    }

    @Test
    fun `hasPasswordBeenSet returns true when the encoded password is present`() {
        val access = MockLettingAgentData.createLettingAgentAccess(encodedPassword = "{bcrypt}stored")

        assertTrue(lettingAgentPasswordService.hasPasswordBeenSet(access))
    }

    @Test
    fun `hasPasswordBeenSet returns false when no password has been set`() {
        val access = MockLettingAgentData.createLettingAgentAccessWithoutPassword()

        assertFalse(lettingAgentPasswordService.hasPasswordBeenSet(access))
    }
}

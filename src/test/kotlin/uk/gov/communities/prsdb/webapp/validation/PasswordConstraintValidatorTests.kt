package uk.gov.communities.prsdb.webapp.validation

import org.junit.jupiter.api.Named
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PasswordConstraintValidatorTests {
    companion object {
        @JvmStatic
        fun provideValidPasswords() =
            arrayOf(
                Named.of("letters and digit", "password1"),
                Named.of("digits and letter at end", "1234567a"),
                Named.of("letters digits and symbols", "abc123!!"),
                Named.of("letters digits and spaces", "ab cd 123"),
            )

        @JvmStatic
        fun provideInvalidPasswords() =
            arrayOf(
                Named.of("too short with digit", "short1"),
                Named.of("only letters", "onlyletters"),
                Named.of("only digits", "12345678"),
                Named.of("only spaces", "        "),
                Named.of("null", null),
            )
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideValidPasswords")
    fun `isValid returns true when`(input: String) {
        assertTrue(PasswordConstraintValidator().isValid(input))
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideInvalidPasswords")
    fun `isValid returns false when`(input: String?) {
        assertFalse(PasswordConstraintValidator().isValid(input))
    }
}

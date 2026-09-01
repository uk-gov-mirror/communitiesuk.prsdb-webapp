package uk.gov.communities.prsdb.webapp.validation

class PasswordConstraintValidator : PropertyConstraintValidator {
    override fun isValid(value: Any?): Boolean =
        value is String && value.length >= 8 &&
            value.any { it.isLetter() } &&
            value.any { it.isDigit() }
}

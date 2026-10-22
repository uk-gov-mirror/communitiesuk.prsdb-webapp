package uk.gov.communities.prsdb.webapp.constants

// Feature flags defined in application.yml should have a corresponding constant for their names here.
// They should also be added to the featureFlagNames list.
//
// Feature flags in code should be referred to using these constants.
//
// When loading the feature flag configuration, the application will check that all feature flag names
// defined in application.yml are included in this list.

const val FAILOVER_TEST_ENDPOINTS = "failover-test-endpoints"

const val SUBJECT_IDENTIFIER_PAGE = "subject-identifier-page"

const val PASSWORD_BENCHMARK_ENDPOINT = "pdjb-1664-password-benchmark-endpoint"

const val PAYMENTS = "pdjb-1009-payments"

const val PROPERTY_REGISTRATION_PHASE_TWO = "pdjb-939-property-registration-phase-two"

const val MULTI_USER_ORGANISATIONS = "pdjb-1210-multi-user-organisations"

const val GATLING_POST_ENDPOINT = "pdjb-238-gatling-post-endpoint"

const val GAS_CERTIFICATE_14_MONTH_EXPIRY = "pdjb-1894-gas-certificate-14-month-expiry"

val featureFlagNames =
    listOf(
        FAILOVER_TEST_ENDPOINTS,
        SUBJECT_IDENTIFIER_PAGE,
        PASSWORD_BENCHMARK_ENDPOINT,
        PAYMENTS,
        PROPERTY_REGISTRATION_PHASE_TWO,
        MULTI_USER_ORGANISATIONS,
        GATLING_POST_ENDPOINT,
        GAS_CERTIFICATE_14_MONTH_EXPIRY,
    )

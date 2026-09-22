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

const val PROPERTY_REGISTRATION_RESTRUCTURE_AND_SKIPPING = "pdjb-939-property-registration-restructure-and-skipping"

const val DASHBOARD_NAV_LINK = "pdjb-1053-dashboard-nav-link"

const val DELEGATE_TO_LETTING_AGENT = "pdjb-1022-delegate-to-letting-agent"

const val PASSWORD_BENCHMARK_ENDPOINT = "pdjb-1664-password-benchmark-endpoint"

const val CORRESPONDENCE_ADDRESS = "pdjb-1040-correspondence-address"

const val PAYMENTS = "pdjb-1009-payments"

const val PROPERTY_REGISTRATION_PHASE_TWO = "pdjb-939-property-registration-phase-two"

val featureFlagNames =
    listOf(
        FAILOVER_TEST_ENDPOINTS,
        SUBJECT_IDENTIFIER_PAGE,
        PROPERTY_REGISTRATION_RESTRUCTURE_AND_SKIPPING,
        DASHBOARD_NAV_LINK,
        DELEGATE_TO_LETTING_AGENT,
        PASSWORD_BENCHMARK_ENDPOINT,
        CORRESPONDENCE_ADDRESS,
        PAYMENTS,
        PROPERTY_REGISTRATION_PHASE_TWO,
    )

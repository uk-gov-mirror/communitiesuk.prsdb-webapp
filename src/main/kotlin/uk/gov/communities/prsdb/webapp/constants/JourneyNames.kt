package uk.gov.communities.prsdb.webapp.constants

// These need to be constants as we cannot use the urlPathSegment from the JourneyType directly in the RequestMapping Annotation
const val REGISTER_LANDLORD_JOURNEY_URL: String = "register-as-a-landlord"
const val DEREGISTER_LANDLORD_JOURNEY_URL: String = "deregister-landlord"
const val DEREGISTER_ORGANISATIONAL_LANDLORD_JOURNEY_URL: String = "deregister-organisational-landlord"
const val REGISTER_LOCAL_COUNCIL_USER_JOURNEY_URL: String = "register-local-council-user"
const val REGISTER_PROPERTY_JOURNEY_URL: String = "register-property"
const val DEREGISTER_PROPERTY_JOURNEY_URL: String = "deregister-property"
const val DEACTIVATE_PROPERTY_JOURNEY_URL: String = "deactivate-property"
const val REACTIVATE_PROPERTY_JOURNEY_URL: String = "reactivate-property"
const val CANCEL_JOINT_LANDLORD_INVITATION_JOURNEY_URL: String = "cancel-joint-landlord-invitation"
const val SWITCH_TO_INDIVIDUAL_JOURNEY_URL: String = "switch-to-individual"
const val LEAVE_PROPERTY_JOURNEY_URL: String = "leave-property"
const val DELEGATE_TO_LETTING_AGENT_JOURNEY_URL: String = "delegate-to-letting-agent"

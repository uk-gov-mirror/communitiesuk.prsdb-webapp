package uk.gov.communities.prsdb.webapp.performance

import io.gatling.javaapi.core.ChainBuilder
import io.gatling.javaapi.core.CoreDsl.css
import io.gatling.javaapi.core.CoreDsl.exec
import io.gatling.javaapi.http.HttpDsl.http
import io.gatling.javaapi.http.HttpDsl.status

object OperatorJourney {
    val operationRequestNames =
        listOf(
            "Operator: dashboard",
            "Operator: council administrators",
            "Operator: invitation form",
            "Council access 1: persisted role",
            "Council access 2: persisted role",
            "Council invitation: confirmation",
            "Council invitation: persisted cancellation",
            "Council access removal: persisted deletion",
        )

    fun operations(
        config: BasicRunConfig,
        fixture: OperatorFixture,
        prefix: String = "",
    ): ChainBuilder =
        dashboard(config, prefix)
            .exec(
                CouncilHttpForms.accessChange(
                    config,
                    "/system-operator/edit-admin/${fixture.targetId}",
                    "/system-operator/manage-local-council-admins",
                    fixture.targetName,
                    fixture.targetManager,
                    prefix,
                ),
            )
            .exec(CouncilManagementJourney.invitation(config, fixture.councilId, prefix))
            .exec(
                CouncilManagementJourney.accessRemoval(
                    config,
                    fixture.targetId,
                    fixture.targetName,
                    prefix,
                ),
            )

    fun dashboard(
        config: BasicRunConfig,
        prefix: String = "",
    ): ChainBuilder =
        exec(
            http("${prefix}Operator: dashboard").get("/system-operator/dashboard").disableFollowRedirect()
                .check(status().`is`(200))
                .check(css("a[href='/system-operator/invite-lc-admin']").exists())
                .check(css("a[href='/system-operator/metrics']").exists()),
        ).exitHereIfFailed()
            .exec(
                http("${prefix}Operator: council administrators").get("/system-operator/manage-local-council-admins")
                    .disableFollowRedirect().check(status().`is`(200))
                    .check(css("a[href='/system-operator/invite-lc-admin'], a[href='invite-lc-admin']").exists()),
            ).exitHereIfFailed()
            .exec(
                CouncilHttpForms.form(
                    config,
                    "${prefix}Operator: invitation form",
                    "/system-operator/invite-lc-admin",
                    "/system-operator/invite-lc-admin",
                ),
            )
}

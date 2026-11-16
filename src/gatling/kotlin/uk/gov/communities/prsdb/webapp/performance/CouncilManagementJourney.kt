package uk.gov.communities.prsdb.webapp.performance

import io.gatling.javaapi.core.ChainBuilder
import io.gatling.javaapi.core.CoreDsl.css
import io.gatling.javaapi.core.CoreDsl.doIf
import io.gatling.javaapi.core.CoreDsl.exec
import io.gatling.javaapi.core.CoreDsl.repeat
import io.gatling.javaapi.http.HttpDsl.header
import io.gatling.javaapi.http.HttpDsl.http
import io.gatling.javaapi.http.HttpDsl.status
import java.net.URI
import java.util.UUID

object CouncilManagementJourney {
    fun invitation(
        config: BasicRunConfig,
        councilId: Long,
        prefix: String = "",
    ): ChainBuilder {
        val invite = "/system-operator/invite-lc-admin"
        val email = "000-gatling-${UUID.randomUUID()}@example.com"
        val fields =
            mapOf("email" to email, "confirmEmail" to email, "localCouncilId" to councilId.toString())
        return CouncilHttpForms.form(config, "${prefix}Council invitation: form", invite, invite)
            .exec(CouncilHttpForms.submit(config, "${prefix}Council invitation: send", fields, "$invite/confirmation"))
            .exec(
                http("${prefix}Council invitation: confirmation").get("#{councilNextUrl}")
                    .queryParam("perfCacheBuster") { UUID.randomUUID().toString() }.disableFollowRedirect()
                    .check(status().`is`(200)).check(css("main").transform { it.contains(email) }.`is`(true)),
            ).exitHereIfFailed()
            .exec(cancelInvitation(config, councilId, email, prefix))
    }

    private fun cancelInvitation(
        config: BasicRunConfig,
        councilId: Long,
        email: String,
        prefix: String = "",
    ): ChainBuilder {
        require(email.matches(Regex("000-gatling-[a-zA-Z0-9-]+@example\\.com"))) { "Only synthetic Gatling invitations may be recovered" }
        val manage = "/system-operator/$councilId/manage-users"
        return exec { it.remove("councilInvitationCancel") }
            .exec(
                repeat(20, "councilInvitationPage").on(
                    doIf { !it.contains("councilInvitationCancel") }.then(
                        exec(
                            http("${prefix}Council invitation: persisted pending account").get(manage)
                                .queryParam("perfCacheBuster") { UUID.randomUUID().toString() }
                                .queryParam("page") { it.getInt("councilInvitationPage") + 1 }.disableFollowRedirect()
                                .check(status().`is`(200))
                                .check(
                                    css("a:contains('$email')", "href").optional()
                                        .saveAs("councilInvitationCancel"),
                                ),
                        ).exitHereIfFailed(),
                    ),
                ),
            )
            .exec { session ->
                val location =
                    HttpDestination.resolve(
                        checkNotNull(session.getString("councilInvitationCancel")) { "Invitation was not found in 20 management pages" },
                        config.baseUrl,
                        config.baseUrl + manage,
                    )
                val path = URI.create(location).path
                val id = path.substringAfterLast('/').toLongOrNull()
                require(id != null && id > 0 && path.contains("/cancel-invitation/")) { "Unexpected cancellation link" }
                session.set("councilInvitationCancelPath", "/system-operator/cancel-invitation/$id")
            }
            .exec(
                http("${prefix}Council invitation: cancellation form").get("#{councilInvitationCancelPath}")
                    .queryParam("perfCacheBuster") { UUID.randomUUID().toString() }
                    .disableFollowRedirect().check(status().`is`(200))
                    .check(css("main").transform { it.contains(email) }.`is`(true))
                    .check(
                        css("form", "action").find().transformWithSession { action, session ->
                            val path = session.getString("councilInvitationCancelPath")!!
                            CouncilHttpForms.destination(action, config, path)
                        }.saveAs("councilFormAction"),
                    )
                    .check(css("form input[type=hidden]", "name").findAll().saveAs("councilHiddenNames"))
                    .check(css("form input[type=hidden]", "value").findAll().saveAs("councilHiddenValues"))
                    .check(css("form input[name=_csrf]", "value").exists()),
            ).exitHereIfFailed().pause(config.thinkTime)
            .exec(
                http("${prefix}Council invitation: cancel").post("#{councilFormAction}").disableFollowRedirect()
                    .formParamMap { session ->
                        val names = session.getList<String>("councilHiddenNames")
                        val values = session.getList<String>("councilHiddenValues")
                        require(names.size == values.size)
                        names.zip(values).toMap()
                    }
                    .check(status().`is`(302))
                    .check(
                        header("Location").find().transformWithSession { location, session ->
                            val path = session.getString("councilInvitationCancelPath")!!
                            CouncilHttpForms.destination(location, config, "$path/confirmation", config.baseUrl + path)
                        }.saveAs("councilNextUrl"),
                    ),
            ).exitHereIfFailed()
            .exec(
                http("${prefix}Council invitation: cancellation confirmation").get("#{councilNextUrl}")
                    .queryParam("perfCacheBuster") { UUID.randomUUID().toString() }.disableFollowRedirect()
                    .check(status().`is`(200)).check(css("main").transform { it.contains(email) }.`is`(true)),
            ).exitHereIfFailed()
            .exec(
                http("${prefix}Council invitation: persisted cancellation").get("#{councilInvitationCancelPath}")
                    .queryParam("perfCacheBuster") { UUID.randomUUID().toString() }
                    .disableFollowRedirect().check(status().`is`(404)),
            ).exitHereIfFailed()
    }

    fun accessRemoval(
        config: BasicRunConfig,
        targetId: Long,
        targetName: String,
        prefix: String = "",
    ): ChainBuilder {
        val delete = "/system-operator/delete-admin/$targetId"
        val edit = "/system-operator/edit-admin/$targetId"
        return CouncilHttpForms.form(config, "${prefix}Council access removal: confirm", delete, delete, targetName)
            .exec(CouncilHttpForms.submit(config, "${prefix}Council access removal: delete", emptyMap(), "$delete/confirmation"))
            .exec(
                http("${prefix}Council access removal: confirmation").get("#{councilNextUrl}")
                    .queryParam("perfCacheBuster") { UUID.randomUUID().toString() }.disableFollowRedirect()
                    .check(status().`is`(200)).check(css("main").transform { it.contains(targetName) }.`is`(true)),
            ).exitHereIfFailed()
            .exec(
                http("${prefix}Council access removal: persisted deletion").get(edit)
                    .queryParam("perfCacheBuster") { UUID.randomUUID().toString() }.disableFollowRedirect()
                    .check(status().`is`(404)),
            ).exitHereIfFailed()
    }
}

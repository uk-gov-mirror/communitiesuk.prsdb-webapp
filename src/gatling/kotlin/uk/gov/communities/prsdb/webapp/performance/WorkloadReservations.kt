package uk.gov.communities.prsdb.webapp.performance

internal class WorkloadReservations {
    private val subjects = mutableSetOf<String>()
    private val properties = mutableSetOf<Long>()
    private val organisations = mutableSetOf<Long>()

    fun reserve(
        subjects: List<String> = emptyList(),
        properties: List<Long> = emptyList(),
        organisations: List<Long> = emptyList(),
    ) {
        require(subjects.all { this.subjects.add(it) }) { "Concurrent workload families share a mutable actor" }
        require(properties.all { this.properties.add(it) }) { "Concurrent workload families share a mutable property" }
        require(organisations.all { this.organisations.add(it) }) { "Concurrent workload families share a mutable organisation" }
    }
}

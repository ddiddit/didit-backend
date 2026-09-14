package com.didit.deploy

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.yaml.snakeyaml.Yaml
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path

class MonitoringDomainConfigTest {
    @Test
    fun `Grafana public URL uses the monitoring host served by Nginx`() {
        val monitoringHost = monitoringHost()
        val environment = grafanaEnvironment()

        assertThat(environment["GF_SERVER_DOMAIN"]).isEqualTo(monitoringHost)
        assertThat(environment["GF_SERVER_ROOT_URL"]).isEqualTo("https://$monitoringHost")
    }

    @Test
    fun `Grafana alert links use the monitoring host served by Nginx`() {
        val monitoringHost = monitoringHost()
        val exploreUrls = findValues(loadYaml(GRAFANA_RULES), "explore_url").map { it.toString() }

        assertThat(exploreUrls).isNotEmpty
        assertThat(exploreUrls.map { URI(it).host }).containsOnly(monitoringHost)
    }

    private fun monitoringHost(): String {
        val nginxConfig = Files.readString(MONITORING_NGINX)
        val serverNames =
            SERVER_NAME
                .findAll(nginxConfig)
                .map { it.groupValues[1].trim() }
                .toSet()

        return requireNotNull(serverNames.singleOrNull()) {
            "Monitoring Nginx config must declare one consistent server_name, but found $serverNames"
        }
    }

    private fun grafanaEnvironment(): Map<String, String> {
        val compose = loadYaml(MONITORING_COMPOSE).asStringMap()
        val services = compose.getValue("services").asStringMap()
        val grafana = services.getValue("grafana").asStringMap()
        val environment = grafana.getValue("environment") as List<*>

        return environment
            .map { it.toString().split("=", limit = 2) }
            .associate { (key, value) -> key to value }
    }

    private fun findValues(
        node: Any?,
        key: String,
    ): List<Any?> =
        when (node) {
            is Map<*, *> ->
                node.entries.flatMap { (entryKey, value) ->
                    if (entryKey == key) listOf(value) else findValues(value, key)
                }
            is Iterable<*> -> node.flatMap { findValues(it, key) }
            else -> emptyList()
        }

    private fun loadYaml(path: Path): Any = Files.newInputStream(path).use(Yaml()::load)

    private fun Any.asStringMap(): Map<String, Any> {
        @Suppress("UNCHECKED_CAST")
        return this as Map<String, Any>
    }

    companion object {
        private val MONITORING_NGINX = Path.of("deploy/prod/monitoring-didit.conf")
        private val MONITORING_COMPOSE = Path.of("deploy/prod/docker-compose.monitoring.yaml")
        private val GRAFANA_RULES = Path.of("deploy/prod/grafana/provisioning/alerting/rules.yml")
        private val SERVER_NAME = Regex("""(?m)^\s*server_name\s+([^;#]+?)\s*;""")
    }
}

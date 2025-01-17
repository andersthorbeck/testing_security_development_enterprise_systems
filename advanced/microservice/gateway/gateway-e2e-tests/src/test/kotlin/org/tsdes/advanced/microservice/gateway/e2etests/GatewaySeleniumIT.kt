package org.tsdes.advanced.microservice.gateway.e2etests

import org.awaitility.Awaitility
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.openqa.selenium.chrome.ChromeOptions
import org.testcontainers.containers.BrowserWebDriverContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

/**
 * Created by arcuri82 on 04-Oct-19.
 */

@Disabled
@Testcontainers
class GatewaySeleniumIT : GatewayIntegrationDockerTestBase() {


    /*
      To run Selenium tests, I need a browser and drivers for it, which will
      depend on the operating system. What about starting it from a Docker?
      Yes! That works great. Only caveat is that it would be on a different network,
      and so Selenium would not be able to interact with the gateway.
      Adding it in the docker-compose.yml would be wrong, as we do not need it in
      production.
      So, the approach here is to start it in its own Docker, and then make sure
      such Docker image share the same network of the Docker Compose one.

      TODO: Unfortunately this still does not work in TestContainer :(
      https://github.com/testcontainers/testcontainers-java/issues/915
   */



    @Container
    @JvmField
    val browser = KBrowserWebDriverContainer()
            .withCapabilities(ChromeOptions())
            .withNetworkMode("gateway-network") //same as in docker-compose.yml
            //record videos if tests fail
            .withRecordingMode(BrowserWebDriverContainer.VncRecordingMode.RECORD_FAILING,
                    Paths.get(".","target").toFile())


    class KBrowserWebDriverContainer : BrowserWebDriverContainer<KBrowserWebDriverContainer>()

    @Test
    fun testIntegration() {

        val po = IndexPageObject(browser.webDriver)

        po.goToPage("scg", 8080)

        assertTrue(po.isOnPage())

        po.deleteMessages()
        assertEquals(0, po.numberOfMessages())

        po.sendMessage("Hail Zuul!!!")
        assertEquals(1, po.numberOfMessages())

        po.sendMessage("Just kidding")
        assertEquals(2, po.numberOfMessages())

        po.deleteMessages()
        assertEquals(0, po.numberOfMessages())
    }

    @Test
    fun testLoadBalance() {

        val po = IndexPageObject(browser.webDriver)

        po.goToPage("scg", 8080)
        assertTrue(po.isOnPage())

        po.deleteMessages()
        assertEquals(0, po.numberOfMessages())


        Awaitility.await().atMost(120, TimeUnit.SECONDS)
                .ignoreExceptions()
                .until{
                    (0..3).forEach { po.sendMessage("foo") }

                    val messages = po.messages()

                    assertEquals(3, messages.toSet().size)

                    assertTrue(messages.any { it.startsWith("A :") })
                    assertTrue(messages.any { it.startsWith("B :") })
                    assertTrue(messages.any { it.startsWith("C :") })

                    true
                }

        po.deleteMessages()
    }
}
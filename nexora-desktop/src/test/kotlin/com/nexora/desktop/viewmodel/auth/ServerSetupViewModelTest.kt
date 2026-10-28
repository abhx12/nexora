package com.nexora.desktop.viewmodel.auth

import com.nexora.desktop.support.vmTest
import com.nexora.desktop.util.SettingsStore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class ServerSetupViewModelTest {

    @TempDir
    lateinit var dir: Path

    @Test
    @DisplayName("NFR-SEC-01 http address only allowed for localhost")
    fun httpAddress_onlyAllowedForLocalhost() {
        assertNull(ServerSetupViewModel.addressProblem("https://nexora.example.com"))
        assertNull(ServerSetupViewModel.addressProblem("http://localhost:8088"))
        assertNull(ServerSetupViewModel.addressProblem("http://127.0.0.1:8088"))
        assertNull(ServerSetupViewModel.addressProblem("http://localhost:8080"))
        assertNull(ServerSetupViewModel.addressProblem("http://127.0.0.1:8080"))
        assertNotNull(ServerSetupViewModel.addressProblem("http://nexora.example.com"))
        assertNotNull(ServerSetupViewModel.addressProblem("http://192.168.1.20:8080"))
        assertNotNull(ServerSetupViewModel.addressProblem("nexora.example.com"))
        assertNotNull(ServerSetupViewModel.addressProblem("ftp://nexora.example.com"))
    }

    @Test
    @DisplayName("default state initializes to local engine mode at port 8088")
    fun defaultState_initializesToLocalEngineMode() = vmTest { server ->
        val vm = ServerSetupViewModel(server.client(), SettingsStore(dir.resolve("settings.json")))
        assertEquals(ServerMode.LOCAL, vm.state.value.mode)
        assertEquals(ServerSetupViewModel.DEFAULT_LOCAL_URL, vm.state.value.address)

        vm.selectMode(ServerMode.REMOTE)
        assertEquals(ServerMode.REMOTE, vm.state.value.mode)
        assertEquals("", vm.state.value.address)

        vm.selectMode(ServerMode.LOCAL)
        assertEquals(ServerMode.LOCAL, vm.state.value.mode)
        assertEquals(ServerSetupViewModel.DEFAULT_LOCAL_URL, vm.state.value.address)
    }


    @Test
    @DisplayName("NFR-SEC-01 http address is rejected before any request is sent")
    fun httpAddress_isRejectedWithoutRequest() = vmTest { server ->
        val vm = ServerSetupViewModel(server.client(), SettingsStore(dir.resolve("settings.json")))
        vm.onAddressChange("http://nexora.example.com")
        vm.testConnection()
        testScheduler.advanceUntilIdle()
        assertNotNull(vm.state.value.addressError)
        assertTrue(server.requests.isEmpty())
    }

    @Test
    @DisplayName("api major version mismatch blocks login")
    fun apiMajorVersionMismatch_blocksLogin() = vmTest { server ->
        server.onJson("GET /actuator/health", """{"status":"UP"}""")
        server.onJson("GET /actuator/info", """{"app":{"name":"nexora","api-version":"2"},"build":{"version":"2.0.0"}}""")
        val settings = SettingsStore(dir.resolve("settings.json"))
        val vm = ServerSetupViewModel(server.client(), settings)

        vm.onAddressChange("https://nexora.example.com")
        vm.testConnection()
        testScheduler.advanceUntilIdle()

        assertFalse(vm.state.value.canContinue)
        assertEquals("This app version (1) does not match the server (2). Install the matching version.", vm.state.value.error)
        assertFalse(vm.saveAndContinue())
        assertNull(settings.current.serverUrl)
    }

    @Test
    @DisplayName("matching api version enables continue and stores the address")
    fun matchingApiVersion_storesAddress() = vmTest { server ->
        server.onJson("GET /actuator/health", """{"status":"UP"}""")
        server.onJson("GET /actuator/info", """{"app":{"name":"nexora","api-version":1},"build":{"version":"0.0.1-SNAPSHOT"}}""")
        val settings = SettingsStore(dir.resolve("settings.json"))
        val vm = ServerSetupViewModel(server.client(), settings)

        vm.onAddressChange("https://nexora.example.com/")
        vm.testConnection()
        testScheduler.advanceUntilIdle()

        assertTrue(vm.state.value.canContinue)
        assertEquals("0.0.1-SNAPSHOT", vm.state.value.serverVersion)
        assertTrue(vm.saveAndContinue())
        assertEquals("https://nexora.example.com", SettingsStore(dir.resolve("settings.json")).current.serverUrl)
    }

    @Test
    @DisplayName("info without api version is not a smartsilk server")
    fun infoWithoutApiVersion_isRejected() = vmTest { server ->
        server.onJson("GET /actuator/health", """{"status":"UP"}""")
        server.onJson("GET /actuator/info", """{}""")
        val vm = ServerSetupViewModel(server.client(), SettingsStore(dir.resolve("settings.json")))
        vm.onAddressChange("https://other.example.com")
        vm.testConnection()
        testScheduler.advanceUntilIdle()
        assertEquals("Not a SmartSilk server.", vm.state.value.error)
    }

    @Test
    @DisplayName("findServerPackageDir detects smartsilk-server directory")
    fun findServerPackageDir_detectsSmartsilkServer() {
        val serverDir = com.nexora.desktop.util.LocalEngineManager.findServerPackageDir()
        assertNotNull(serverDir)
        assertTrue(serverDir!!.isDirectory)
        assertTrue(java.io.File(serverDir, "docker-compose.yml").exists())
    }
}

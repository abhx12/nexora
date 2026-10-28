package com.nexora.desktop.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.util.concurrent.TimeUnit

object LocalEngineManager {

    private const val LOCAL_HEALTH_URL = "http://localhost:8088/actuator/health"
    private var engineProcess: Process? = null
    private var startedServerPackageDir: File? = null

    init {
        // Ensure the engine is cleanly shut down when the desktop app is closed
        Runtime.getRuntime().addShutdownHook(Thread {
            stopEngine()
        })
    }

    /**
     * Checks if the local engine is already up and healthy on port 8088.
     */
    suspend fun isEngineRunning(): Boolean = withContext(Dispatchers.IO) {
        try {
            val uri = URI(LOCAL_HEALTH_URL)
            val connection = uri.toURL().openConnection() as HttpURLConnection
            connection.connectTimeout = 1000
            connection.readTimeout = 1000
            connection.requestMethod = "GET"
            val responseCode = connection.responseCode
            responseCode in 200..299
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Finds the standalone server package directory containing Docker compose/scripts.
     */
    fun findServerPackageDir(): File? {
        val envDir = System.getenv("SMARTSILK_SERVER_DIR")
        if (!envDir.isNullOrBlank()) {
            val dir = File(envDir)
            if (isValidServerPackage(dir)) return dir
        }

        // 1. Compose application resources directory (set in production native distributions)
        val composeResourcesDir = System.getProperty("compose.application.resources.dir")
        if (!composeResourcesDir.isNullOrBlank()) {
            listOf(
                File(composeResourcesDir, "smartsilk-server"),
                File(composeResourcesDir, "server")
            ).firstOrNull { isValidServerPackage(it) }?.let { return it }
        }

        // 2. Relative to application installation root (MSI / DEB / portable)
        val javaHome = System.getProperty("java.home")
        if (!javaHome.isNullOrBlank()) {
            val parent = File(javaHome).parentFile
            if (parent != null) {
                listOf(
                    File(parent, "smartsilk-server"),
                    File(parent, "server"),
                    File(parent, "app/smartsilk-server"),
                    File(parent, "../smartsilk-server"),
                    File(parent, "../../smartsilk-server")
                ).firstOrNull { isValidServerPackage(it) }?.let { return it }
            }
        }

        // 3. User working directory and common relative paths
        val userDir = System.getProperty("user.dir", ".")
        listOf(
            File("smartsilk-server"),
            File("../smartsilk-server"),
            File("../../smartsilk-server"),
            File(userDir, "smartsilk-server"),
            File(userDir, "../smartsilk-server"),
            File("server"),
            File("../server")
        ).firstOrNull { isValidServerPackage(it) }?.let { return it }

        // 4. Standard user home directories
        val userHome = System.getProperty("user.home")
        if (!userHome.isNullOrBlank()) {
            listOf(
                File(userHome, "smartsilk-server"),
                File(userHome, ".local/share/smartsilk/smartsilk-server"),
                File(userHome, ".config/smartsilk/smartsilk-server")
            ).firstOrNull { isValidServerPackage(it) }?.let { return it }
        }

        val localAppData = System.getenv("LOCALAPPDATA")
        if (!localAppData.isNullOrBlank()) {
            val dir = File(localAppData, "SmartSilk/smartsilk-server")
            if (isValidServerPackage(dir)) return dir
        }

        return null
    }

    private fun isValidServerPackage(dir: File?): Boolean {
        if (dir == null || !dir.exists() || !dir.isDirectory) return false
        return File(dir, "docker-compose.yml").exists() ||
                File(dir, "start-server.sh").exists() ||
                File(dir, "start-server.bat").exists()
    }

    /**
     * Finds the backend jar file. Checks production installed locations as well as
     * development directories.
     */
    fun findBackendJar(): File? {
        // 1. Compose application resources directory (set in production native distributions)
        val composeResourcesDir = System.getProperty("compose.application.resources.dir")
        if (!composeResourcesDir.isNullOrBlank()) {
            val file = File(composeResourcesDir, "backend-engine.jar")
            if (file.exists()) return file
        }

        // 2. Relative to application installation root (Windows MSI / Linux DEB)
        val javaHome = System.getProperty("java.home")
        if (!javaHome.isNullOrBlank()) {
            val parent = File(javaHome).parentFile
            if (parent != null) {
                listOf(
                    File(parent, "app/resources/backend-engine.jar"),
                    File(parent, "lib/app/resources/backend-engine.jar"),
                    File(parent, "resources/backend-engine.jar"),
                    File(parent, "app/backend-engine.jar")
                ).firstOrNull { it.exists() }?.let { return it }
            }
        }

        // 3. Working directory resources
        listOf(
            File("resources/backend-engine.jar"),
            File("extra-resources/backend-engine.jar"),
            File("packaging/backend-engine.jar"),
            File("../Nexora-backend/build/libs/app.jar"),
            File("Nexora-backend/build/libs/app.jar")
        ).firstOrNull { it.exists() }?.let { return it }

        return null
    }

    /**
     * Locates the Java executable (uses bundled JRE in production).
     */
    private fun findJavaBinary(): String {
        val javaHome = System.getProperty("java.home")
        if (!javaHome.isNullOrBlank()) {
            val isWin = System.getProperty("os.name", "").lowercase().contains("win")
            val binName = if (isWin) "java.exe" else "java"
            val bin = File(javaHome, "bin/$binName")
            if (bin.exists() && bin.canExecute()) {
                return bin.absolutePath
            }
        }
        return "java"
    }

    /**
     * Overload for backward compatibility.
     */
    suspend fun ensureEngineStarted(onProgress: (String) -> Unit): Boolean {
        return ensureEngineStarted(onProgress = onProgress, onError = onProgress)
    }

    /**
     * Starts the local server engine if it is not already running.
     * Tries the standalone server package (docker compose) first, then the embedded backend jar.
     * If neither is found, reports an error to download the server package.
     */
    suspend fun ensureEngineStarted(
        onProgress: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ): Boolean {
        if (isEngineRunning()) {
            return true
        }

        val serverPackageDir = findServerPackageDir()
        val jar = findBackendJar()

        if (serverPackageDir == null && jar == null) {
            val notFoundMsg = try {
                s("setup.serverPackageNotFound")
            } catch (_: Exception) {
                "Standalone server not found. Please download the server package available from the provider."
            }
            onError(notFoundMsg)
            return false
        }

        if (serverPackageDir != null) {
            val started = startServerPackage(serverPackageDir, onProgress, onError)
            if (started) return true
            // If server package failed to start and a standalone jar exists, try falling back to the jar
            if (jar == null) return false
        }

        if (jar != null) {
            return startJarEngine(jar, onProgress, onError)
        }

        return false
    }

    private suspend fun startServerPackage(
        serverDir: File,
        onProgress: (String) -> Unit,
        onError: (String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            onProgress("Starting local SmartSilk server engine...")

            // Ensure .env exists in server package directory
            val envFile = File(serverDir, ".env")
            val envExample = File(serverDir, ".env.example")
            if (!envFile.exists() && envExample.exists()) {
                try {
                    val content = envExample.readText()
                    val randomKey = (1..48).map { (('a'..'z') + ('A'..'Z') + ('0'..'9')).random() }.joinToString("")
                    val modified = content.replace("JWT_SECRET=smartsilk-server-production-master-jwt-secret-key-32chars", "JWT_SECRET=$randomKey")
                    envFile.writeText(modified)
                } catch (_: Exception) {}
            }

            // Verify Docker is installed and running
            val isDockerAvailable = try {
                val p = ProcessBuilder("docker", "--version").start()
                p.waitFor(3, TimeUnit.SECONDS) && p.exitValue() == 0
            } catch (_: Exception) {
                false
            }

            if (!isDockerAvailable) {
                onError("Docker is not installed or not in PATH. Please install and start Docker to run the server package.")
                return@withContext false
            }

            // Ensure local image fallback: tag nexora:local to ghcr.io/nithiishsd/nexora-backend:latest if needed
            try {
                val inspectP = ProcessBuilder("docker", "image", "inspect", "ghcr.io/nithiishsd/nexora-backend:latest").start()
                val exists = inspectP.waitFor(2, TimeUnit.SECONDS) && inspectP.exitValue() == 0
                if (!exists) {
                    val localInspect = ProcessBuilder("docker", "image", "inspect", "nexora:local").start()
                    val localExists = localInspect.waitFor(2, TimeUnit.SECONDS) && localInspect.exitValue() == 0
                    if (localExists) {
                        ProcessBuilder("docker", "tag", "nexora:local", "ghcr.io/nithiishsd/nexora-backend:latest").start().waitFor(3, TimeUnit.SECONDS)
                    }
                }
            } catch (_: Exception) {}

            // Ensure scripts are executable on Unix
            val startSh = File(serverDir, "start-server.sh")
            if (startSh.exists()) {
                try { startSh.setExecutable(true) } catch (_: Exception) {}
            }
            val stopSh = File(serverDir, "stop-server.sh")
            if (stopSh.exists()) {
                try { stopSh.setExecutable(true) } catch (_: Exception) {}
            }

            // Launch containers using docker compose up -d
            onProgress("Launching server engine containers...")
            val composeProcess = try {
                ProcessBuilder("docker", "compose", "up", "-d")
                    .directory(serverDir)
                    .redirectErrorStream(true)
                    .start()
            } catch (_: Exception) {
                ProcessBuilder("docker-compose", "up", "-d")
                    .directory(serverDir)
                    .redirectErrorStream(true)
                    .start()
            }

            val exited = composeProcess.waitFor(25, TimeUnit.SECONDS)
            if (!exited || composeProcess.exitValue() != 0) {
                val output = composeProcess.inputStream.bufferedReader().readText().trim()
                onError("Failed to start server containers: ${if (output.isNotBlank()) output else "Process exited with code ${composeProcess.exitValue()}"}")
                return@withContext false
            }

            startedServerPackageDir = serverDir

            // Poll until server is healthy on port 8088
            onProgress("Waiting for SmartSilk server engine on port 8088...")
            val startTime = System.currentTimeMillis()
            val timeoutMs = 45_000L

            while (System.currentTimeMillis() - startTime < timeoutMs) {
                if (isEngineRunning()) {
                    onProgress("Local server engine is ready.")
                    return@withContext true
                }
                delay(1000)
            }

            onError("Timed out waiting for local server engine on port 8088.")
            false
        } catch (e: Exception) {
            onError("Failed to start server package: ${e.message}")
            false
        }
    }

    private suspend fun startJarEngine(
        jar: File,
        onProgress: (String) -> Unit,
        onError: (String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            onProgress("Starting local SmartSilk business engine...")
            val javaBin = findJavaBinary()
            val pb = ProcessBuilder(
                javaBin,
                "-jar",
                jar.absolutePath,
                "--spring.profiles.active=standalone",
                "--server.port=8088"
            )
            pb.redirectErrorStream(true)
            engineProcess = pb.start()

            // Wait for up to 35 seconds for the engine to initialize and report healthy
            val startTime = System.currentTimeMillis()
            val timeoutMs = 35_000L

            while (System.currentTimeMillis() - startTime < timeoutMs) {
                if (isEngineRunning()) {
                    onProgress("Local engine ready.")
                    return@withContext true
                }
                if (engineProcess?.isAlive == false) {
                    onError("Local engine process exited unexpectedly.")
                    return@withContext false
                }
                delay(800)
            }

            onError("Timed out waiting for local engine to start.")
            false
        } catch (e: Exception) {
            onError("Failed to start local engine: ${e.message}")
            false
        }
    }

    /**
     * Stops the running local engine cleanly.
     */
    fun stopEngine() {
        try {
            startedServerPackageDir?.let { dir ->
                try {
                    ProcessBuilder("docker", "compose", "down")
                        .directory(dir)
                        .start()
                        .waitFor(8, TimeUnit.SECONDS)
                } catch (_: Exception) {}
            }
            engineProcess?.let {
                if (it.isAlive) {
                    it.destroy()
                    it.waitFor(TimeUnit.SECONDS.toMillis(5), TimeUnit.MILLISECONDS)
                    if (it.isAlive) {
                        it.destroyForcibly()
                    }
                }
            }
        } catch (_: Exception) {
        } finally {
            engineProcess = null
            startedServerPackageDir = null
        }
    }
}

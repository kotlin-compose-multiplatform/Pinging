package com.none.pinging

import android.content.Context
import androidx.annotation.Keep
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.InetAddress

@Keep
enum class VpnStatus(status: String) {
    NONE("none"),
    PINGING("pinging"),
    SUCCESS("sucecss"),
    ERROR("error"),

}


@Keep
data class VpnServer(
    val hostName: String,
    val ip: String,
    val score: Int,
    val ping: Int,
    val speed: Long,
    val countryLong: String,
    val countryShort: String,
    val numVpnSessions: Int,
    val uptime: Long,
    val totalUsers: Int,
    val totalTraffic: Long,
    val logType: String,
    val operator: String,
    val message: String,
    val openVPNConfigDataBase64: String,
    var status: VpnStatus = VpnStatus.NONE
)


fun parseCsv(context: Context, inputStream: InputStream): List<VpnServer> {
    val vpnServers = mutableListOf<VpnServer>()
    val reader = BufferedReader(InputStreamReader(inputStream))
    reader.useLines { lines ->
        lines.drop(1).forEach { line ->
            val values = line.split(",")
            val vpnServer = VpnServer(
                values[0],
                values[1],
                try {
                    values[2].toInt()
                } catch (_: Exception) {0},
                try {
                    values[3].toInt()
                } catch (_: Exception) {0},
                try {
                    values[4].toLong()
                } catch (_: Exception) {0},
                values[5],
                values[6],
                try {
                    values[7].toInt()
                } catch (_: Exception) {0},
                try {
                    values[8].toLong()
                } catch (_: Exception) {0},
                try {
                    values[9].toInt()
                } catch (_: Exception) {0},
                try {
                    values[10].toLong()
                } catch (_: Exception) {0},
                values[11],
                values[12],
                values[13],
                values[14]
            )
            vpnServers.add(vpnServer)
        }
    }
    return vpnServers
}

fun pingVpnServers(vpnServers: List<VpnServer>, onPinging: (VpnServer) -> Unit, onSuccess: (VpnServer) -> Unit, onFailed: (VpnServer) -> Unit) {
    val scope = CoroutineScope(Dispatchers.IO) // Run in the IO thread pool
    scope.launch {
        vpnServers.forEach { vpnServer ->
            try {
                onPinging(vpnServer)
                val address = InetAddress.getByName(vpnServer.ip)
                val startTime = System.currentTimeMillis()
                val reachable = address.isReachable(2000) // Timeout in milliseconds
                val endTime = System.currentTimeMillis()
                val pingTime = if (reachable) endTime - startTime else -1
                println("Ping to ${vpnServer.ip}: ${if (reachable) "Success" else "Failed"} (Ping time: ${pingTime}ms)")
                if(reachable) {
                    onSuccess(vpnServer)
                } else {
                    onFailed(vpnServer)
                }
            } catch (e: Exception) {
                onFailed(vpnServer)
                println("Ping to ${vpnServer.ip} failed: ${e.message}")
            }
        }
    }
}

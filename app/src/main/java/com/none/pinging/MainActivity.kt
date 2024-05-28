package com.none.pinging

import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.none.pinging.ui.theme.PingingTheme
import com.potterhsu.Pinger
import kotlinx.coroutines.launch
import java.io.InputStream
import java.net.InetAddress


class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                111
            );
        }
        setContent {
            PingingTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color(0xFF000814)
                ) { innerPadding ->
                    Column {
                        val selectedServer = remember {
                            mutableStateOf<VpnServer?>(null)
                        }
                        val showDetails = remember {
                            mutableStateOf(false)
                        }
                        if(showDetails.value) {
                            ModalBottomSheet(onDismissRequest = { showDetails.value = false }) {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    selectedServer.value?.let {server->
                                        Text(text = "${server.toString()}")
                                    }
                                }
                            }
                        }
                        val context = LocalContext.current
                        val isPinging = remember { mutableStateOf(false) }
                        Spacer(modifier = Modifier.height(30.dp))

                        val list = remember {
                            mutableStateOf<List<VpnServer>>(emptyList())
                        }

                        fun updateStatus(ip: String, status: VpnStatus) {
                            val newList = list.value.toMutableList()
                            val index = newList.indexOfFirst { it.ip == ip }
                            if (index != -1) {
                                val updatedItem = newList[index].copy(status = status)
                                newList[index] = updatedItem
                                list.value = newList.toList()
                            }
                        }



                        FilePickerApp {
                            contentResolver.openInputStream(it)?.use { inputStream ->
                                isPinging.value = true
                                val vpnServers = parseCsv(context, inputStream)
                                list.value = vpnServers
                                pingVpnServers(
                                    vpnServers = vpnServers,
                                    onSuccess = { server ->
                                        updateStatus(server.ip, VpnStatus.SUCCESS)
                                    },
                                    onPinging = { server ->
                                        updateStatus(server.ip, VpnStatus.PINGING)

                                    },
                                    onFailed = { server ->
                                        updateStatus(server.ip, VpnStatus.ERROR)

                                    }
                                )
                            }

                        }
                        if (isPinging.value) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(list.value.size) {
                                    Card(
                                        onClick = { /*TODO*/ },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)){
                                            Column(modifier = Modifier
                                                .weight(1f)
                                                .padding(10.dp)) {
                                                Text(
                                                    text = list.value[it].ip,
                                                    color = Color(0xFFf7fff7),
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.W500
                                                )
                                                when (list.value[it].status) {
                                                    VpnStatus.NONE -> {
                                                        Text(
                                                            text = "waiting...",
                                                            color = Color(0xFFf7fff7),
                                                            fontSize = 12.sp
                                                        )
                                                    }

                                                    VpnStatus.PINGING -> {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(
                                                                12.dp
                                                            )
                                                        ) {
                                                            Text(
                                                                text = "Checking...",
                                                                color = Color(0xFFf7fff7),
                                                                fontSize = 12.sp
                                                            )
                                                            CircularProgressIndicator(
                                                                modifier = Modifier.size(
                                                                    20.dp
                                                                )
                                                            )
                                                        }
                                                    }

                                                    VpnStatus.SUCCESS -> {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(
                                                                12.dp
                                                            )
                                                        ) {
                                                            Icon(
                                                                Icons.Default.CheckCircle,
                                                                contentDescription = null,
                                                                tint = Color.Green
                                                            )
                                                            Text(
                                                                text = "Success",
                                                                color = Color.Green,
                                                                fontSize = 12.sp
                                                            )
                                                        }
                                                    }

                                                    VpnStatus.ERROR -> {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(
                                                                12.dp
                                                            )
                                                        ) {
                                                            Icon(
                                                                Icons.Default.Clear,
                                                                contentDescription = null,
                                                                tint = Color.Red
                                                            )
                                                            Text(
                                                                text = "Error",
                                                                color = Color.Red,
                                                                fontSize = 12.sp
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            IconButton(onClick = {
                                                selectedServer.value = list.value[it]
                                                showDetails.value = true
                                            }) {
                                                Icon(Icons.Default.Info, contentDescription = null)
                                            }
                                            IconButton(onClick = {
                                                selectedServer.value = list.value[it]
                                                showDetails.value = true
                                            }) {
                                                Icon(painter = painterResource(id = R.drawable.baseline_download_24), contentDescription = null)
                                            }
                                        }
                                    }
                                }
                                item { 
                                    Spacer(modifier = Modifier.height(30.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun sendPing(ipAddress: String): Boolean {
    return try {
        val address = InetAddress.getByName(ipAddress)
        val reachable = address.isReachable(5000) // Timeout in milliseconds
        reachable
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

@Composable
fun FilePickerApp(onSelected: (Uri) -> Unit) {
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    val openDocumentLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            selectedFileUri = uri
            uri?.let(onSelected)
        }

    if (selectedFileUri != null) {
        Text(text = "Pinging...")
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Button(onClick = {
                // Specify the MIME types you want to open
                val mimeTypes = arrayOf("application/pdf", "image/*", "text/*")
                openDocumentLauncher.launch(mimeTypes)
            }) {
                Text("Open csv file")
            }
        }
    }


}



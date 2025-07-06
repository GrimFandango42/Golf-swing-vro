package com.swingsync.ai.utils.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkManager @Inject constructor(
    private val context: Context
) {
    
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    private val _networkState = MutableStateFlow(NetworkState.UNKNOWN)
    val networkState: StateFlow<NetworkState> = _networkState.asStateFlow()
    
    private val _connectionType = MutableStateFlow(ConnectionType.NONE)
    val connectionType: StateFlow<ConnectionType> = _connectionType.asStateFlow()
    
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            updateNetworkState()
        }
        
        override fun onLost(network: Network) {
            super.onLost(network)
            updateNetworkState()
        }
        
        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            super.onCapabilitiesChanged(network, networkCapabilities)
            updateNetworkState()
        }
        
        override fun onLinkPropertiesChanged(network: Network, linkProperties: android.net.LinkProperties) {
            super.onLinkPropertiesChanged(network, linkProperties)
            updateNetworkState()
        }
    }
    
    fun startNetworkMonitoring() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .build()
        
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        updateNetworkState()
    }
    
    fun stopNetworkMonitoring() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            Timber.e(e, "Error unregistering network callback")
        }
    }
    
    private fun updateNetworkState() {
        val networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        
        if (networkCapabilities != null) {
            when {
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                    _connectionType.value = ConnectionType.WIFI
                    _networkState.value = NetworkState.CONNECTED
                }
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                    _connectionType.value = ConnectionType.CELLULAR
                    _networkState.value = NetworkState.CONNECTED
                }
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                    _connectionType.value = ConnectionType.ETHERNET
                    _networkState.value = NetworkState.CONNECTED
                }
                else -> {
                    _connectionType.value = ConnectionType.NONE
                    _networkState.value = NetworkState.DISCONNECTED
                }
            }
        } else {
            _connectionType.value = ConnectionType.NONE
            _networkState.value = NetworkState.DISCONNECTED
        }
        
        Timber.d("Network state updated: ${_networkState.value}, Connection type: ${_connectionType.value}")
    }
    
    fun isConnected(): Boolean {
        return _networkState.value == NetworkState.CONNECTED
    }
    
    fun isWifiConnected(): Boolean {
        return _connectionType.value == ConnectionType.WIFI
    }
    
    fun isCellularConnected(): Boolean {
        return _connectionType.value == ConnectionType.CELLULAR
    }
    
    fun getConnectionSpeed(): ConnectionSpeed {
        val networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        
        return when {
            networkCapabilities == null -> ConnectionSpeed.NONE
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                val linkDownstreamBandwidth = networkCapabilities.linkDownstreamBandwidthKbps
                when {
                    linkDownstreamBandwidth > 50000 -> ConnectionSpeed.FAST
                    linkDownstreamBandwidth > 10000 -> ConnectionSpeed.MEDIUM
                    else -> ConnectionSpeed.SLOW
                }
            }
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                val linkDownstreamBandwidth = networkCapabilities.linkDownstreamBandwidthKbps
                when {
                    linkDownstreamBandwidth > 20000 -> ConnectionSpeed.FAST
                    linkDownstreamBandwidth > 5000 -> ConnectionSpeed.MEDIUM
                    else -> ConnectionSpeed.SLOW
                }
            }
            else -> ConnectionSpeed.UNKNOWN
        }
    }
    
    fun canUploadVideo(): Boolean {
        return isConnected() && (isWifiConnected() || getConnectionSpeed() != ConnectionSpeed.SLOW)
    }
    
    fun canStreamRealtime(): Boolean {
        return isConnected() && getConnectionSpeed() != ConnectionSpeed.SLOW
    }
    
    fun shouldUseOfflineMode(): Boolean {
        return !isConnected() || getConnectionSpeed() == ConnectionSpeed.SLOW
    }
    
    fun getNetworkInfo(): NetworkInfo {
        return NetworkInfo(
            isConnected = isConnected(),
            connectionType = _connectionType.value,
            speed = getConnectionSpeed(),
            canUploadVideo = canUploadVideo(),
            canStreamRealtime = canStreamRealtime(),
            shouldUseOfflineMode = shouldUseOfflineMode()
        )
    }
}

enum class NetworkState {
    UNKNOWN,
    CONNECTED,
    DISCONNECTED,
    CONNECTING
}

enum class ConnectionType {
    NONE,
    WIFI,
    CELLULAR,
    ETHERNET
}

enum class ConnectionSpeed {
    NONE,
    SLOW,
    MEDIUM,
    FAST,
    UNKNOWN
}

data class NetworkInfo(
    val isConnected: Boolean,
    val connectionType: ConnectionType,
    val speed: ConnectionSpeed,
    val canUploadVideo: Boolean,
    val canStreamRealtime: Boolean,
    val shouldUseOfflineMode: Boolean
)
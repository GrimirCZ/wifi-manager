package cz.grimir.wifimanager.captive.routeragent.grpc

import io.grpc.Context
import java.net.SocketAddress

object RouterAgentClientInfo {
    val REMOTE_ADDR_KEY: Context.Key<SocketAddress> = Context.key("router-agent-remote-addr")
    val PEER_CERT_SUBJECT_KEY: Context.Key<String> = Context.key("router-agent-peer-cert-subject")
}

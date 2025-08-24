package com.example.dive.wear

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object WearBridge {
    private const val PATH_ALERT_TIDE = "/alert/tide"
    private const val PATH_TEST = "/tide/test"

    /** 워치로 tide 알림 데이터(title/body/id) 전송 → 워치가 직접 Notification 띄움 */
    fun sendTideAlert(ctx: Context, title: String, body: String, id: Int = genId()) {
        sendJson(ctx, PATH_ALERT_TIDE, JSONObject().apply {
            put("title", title)
            put("body", body)
            put("id", id)
        })
    }

    /** 단순 테스트용 (워치에서 body만 표시) */
    fun sendTest(ctx: Context, message: String = "폰에서 준거지롱") {
        sendBytes(ctx, PATH_TEST, message.toByteArray(StandardCharsets.UTF_8))
    }

    // ---------- 내부 공통 ----------

    private fun sendJson(ctx: Context, path: String, obj: JSONObject) {
        sendBytes(ctx, path, obj.toString().toByteArray(StandardCharsets.UTF_8))
    }

    private fun sendBytes(ctx: Context, path: String, payload: ByteArray) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val nodeClient = Wearable.getNodeClient(ctx)
                val nodes: List<Node> = Tasks.await(nodeClient.connectedNodes)
                if (nodes.isEmpty()) {
                    Log.w("WearBridge", "No connected wear nodes; skip send path=$path")
                    return@launch
                }
                val msgClient: MessageClient = Wearable.getMessageClient(ctx)
                nodes.forEach { node ->
                    Tasks.await(msgClient.sendMessage(node.id, path, payload))
                    Log.d("WearBridge", "Sent to ${node.displayName} (${node.id}) path=$path")
                }
            } catch (e: Exception) {
                Log.e("WearBridge", "send failed path=$path : ${e.message}", e)
            }
        }
    }

    private fun genId(): Int = (System.currentTimeMillis() % 100000).toInt()
}

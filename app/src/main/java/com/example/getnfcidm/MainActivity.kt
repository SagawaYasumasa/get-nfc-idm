package com.example.getnfcidm

import android.app.Activity
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.util.Log

import android.widget.*
// NFC関連
import android.nfc.NfcAdapter
import android.nfc.NfcManager
import android.nfc.Tag
import android.nfc.tech.NfcF
import android.os.*

class MainActivity : AppCompatActivity() , Runnable {
    val felica = FelicaReader(this,this)

    private val felicaListener = object : FelicaReaderInterface{
        // NFCタグ受信時処理
        override fun onReadTag(tag : Tag){
            val tvMain = findViewById<TextView>(R.id.tvMain)
            val idm : ByteArray = tag.id
            val idmString =byteToHex(idm)
            tag.techList
            Log.d("NFC_onReadTag",idmString)
            tvMain.text = idmString
        }
        override fun onConnect(){
            Log.d("NFC","onConnected")
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val tvVersion = findViewById<TextView>(R.id.tvVersion)
        val version = getString(R.string.version_label,BuildConfig.VERSION_NAME)
        tvVersion.text = version
        felica.setListener(felicaListener)
    }
    override fun onStop(){
        super.onStop()
    }
    override fun onResume(){
        super.onResume()
        felica.start()
    }
    override fun onPause() {
        super.onPause()
        felica.stop()
    }
    override fun run(){
    }
    private fun byteToHex(b: ByteArray) : String{
        var s =""
        for (i in 0 until b.size){
            s += "[%02X]".format(b[i])
//            s += "%02X".format(b[i])
        }
        return s
    }
}
interface FelicaReaderInterface : FelicaReader.Listener {
    fun onReadTag(tag : Tag)                            //タグ受信イベント
    fun onConnect()
}
class FelicaReader(private val context: Context, private val activity: Activity) : Handler(Looper.getMainLooper()){
    private var nfcmanager : NfcManager? = null
    private var nfcadapter : NfcAdapter? = null
    private var callback : CustomReaderCallback? = null
    private var listener : FelicaReaderInterface? =null
    interface Listener {}

    fun start(){
        callback = CustomReaderCallback()
        callback?.setHandler(this)
        nfcmanager = context.getSystemService(Context.NFC_SERVICE) as NfcManager?
        nfcadapter = nfcmanager!!.getDefaultAdapter()
        nfcadapter!!.enableReaderMode(activity,callback
            ,NfcAdapter.FLAG_READER_NFC_F or
                    NfcAdapter.FLAG_READER_NFC_A or
                    NfcAdapter.FLAG_READER_NFC_B or
                    NfcAdapter.FLAG_READER_NFC_V or
                    NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS or
                    NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,null)
    }
    fun stop(){
        nfcadapter!!.disableReaderMode(activity)
        callback = null
    }
    override fun handleMessage(msg: Message) {      //コールバックからのメッセージクラス
        if(msg.arg1 == 1){                          //見取り終了
            listener?.onReadTag(msg.obj as Tag)
        }
        if(msg.arg1 == 2){                          //読み取り終了
            listener?.onConnect()
        }
    }
    fun setListener(listener: Listener?){  // イベント受取先を設定
        if(listener is FelicaReaderInterface){
            this.listener = listener
        }
    }
    private class CustomReaderCallback : NfcAdapter.ReaderCallback {
        private var handler : Handler? = null
        override fun onTagDiscovered(tag:Tag){
            Log.d("NFC-onTagDiscovered", tag.id.toString())
            val msg = Message.obtain()
            msg.arg1 = 1
            msg.obj = tag
            if(handler != null) handler?.sendMessage(msg)
            val nfc : NfcF = NfcF.get(tag) ?: return
            try {
                nfc.connect()
                //nfc.transceive
                nfc.close()
                msg.arg1 =2
                msg.obj = tag
                if(handler != null) handler?.sendMessage(msg)
            }catch(e : Exception){
                nfc.close()
            }
        }
        fun setHandler(handler : Handler){
            this.handler = handler
        }
    }
}

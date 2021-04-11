package de.planetcat.protopietomidi

import android.content.*
import android.os.*
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * Die Main-Activity ist der Beginn der App.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var mService: MidiService
    private var mBound: Boolean = false
    private var debugmode = false
    private var serviceStatus = false //Speichert den Zustand des Toggle-Buttons (Bridge starten/stoppen)
    /** Defines callbacks for service binding, passed to bindService()  */
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            val binder = service as MidiService.LocalBinder
            mService = binder.getService()
            mBound = true
        }
        override fun onServiceDisconnected(arg0: ComponentName) {
            mBound = false
        }
    }



    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val messageId = intent.getStringExtra("messageId")
            val value = intent.getStringExtra("value")
            println("Message from ProtoPie. messageId=$messageId value=$value")
            if (mBound) {
                if (messageId != null) {
                    val midiStringParameters = messageId.split("-")
                    if (midiStringParameters.size == 3) {
                        if (value != null && value.toInt() < 128) {
                            mService.convertMidiStringToMidiMessageAndSend(midiStringParameters, value.toInt())
                        } else {
                            mService.convertMidiStringToMidiMessageAndSend(midiStringParameters, 127)
                        }
                        if (debugmode) {
                            val text = "PP->MID: $messageId - $value" //Show that it worked :)
                            val duration = Toast.LENGTH_SHORT
                            val toast = Toast.makeText(applicationContext, text, duration)
                            toast.show()
                        }
                    } else {
                        val text = "P2M Fehler: falsches Nachrichtenformat bei $messageId"
                        val duration = Toast.LENGTH_SHORT
                        val toast = Toast.makeText(applicationContext, text, duration)
                        toast.show()
                    }
                }
            }
        }
    }

    /**
     * OnCreate: Initialisiert alles, was für die Bridge notwendig ist.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        //Anzeigen und vorbereiten des Hauptbildschirms
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val button = findViewById<Button>(R.id.bridgecontroll)
        val text = findViewById<TextView>(R.id.textView)
        button?.setOnClickListener() //Hier werden die Aktionen des Buttons ausgelöst.
        {
            if (!serviceStatus) {
                MidiService.startService(this, "Foreground Service is running...")
                serviceStatus = true
                text.text = getTextfieldInformation()
                button.text = getString(R.string.stopbridge)
            } else {
                MidiService.stopService(this)
                serviceStatus = false
                text.text = getTextfieldInformation()
                button.text = getString(R.string.startbridge)
            }
        }
        val debugbutton = findViewById<Button>(R.id.debug)
        debugbutton?.setOnClickListener()
        {
            if (!debugmode) {
                debugmode = true
                text.text = getTextfieldInformation()
                debugbutton.text = getString(R.string.stopdebug)
            } else {
                debugmode = false
                text.text = getTextfieldInformation()
                debugbutton.text = getString(R.string.startdebug)
            }
        }
        //Anmelden für Nachrichten von ProtoPie
        val filter = IntentFilter("io.protopie.action.ONE_TIME_RESPONSE")
        this.registerReceiver(receiver, filter)

        Intent(this, MidiService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun getTextfieldInformation(): String {
        var textfieldInformation: String
        if (serviceStatus) {
            textfieldInformation = getString(R.string.bridgeisactive)
        } else {
            textfieldInformation = getString(R.string.bridgenotrunning)
        }
        textfieldInformation += "\n"
        if (debugmode){
            textfieldInformation+= getString(R.string.debugon)
        } else {
            textfieldInformation+= "Debug-Modus ist aus"
        }
        return textfieldInformation
    }

    /**
     * Wenn die App beendet wird, soll auch der Hintergrunddienst beendet werden.
     */
    override fun onDestroy() {
        unbindService(connection)
        mBound = false
        unregisterReceiver(receiver)
        super.onDestroy()
    }

}
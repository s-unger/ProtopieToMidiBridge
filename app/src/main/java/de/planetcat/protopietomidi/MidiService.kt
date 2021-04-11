package de.planetcat.protopietomidi

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.midi.*
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.io.IOException
import kotlin.experimental.or


class MidiService : Service()  {
    private val CHANNELID = "ForegroundService Kotlin"
    private lateinit var inputPort: MidiInputPort
    private var connected = false


    //To Receive The Data MainActivity gets from ProtoPie
    private val binder = LocalBinder()
    inner class LocalBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods
        fun getService(): MidiService = this@MidiService
    }
    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    fun convertMidiStringToMidiMessageAndSend(midiStringParameters: List<String>, valueAsInt: Int) {
        println(midiStringParameters.toString())
        println(midiStringParameters.size)
        if (midiStringParameters.size <= 3) {
            var status = 0b00000000
            when (midiStringParameters[0]) {
                Status.NoteOff.toString() -> {
                    status = 0b10000000
                }
                Status.NoteOn.toString() -> {
                    status = 0b10010000
                }
                Status.PolyphonicPressure.toString() -> {
                    status = 0b10100000
                }
                Status.ControlChange.toString() -> {
                    status = 0b10110000
                }
                Status.ProgrammChange.toString() -> {
                    status = 0b11000000
                }
                Status.ChannelPressure.toString() -> {
                    status = 0b11010000
                }
                Status.PitchBending.toString() -> {
                    status = 0b11100000
                }
                Status.SystemExclusive.toString() -> {
                    status = 0b11110000
                }
            }
            val channel = midiStringParameters[1].toInt().toUByte()
            val buffer = ByteArray(32)
            buffer[0] = status.toUByte().toByte() or channel.toByte()
            buffer[1] = midiStringParameters[2].toInt().toUByte().toByte()
            buffer[2] = valueAsInt.toInt().toUByte().toByte()
            println("Hammond was here")
            println(buffer[0].toUByte().toString(2))
            println(buffer[1].toUByte().toString(2))
            println(buffer[2].toUByte().toString(2))
            if (connected == true && inputPort != null) {
                inputPort.send(buffer, 0, 3);
            } else {
                val text = "P2M Fehler: Midiverbindung getrennt. Bitte P2M-App neu starten."
                val duration = Toast.LENGTH_SHORT
                val toast = Toast.makeText(applicationContext, text, duration)
                toast.show()
            }
        }
    }




    companion object {
        fun startService(context: Context, message: String) {
            val startIntent = Intent(context, MidiService::class.java)
            startIntent.putExtra("inputExtra", message)
            ContextCompat.startForegroundService(context, startIntent)
        }
        fun stopService(context: Context) {
            val stopIntent = Intent(context, MidiService::class.java)
            context.stopService(stopIntent)
        }
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("status", "Service started")
        createNotificationChannel()
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0, notificationIntent, 0
        )
        val notification = NotificationCompat.Builder(this, CHANNELID)
            .setContentTitle("Bridge running")
            .setContentText("Midi transfer active between ProtoPie and MidiOut")
            .setContentIntent(pendingIntent)
            .build()
        startForeground(1, notification)

        val m = this.getSystemService(Context.MIDI_SERVICE) as MidiManager
        val midiDevices = getMidiDevices(true, m)
        if (midiDevices.isNotEmpty()){
            println(midiDevices)
            m.openDevice(midiDevices[0], {
                //startReadingMidi (it, 0)

                class MyReceiver : MidiReceiver() {
                    @Throws(IOException::class)
                    override fun onSend(
                        data: ByteArray, offset: Int,
                        count: Int, timestamp: Long
                    ) {
                        // parse MIDI or whatever
                        val midicmd = decodeMidi(data.toUByteArray(), offset)
                        val text = midicmd.toString()

                        //val text = String.format("%8s", Integer.toBinaryString(data[offset].toInt() and 0xFF)).replace(' ', '0')
                        val intent = Intent("io.protopie.action.ONE_TIME_TRIGGER")
                        intent.putExtra("messageId", text)
                        intent.putExtra("value", midicmd.value2) // Optional
                        this@MidiService.sendBroadcast(intent)

                    }
                }


                val outputPort: MidiOutputPort = it.openOutputPort(0)
                outputPort.connect(MyReceiver())
                inputPort = it.openInputPort(0)
                connected = true

            }, null)
        } else {
            val text = "P2M Fehler: Handy nicht im MIDI-Modus (Aktivieren) oder Midiport belegt (App neu starten)."
            val duration = Toast.LENGTH_SHORT
            val toast = Toast.makeText(applicationContext, text, duration)
            toast.show()
            stopSelf()
        }



        //stopSelf();
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNELID, "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager!!.createNotificationChannel(serviceChannel)
        }
    }
    private fun getMidiDevices(isOutput: Boolean, m: MidiManager) : List<MidiDeviceInfo> {
            return m.devices.filter { it.outputPortCount > 0 }
    }

    private fun decodeMidi(byteArray: UByteArray, offset: Int):MidiCmd {
        val msb: UByte = byteArray[offset].toInt().ushr(4).toUByte()
        var status = Status.NoteOff
        when (msb.toUByte()) {
            "00001000".toUByte(2) -> {
                status = Status.NoteOff
            }
            "00001001".toUByte(2) -> {
                status = Status.NoteOn
            }
            "00001010".toUByte(2) -> {
                status = Status.PolyphonicPressure
            }
            "00001011".toUByte(2) -> {
                status = Status.ControlChange
            }
            "00001100".toUByte(2) -> {
                status = Status.ProgrammChange
            }
            "00001101".toUByte(2) -> {
                status = Status.ChannelPressure
            }
            "00001110".toUByte(2) -> {
                status = Status.PitchBending
            }
            "00001111".toUByte(2) -> {
                status = Status.SystemExclusive
            }
        }
        val lsb: UByte = byteArray[offset].toUByte() and 15.toUByte()
        return MidiCmd(
            status,
            lsb.toInt(),
            byteArray[offset + 1].toUByte().toInt(),
            byteArray[offset + 2].toUByte().toInt()
        )
    }


}

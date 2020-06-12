package com.example.apppanel

import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*


class Graficas : AppCompatActivity() {

    // Objeto compartido entre la clase Graficas y ConnectToDevice
    companion object{
        var m_myUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        var m_bluetoothSocket: BluetoothSocket? = null
        lateinit var m_progress: ProgressDialog
        lateinit var m_bluetoothAdapter: BluetoothAdapter
        var m_isConnected: Boolean = false
        lateinit var m_address: String
        lateinit var btnConectar: Button

        // Defines several constants used when transmitting messages between the
        // service and the UI.
        const val MESSAGE_READ: Int = 0
        const val MESSAGE_WRITE: Int = 1
        const val MESSAGE_TOAST: Int = 2

        lateinit var mHandler: Handler
    }



    val MODULOS = mapOf(
        "Modulo1" to mapOf(
            "MAC" to "20:18:08:14:85:01",
            "TITULO" to "Modulo #1"),

        "Modulo2" to mapOf(
            "MAC" to "Desconocida",
            "TITULO" to "Modulo #2"),

        "Modulo3" to mapOf(
            "MAC" to "Desconocida",
            "TITULO" to "Modulo #3")
    )

    lateinit var txtTituloModulo: TextView
    lateinit var txtViewData: TextView
    lateinit var moduloActual: Map<String, String>

    val SCREEN_ORIENTATION_SENSOR_LANDSCAPE = 6
    val REQUEST_ENABLE_BLUETOOTH = 1



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_graficas)
        setRequestedOrientation(SCREEN_ORIENTATION_SENSOR_LANDSCAPE)

        txtTituloModulo = findViewById(R.id.txtTituloModulo)
        btnConectar = findViewById(R.id.btnConectar)
        txtViewData = findViewById(R.id.txtViewData)

        if (intent.extras != null) {
            val nombreModulo = intent.extras!!["nombreModulo"] as String
            moduloActual = MODULOS[nombreModulo]!!
        }

        txtTituloModulo.text = moduloActual["TITULO"]
        m_address = moduloActual["MAC"].toString()

        btnConectar.setOnClickListener {
            // Adaptador Bluetooth por defecto
            m_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

            // Verifica si el dispositivo cuenta con bluetooth
            if(m_bluetoothAdapter == null){
                Toast.makeText(this,"El dispositivo no soporta Bluetooth", Toast.LENGTH_LONG).show()
            }
            // Si bluetooth esta apagado lanza una actividad para encenderlo
            else if(!m_bluetoothAdapter!!.isEnabled){
                Toast.makeText(this,"Activa el Bluetooth para conectarte a los modulos", Toast.LENGTH_SHORT).show()
                val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BLUETOOTH)
                ConnectToDevice(this).execute()
            } else{
                ConnectToDevice(this).execute()
            }

            mHandler = object : Handler() {
                override fun handleMessage(msg: Message?) {
                    when(msg!!.what){
                        MESSAGE_READ -> {
                            //val readBuf = msg.obj as ByteArray
                            //val readMessage = String(readBuf, 0, msg.arg1)
                            val readMessage = msg.obj as String
                            runOnUiThread {
                                txtViewData.text = readMessage
                            }
                        }
                        else -> {
                            print("khe")
                        }
                    }
                }
            }

            //val BTCom = Thread(BluetoothCom(m_bluetoothSocket!!, mHandler))
            //BTCom.start()
        }
    }



    private class BluetoothCom(BTSocket: BluetoothSocket, handler: Handler):Runnable{

        private val mmSocket: BluetoothSocket = BTSocket
        private val mmInStream: InputStream = mmSocket.inputStream
        private val mmOutStream: OutputStream? = mmSocket.outputStream
        private val handler = handler
        var msg = ""
        var ch: Byte = 0.toByte()
        override fun run() {
            var numBytes: Int = 0 // bytes returned from read()
            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                if(mmInStream.available()>0) {
                    // Read from the InputStream.
                    msg = ""
                    ch = 0.toByte()
                    while(ch.toString()!="10") {
                        ch = mmInStream.read().toByte()
                        numBytes++;
                        msg+=ch.toChar();
                    }

                    val readMsg = handler.obtainMessage(
                        MESSAGE_READ, msg)
                    readMsg.sendToTarget()
                }
            }
        }
    }

    // Cierra la conexión Bluetooth al salirse de la actividad
    override fun onDestroy() {
        super.onDestroy()
        if(m_bluetoothSocket != null){
            try {
                m_bluetoothSocket!!.close()
                m_bluetoothSocket = null
                m_isConnected = false
            } catch (e: IOException){
                e.printStackTrace()
            }
        }
    }

    // Función para enviar datos
    private fun sendCommand(input: String){
        if(m_bluetoothSocket != null){
            try {
                m_bluetoothSocket!!.outputStream.write(input.toByteArray())
            } catch (e: IOException){
                e.printStackTrace()
            }
        }
    }

    private fun actualizarValores(incomingMessage: String){
        runOnUiThread{
            txtViewData.text = incomingMessage
        }
    }


    // La conexión debe hacerse iniciando una nueva tarea
    // Para no bloquear el proceso principal
    private class ConnectToDevice(c: Context) : AsyncTask<Void, Void, String>(){
        private var connectSuccess: Boolean = true
        private val context: Context
        init{
            this.context = c
        }
        override fun onPreExecute() {
            super.onPreExecute()
            m_progress = ProgressDialog.show(context, "Conectando...", "Por favor espera")
        }
        override fun doInBackground(vararg params: Void?): String? {
            try {
                // Si no existe conexión
                if (m_bluetoothSocket == null || !m_isConnected){
                    m_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

                    // Obtiene el objeto BluetoothDevice utilizando la dirección MAC
                    val device: BluetoothDevice = m_bluetoothAdapter.getRemoteDevice(m_address)

                    // Crea un socket entre el dispositivo y el modulo
                    m_bluetoothSocket = device.createInsecureRfcommSocketToServiceRecord(m_myUUID)

                    // Siempre se debe llamar esta función antes de conectarse
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery()

                    // Se realiza la conexión con el modulo
                    m_bluetoothSocket!!.connect()

                }
            } catch (e: IOException){
                connectSuccess = false
                e.printStackTrace()
            }
            return null
        }
        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            if(!connectSuccess){
                Log.i("data", "No se puede conectar")
                Toast.makeText(this.context, "No se logró realizar la conexión :c", Toast.LENGTH_SHORT)
            } else{
                m_isConnected = true
                Toast.makeText(this.context, "Conectado con éxito", Toast.LENGTH_SHORT)
                btnConectar.visibility = View.GONE
                val BTCom = Thread(BluetoothCom(m_bluetoothSocket!!, mHandler))
                BTCom.start()
            }
            m_progress.dismiss()
        }
    }


}

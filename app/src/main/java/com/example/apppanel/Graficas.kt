package com.example.apppanel

import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
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

        lateinit var mmInStream: InputStream
        lateinit var mmOutStream: OutputStream
        private val mmBuffer: ByteArray = ByteArray(1024) // mmBuffer store for the stream
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
    lateinit var moduloActual: Map<String, String>

    val SCREEN_ORIENTATION_SENSOR_LANDSCAPE = 6
    val REQUEST_ENABLE_BLUETOOTH = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_graficas)
        setRequestedOrientation(SCREEN_ORIENTATION_SENSOR_LANDSCAPE)

        txtTituloModulo = findViewById(R.id.txtTituloModulo)
        btnConectar = findViewById(R.id.btnConectar)

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

                    mmInStream = m_bluetoothSocket!!.inputStream
                    mmOutStream = m_bluetoothSocket!!.outputStream
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
            }
            m_progress.dismiss()
        }
    }
}

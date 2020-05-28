package com.example.apppanel

import android.app.Activity
import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import java.io.IOException
import java.util.*


class MainActivity : AppCompatActivity() {

    companion object{
        var m_myUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        var m_bluetoothSocket: BluetoothSocket? = null
        lateinit var m_progress: ProgressDialog
        lateinit var m_bluetoothAdapter: BluetoothAdapter
        var m_isConnected: Boolean = false
        lateinit var m_address: String

        lateinit var txtMod1: TextView
        lateinit var btnDesconectarMod1: Button

        fun moduloConectado(txtMod: TextView, btnDesconectar: Button){
            txtMod.text = "Conectado"
            txtMod.setBackgroundColor(Color.parseColor("#00C853"))
            btnDesconectar.visibility = View.VISIBLE
        }
    }

    lateinit var btnMod1: Button
    lateinit var btnMod2: Button
    lateinit var btnMod3: Button
    lateinit var btnConectar: Button

    val DEVICE_REQUEST = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Views necesarios
        txtMod1 = findViewById(R.id.txtMod1)

        btnMod1 = findViewById<Button>(R.id.btnMod1)
        btnMod2 = findViewById<Button>(R.id.btnMod2)
        btnMod3 = findViewById<Button>(R.id.btnMod3)
        btnConectar = findViewById<Button>(R.id.btnConectar)
        btnDesconectarMod1 = findViewById<Button>(R.id.btnDesconectarMod1)

        // Botones para ingresar a las graficas de cada modulo
        btnMod1.setOnClickListener(){view ->
            val i = Intent(this, Graficas::class.java)
            startActivity(i)
        }

        btnMod2.setOnClickListener(){view ->
            Toast.makeText(this@MainActivity, "Modulo 2", Toast.LENGTH_SHORT).show()
        }

        btnMod3.setOnClickListener(){view ->
            Toast.makeText(this@MainActivity, "Modulo 3", Toast.LENGTH_SHORT).show()
        }

        // Botones para la conexión bluetooth
        btnConectar.setOnClickListener(){view ->
            // Toast.makeText(this@MainActivity, "Conectar", Toast.LENGTH_SHORT).show()
            val i = Intent(this, ConectarBluetooth::class.java)
            startActivityForResult(i, DEVICE_REQUEST)
        }

        // Botones para desconexión de los modulos
        btnDesconectarMod1.setOnClickListener(){view ->
            if(m_bluetoothSocket != null){
                try {
                    m_bluetoothSocket!!.close()
                    m_bluetoothSocket = null
                    m_isConnected = false
                    moduloDesconectado(txtMod1, btnDesconectarMod1)
                } catch (e: IOException){
                    e.printStackTrace()
                }
            }
        }
    }

    // Función que asigna propiedades a los modulos desconectados
    fun moduloDesconectado(txtMod: TextView, btnDesconectar: Button){
        txtMod.text = "Desconectado"
        txtMod.setBackgroundColor(Color.parseColor("#FF5252"))
        btnDesconectar.visibility = View.INVISIBLE
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == DEVICE_REQUEST) {
            // Al abrir la actividad ConectarBluetooth.kt se espera esta respuesta
            if (resultCode == Activity.RESULT_OK) {
                // Se recupera la dirección MAC del dispositivo seleccionado
                m_address = data!!.getStringExtra("Device_address")
                ConnectToDevice(this).execute()
            }
        }
    }

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
            } else{
                m_isConnected = true
                moduloConectado(txtMod1, btnDesconectarMod1)
            }
            m_progress.dismiss()
        }
    }
}

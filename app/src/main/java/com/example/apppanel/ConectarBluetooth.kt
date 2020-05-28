package com.example.apppanel

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.*

/*
Información sobre la conexión bluetooth:
https://developer.android.com/guide/topics/connectivity/bluetooth
 */

class ConectarBluetooth : AppCompatActivity() {

    var m_bluetoothAdapter: BluetoothAdapter? = null
    val REQUEST_ENABLE_BLUETOOTH = 1
    lateinit var m_pairedDevices: Set<BluetoothDevice>
    lateinit var txtListaDispositivos: ListView
    lateinit var btnActualizar: Button

    companion object{
        val EXTRA_ADDRESS: String = "Device_address"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_conectar_bluetooth)

        // Views para ver y actualizar dispositivos
        txtListaDispositivos = findViewById(R.id.txtListaDispositivos)
        btnActualizar = findViewById(R.id.btnActualizar)

        // Adaptador Bluetooth por defecto
        m_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        // Verifica si el dispositivo cuenta con bluetooth
        if(m_bluetoothAdapter == null){
            Toast.makeText(this,"El dispositivo no soporta Bluetooth", Toast.LENGTH_LONG).show()
        }

        // Si bluetooth esta apagado lanza una actividad para encenderlo
        if(!m_bluetoothAdapter!!.isEnabled){
            Toast.makeText(this,"Activa el Bluetooth para conectarte a los modulos", Toast.LENGTH_SHORT).show()
            val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BLUETOOTH)
        }

        // Obtiene la lista de conexiones bluetooth sincronizadas con el dispositivo
        pairedDeviceList()

        btnActualizar.setOnClickListener(){pairedDeviceList()}
    }

    private fun pairedDeviceList(){
        // Obtiene BluetoothDevices sincronizados al dispositivo
        m_pairedDevices = m_bluetoothAdapter!!.bondedDevices

        // Los dispositivos se guardan en listas
        val list: ArrayList<BluetoothDevice> = ArrayList()
        val listName: ArrayList<String> = ArrayList()

        if(!m_pairedDevices.isEmpty()){
            for(device: BluetoothDevice in m_pairedDevices){
                list.add(device)
                listName.add(device.name)
            }
        } else{
            Toast.makeText(this, "No hay dispositivos emparejados", Toast.LENGTH_SHORT).show()
        }

        // Se utiliza un adaptador para visualizar la lista de nombres
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, listName)
        txtListaDispositivos.adapter = adapter

        // Evento para la selección de un dispositivo
        txtListaDispositivos.onItemClickListener = AdapterView.OnItemClickListener{_, _, position, _ ->
            // Se obtiene el dispositivo seleccionado
            val device: BluetoothDevice = list[position]
            val address: String = device.address

            // Se fija como resultado de la actividad la MAC del dispositivo seleccionado
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra(EXTRA_ADDRESS, address)
            setResult(Activity.RESULT_OK, intent)

            // Fin de la actividad
            finish()
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Resultado para la actividad de encender el bluetooth
        if(requestCode == REQUEST_ENABLE_BLUETOOTH
            && resultCode  == Activity.RESULT_OK
            && m_bluetoothAdapter!!.isEnabled){
            Toast.makeText(this, "Bluetooth Conectado", Toast.LENGTH_SHORT).show()
        }
        else{
            Toast.makeText(this, "Bluetooth no se ha podido conectar", Toast.LENGTH_SHORT).show()
        }
    }
}

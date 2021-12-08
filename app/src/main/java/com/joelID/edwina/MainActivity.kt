package com.joelID.edwina

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.pm.ActivityInfo
import android.util.Log
import android.view.View
import android.widget.*
import com.google.android.material.switchmaterial.SwitchMaterial
import java.io.OutputStream
import java.lang.ArithmeticException
import java.lang.Exception
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {
    private var REQUEST_ENABLE_BLUETOTH:Int = 1 //doit être >=0
    private lateinit var bAdapter:BluetoothAdapter //abstract variable
    private lateinit var connectingBar:ProgressBar
    private lateinit var OutStream:OutputStream

    private fun connectEdwina(){ //connects to Edwina and initializes the OutputStream that allows the device to send orders to the robot
        val EdwinaMAC = "98:DA:50:00:5A:A7"
        var found = false //indicates if Edwina is in the paired devices
        lateinit var Edwina:BluetoothDevice
        val pairedDevices: Set<BluetoothDevice>? = bAdapter.bondedDevices
        pairedDevices?.forEach { device ->
            if (device.address == EdwinaMAC){
                found = true
                Edwina = device
            }
        }
        if (!found){ //you must pair Edwina in the device settings before using the Edwina app
            Toast.makeText(this@MainActivity, getString(R.string.not_paired_error), Toast.LENGTH_LONG).show()
            this@MainActivity.finish()
            exitProcess(0)
        }

        //the Edwina variable has been initialized, now let's connect the robot
        val UUID = Edwina.getUuids()[0].getUuid() //get Edwina's UUID
        val socket = Edwina.createRfcommSocketToServiceRecord(UUID)
        Thread(Runnable {
            try {
                this@MainActivity.runOnUiThread(Runnable {
                    connectingBar.visibility = View.VISIBLE //make the spinning icon visible
                })

                socket.connect()
                val connection_info: TextView = findViewById(R.id.info)
                this@MainActivity.runOnUiThread(Runnable {
                    connection_info.setText(getString(R.string.connection_succeeded))
                })
                OutStream = socket.outputStream

                this@MainActivity.runOnUiThread(Runnable {
                    connectingBar.visibility = View.GONE
                })
            }catch(e:Exception){
                this@MainActivity.runOnUiThread(Runnable {
                    Toast.makeText(
                    this@MainActivity,
                    getString(R.string.connection_failed),
                    Toast.LENGTH_LONG
                    ).show()
                    connectingBar.visibility = View.GONE
                })
            }
        }).start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
        setContentView(R.layout.activity_main)
        bAdapter = BluetoothAdapter.getDefaultAdapter()

        connectingBar = findViewById(R.id.connecting)
        connectingBar.setVisibility(View.INVISIBLE) //hide the spinning icon for the moment

        //ACTIVATE BLUETOOTH ON CLICK ON THE BUTTON
        val connect :Button = findViewById(R.id.connect) as Button

        connect.setOnClickListener{
            if (!bAdapter.isEnabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent,REQUEST_ENABLE_BLUETOTH)
            }
            else{
                connectEdwina()
            }
        }


        //ACTION LISTENERS FOR THE CONTROLS

        val up_arrow = findViewById(R.id.up_arrow) as ImageView
        var up_arrow_pressed = false
        val right_arrow = findViewById(R.id.right_arrow) as ImageView
        var right_arrow_pressed = false
        val left_arrow = findViewById(R.id.left_arrow) as ImageView
        var left_arrow_pressed = false
        val down_arrow = findViewById(R.id.down_arrow) as ImageView
        var down_arrow_pressed = false

        //UP ARROW

        up_arrow.setOnClickListener{
            up_arrow_pressed = !up_arrow_pressed
            if (up_arrow_pressed){
                up_arrow.setImageResource(R.drawable.arrow_pressed)
                OutStream.write("6".toByteArray())

                //unpress down arrow
                down_arrow.setImageResource(R.drawable.arrow)
                down_arrow_pressed = false
            }
            else{
                up_arrow.setImageResource(R.drawable.arrow)
                OutStream.write("8".toByteArray())
            }
        }

        //DOWN ARROW

        down_arrow.setOnClickListener{
            down_arrow_pressed = !down_arrow_pressed
            if (down_arrow_pressed){
                down_arrow.setImageResource(R.drawable.arrow_pressed)
                OutStream.write("7".toByteArray())

                //unpress up arrow
                up_arrow.setImageResource(R.drawable.arrow)
                up_arrow_pressed = false
            }
            else{
                down_arrow.setImageResource(R.drawable.arrow)
                OutStream.write("8".toByteArray())
            }
        }

        //RIGHT ARROW

        right_arrow.setOnClickListener{
            right_arrow_pressed = !right_arrow_pressed
            if (right_arrow_pressed){
                right_arrow.setImageResource(R.drawable.arrow_pressed)
                OutStream.write("9".toByteArray())

                //unpress left arrow
                left_arrow.setImageResource(R.drawable.arrow)
                left_arrow_pressed = false
            }
            else{
                right_arrow.setImageResource(R.drawable.arrow)
                OutStream.write("11".toByteArray())
            }
        }

        //LEFT ARROW

        left_arrow.setOnClickListener{
            left_arrow_pressed = !left_arrow_pressed
            if (left_arrow_pressed){
                left_arrow.setImageResource(R.drawable.arrow_pressed)
                OutStream.write("10".toByteArray())

                //unpress right arrow
                right_arrow.setImageResource(R.drawable.arrow)
                right_arrow_pressed = false
            }
            else{
                left_arrow.setImageResource(R.drawable.arrow)
                OutStream.write("11".toByteArray())
            }
        }

        //CENTER BUTTON
        val centerButton:Button = findViewById(R.id.center)

        centerButton.setOnClickListener{
            OutStream.write("11".toByteArray())

            //unpress left arrow
            left_arrow.setImageResource(R.drawable.arrow)
            left_arrow_pressed = false

            //unpress right arrow
            right_arrow.setImageResource(R.drawable.arrow)
            right_arrow_pressed = false
        }

        //LEFT ARM LED SWITCH
        val leftLED = findViewById(R.id.left_led) as Switch

        leftLED.setOnClickListener{
            if (leftLED.isChecked()){
                OutStream.write("3".toByteArray())
            }
            else{
                OutStream.write("2".toByteArray())
            }
        }

        //RIGHT ARM LED SWITCH
        val rightLED = findViewById(R.id.right_led) as Switch

        rightLED.setOnClickListener{
            if (rightLED.isChecked()){
                OutStream.write("5".toByteArray())
            }
            else{
                OutStream.write("4".toByteArray())
            }
        }

        //HEAD LEDS SWITCH
        val headsLED = findViewById(R.id.head_led) as Switch

        headsLED.setOnClickListener{
            if (headsLED.isChecked()){
                OutStream.write("1".toByteArray())
            }
            else{
                OutStream.write("0".toByteArray())
            }
        }

        //HEAD SERVO
        val headServo = findViewById(R.id.head_bar) as SeekBar

        headServo.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seek:SeekBar,progress:Int,fromUser:Boolean){
                //Auto-generated method
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
                //Auto-generated method
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
                val progress = headServo.progress + 620
                OutStream.write((progress.toString()).toByteArray())
            }
        })

        //HAND SERVO
        val handServo = findViewById(R.id.hand_bar) as SeekBar

        handServo.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seek:SeekBar,progress:Int,fromUser:Boolean){
                //Auto-generated method
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
                //Auto-generated method
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
                val progress = handServo.progress + 420
                OutStream.write((progress.toString()).toByteArray())
            }
        })

        //LEFT ARM
        val leftServo = findViewById(R.id.left_arm_bar) as SeekBar

        leftServo.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seek:SeekBar,progress:Int,fromUser:Boolean){
                //Auto-generated method
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
                //Auto-generated method
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
                val progress = leftServo.progress + 20
                OutStream.write((progress.toString()).toByteArray())
            }
        })

        //RIGHT ARM
        val rightServo = findViewById(R.id.right_arm_bar) as SeekBar

        rightServo.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seek:SeekBar,progress:Int,fromUser:Boolean){
                //Auto-generated method
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
                //Auto-generated method
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
                val progress = rightServo.progress + 220
                OutStream.write((progress.toString()).toByteArray())
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) { //executes after the pop-up windiw where the user chose to activate bluetooth or not
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode){
            REQUEST_ENABLE_BLUETOTH ->
                if (resultCode== Activity.RESULT_OK){
                    connectEdwina()
                }
                else{ //si l'utilisateur refuse d'activer le bluetooth, l'application se ferme
                    this@MainActivity.finish()
                    exitProcess(0)
                }
        }
    }
}
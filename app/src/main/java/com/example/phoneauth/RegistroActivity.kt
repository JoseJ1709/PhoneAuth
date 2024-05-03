package com.example.phoneauth

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.auth.FirebaseAuth
import android.content.ContentValues.TAG
import android.location.Location
import android.text.TextUtils
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.core.net.toUri
import com.example.phoneapp.Datos
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import com.google.firebase.storage.ktx.storage
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class RegistroActivity : AppCompatActivity() {
    private lateinit var locationOverlay: MyLocationNewOverlay
    private lateinit var auth : FirebaseAuth
    val PATH_USERS = "users/"
    private lateinit var listener1 : ValueEventListener
    private val database = FirebaseDatabase.getInstance()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var activityResultLauncherCamara: ActivityResultLauncher<Intent>
    private lateinit var storage: FirebaseStorage
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)
        //asingando servicio de firebase
        auth = Firebase.auth
        storage = Firebase.storage
        guardarUriImagen(null)

        //asingando servicio de mapas
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val nombreInput = findViewById<EditText>(R.id.nombre_input)
        val apellidoInput = findViewById<EditText>(R.id.apellido_input)
        val emailInput = findViewById<EditText>(R.id.email_input)
        val passwordInput = findViewById<EditText>(R.id.pasword_input)
        val identificacionInput = findViewById<EditText>(R.id.identificacion_input)
        val imagenInput = findViewById<ImageButton>(R.id.imagen_input)
        val latitud = findViewById<EditText>(R.id.latitud_g_input)
        val longitud = findViewById<EditText>(R.id.longitud_g_input)
        val setLatitudLongitud = findViewById<Button>(R.id.set_latitud_longitud)
        val buttonRegistrarse = findViewById<Button>(R.id.button_registrarse)
        activarResultLauncherCamara(imagenInput)
        //seters
        setersImgMaps(setLatitudLongitud, imagenInput, latitud,longitud)
        seterRegistrar(buttonRegistrarse, nombreInput, apellidoInput, emailInput, passwordInput, identificacionInput, latitud, longitud)

    }
    private fun setersImgMaps(setLatitudLongitud: Button, imagenInput: ImageButton, latitud: EditText, longitud: EditText) {

        setLatitudLongitud.setOnClickListener {
            mapLocation(latitud, longitud)
        }
        imagenInput.setOnClickListener {
            requestCameraPermission()
        }
    }
    fun mapLocation(latitud: EditText, longitud: EditText) {

        when {
            ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                myLocation(latitud, longitud)
            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                this, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) -> {
                requestPermissions(
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                    Datos.MY_PERMISSION_REQUEST_LOCATION
                )
            }

            else -> {
                requestPermissions(
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                    Datos.MY_PERMISSION_REQUEST_LOCATION
                )
            }
        }
    }
    private fun requestCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                abrirCamara()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                this, android.Manifest.permission.CAMERA) -> {
                requestPermissions(
                    arrayOf(android.Manifest.permission.CAMERA),
                    Datos.MY_PERMISSION_REQUEST_READ_CAMERA
                )
            }
            else -> {
                requestPermissions(
                    arrayOf(android.Manifest.permission.CAMERA),
                    Datos.MY_PERMISSION_REQUEST_READ_CAMERA
                )
            }
        }
    }

    //Código relacionado a la cámara
    private fun abrirCamara(){
        val intentCamara = Intent("android.media.action.IMAGE_CAPTURE")
        activityResultLauncherCamara.launch(intentCamara)
    }

    private fun activarResultLauncherCamara(imagen: ImageButton){
        activityResultLauncherCamara = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()){ resultado ->
            if(resultado.resultCode == Activity.RESULT_OK){
                val bitMapImagen = resultado.data?.extras?.get("data") as? Bitmap
                if(bitMapImagen != null){
                    val imagenURI = MediaStore.Images.Media.insertImage(
                        contentResolver,
                        bitMapImagen,
                        "Imagen",
                        "Imagen de la cuenta"
                    )
                    Glide.with(this).load(bitMapImagen).into(imagen)
                    Toast.makeText(this, "Imagen cargada", Toast.LENGTH_SHORT).show()
                    guardarUriImagen(imagenURI.toString())
                    obtenerUriImagen()
                }
            }else{
                Toast.makeText(this, "No se pudo cargar la imagen", Toast.LENGTH_SHORT).show()
            }
        }
    }
    //URI de las imágenes
    private fun guardarUriImagen(uri: String?) {
        val sharedPreferences = getSharedPreferences("preferencias_imagen", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("uri_imagen", uri)
        editor.apply()
    }

    private fun obtenerUriImagen(): String? {
        val sharedPreferences = getSharedPreferences("preferencias_imagen", MODE_PRIVATE)
        Log.i("URI", sharedPreferences.getString("uri_imagen", null).toString())
        return sharedPreferences.getString("uri_imagen", null)
    }

    private fun seterRegistrar(buttonRegistrarse: Button, nombreInput: EditText, apellidoInput: EditText, emailInput: EditText, passwordInput: EditText, identificacionInput: EditText, latitud : EditText, longitud : EditText ) {
        buttonRegistrarse.setOnClickListener {
            val nombre = nombreInput.text.toString()
            val apellido = apellidoInput.text.toString()
            val email = emailInput.text.toString()
            val password = passwordInput.text.toString()
            val identificacion = identificacionInput.text.toString()
            val latitud = latitud.text.toString()
            val longitud = longitud.text.toString()
            val uriImagen = obtenerUriImagen()
            if (nombre.isEmpty() || apellido.isEmpty() || email.isEmpty() ||  identificacion.isEmpty() || latitud.isEmpty() || longitud.isEmpty() || uriImagen == null){
                Toast.makeText(this, "No se ha llenado algun campo del Registro", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!email.contains("@")) {
                Toast.makeText(this, "El correo electrónico debe contener un @", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Crear una autentificacion de usuario y guardar los datos en la base de datos realtime database
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        updateData(user, nombre, apellido, email, password, identificacion, latitud, longitud)
                        registrarUsuarioFirebaseStorage(user)
                    } else {
                        Toast.makeText(baseContext, "Authentication failed.",
                            Toast.LENGTH_SHORT).show()
                    }
                }

            startActivity(Intent(this, InicioActivity::class.java))
        }
    }
    private fun registrarUsuarioFirebaseStorage(user : FirebaseUser?){
        val uriImagen = obtenerUriImagen()
        val imagenRef = storage.reference.child("images/${auth.currentUser!!.uid}/${auth.currentUser!!.uid}")
        imagenRef.putFile(uriImagen!!.toUri())
            .addOnSuccessListener(object: OnSuccessListener<UploadTask.TaskSnapshot>{
                override fun onSuccess(taskSnapshot: UploadTask.TaskSnapshot){
                    Log.i("STORAGE", "Imagen cargada exitosamente")
                }
            })
            .addOnFailureListener(object: OnFailureListener {
                override fun onFailure(exception: Exception){
                    Log.e("STORAGE", "Error al cargar la imagen", exception)
                }
            })
    }
    public fun updateData(user: FirebaseUser?, nombre: String, apellido: String, email: String, password : String, identificacion: String, latitud: String, longitud: String) {
        if (user != null) {
            val database = Firebase.database
            val userId = user.uid
            val user = Usuario()
            user.nombre = nombre
            user.apellido = apellido
            user.email = email
            user.password = password
            user.identificacion = identificacion
            user.latitud = latitud
            user.longitud = longitud
            user.disponible = false
            val userRef = database.getReference(PATH_USERS + userId)
            userRef.setValue(user)
        }
    }
    private fun myLocation(latitud: EditText, longitud: EditText){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request location permissions if not granted
            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location : Location? ->
                // Got last known location. In some rare situations this can be null.
                if (location != null) {
                    latitud.setText(location.latitude.toString())
                    longitud.setText(location.longitude.toString())
                }
            }
    }
}
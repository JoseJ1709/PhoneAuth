package com.example.phoneauth

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.database
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import org.json.JSONObject
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration
import org.osmdroid.library.BuildConfig
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.drawing.OsmPath
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.IOException
import java.io.InputStream

class MapsActivity : AppCompatActivity() {
    //mapas
    private lateinit var map: MapView
    private lateinit var locationOverlay: MyLocationNewOverlay
    private var latitud: Double = 0.0
    private var longitud: Double = 0.0
    private val startPoint = org.osmdroid.util.GeoPoint(4.628593, -74.065041)
    //base de datos
    private lateinit var auth: FirebaseAuth
    val PATH_USERS = "users/"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        //definir la variable de autenticación para actualizar
        auth = Firebase.auth
        //Empezar con el mapa
        Configuration.getInstance().setUserAgentValue(BuildConfig.BUILD_TYPE)
        //configurando tool bar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        map = findViewById(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setBuiltInZoomControls(true)
        map.setMultiTouchControls(true)
        //poniendo los puntos en el mapa
        setPoints(map)
        inicializarUbicacion()
    }
    @SuppressLint("MissingPermission")
    private fun inicializarUbicacion() {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        if (location != null) {
            latitud = location.latitude
            longitud = location.longitude
            setMylocation(latitud, longitud)
        }
    }

    //Opciones de menu
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu,menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean
    {
        // Handle item selection
        return when (item.itemId)
        {
            R.id.menuLogOut ->
            {
                val database = Firebase.database
                val uid = auth.currentUser?.uid
                val usuarioActual = database.getReference(PATH_USERS + uid)
                usuarioActual.child("disponible").setValue(false)
                auth.signOut()
                val intentLogOut = Intent(this, InicioActivity::class.java)
                intentLogOut.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intentLogOut)
                finish()

                true
            }
            R.id.menuToggleStatus ->
            {
                val database = Firebase.database
                val uid = auth.currentUser?.uid
                val usuarioActual = database.getReference(PATH_USERS + uid)
                usuarioActual.child("disponible").get().addOnSuccessListener { availableSnapshot ->
                    val isAvailable = availableSnapshot.getValue(Boolean::class.java) ?: false
                    usuarioActual.child("disponible").setValue(!isAvailable)
                    val statusText = if (!isAvailable) "disponible" else "no disponible"
                    Toast.makeText(this, "Ahora te encuentras $statusText", Toast.LENGTH_SHORT).show()
                }
                true
            }
            R.id.menuAvailableUsers ->
            {
                val intentAvailableUsers = Intent(this, UsuariosActivity::class.java)
                startActivity(intentAvailableUsers)
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    private fun setPoints(map: MapView) {
        //cargando y poniendo los puntos en el mapa
        MarcadoresJSON()
    }
    private val locationListener: LocationListener = object : LocationListener {
        //Método que se ejecuta cuando la ubicación cambia, actualizando el objeto location
        override fun onLocationChanged(location: Location) {
            Log.d("MapsActivity", "Ubicación actualizada: $location")
            latitud = location.latitude
            longitud = location.longitude
            Log.i("LISTENER", "Latitud: $latitud y Longitud: $longitud")
            startPoint.latitude = latitud
            startPoint.longitude = longitud
            setMylocation(latitud, longitud)
            actualizarUbicacionUsuarioDatabase(latitud, longitud)
        }
    }
    //poniendo my locacion en el mapa
    private var marcador: Marker? = null
    private fun setMylocation(latitud: Double, longitud: Double) {
        if (marcador != null){
            marcador?.let { map.overlays.remove(it)}
        }

        val punto = GeoPoint(latitud, longitud)
            marcador = Marker(map).apply{
            icon = cambioTamañoIcono(resources.getDrawable(R.drawable.ubicacion_json))
            position = punto
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        map.overlays.add(marcador)
        val mapController = map.controller
        mapController.animateTo(punto)
        mapController.setZoom(18.0)
    }
    //cambiando el color de mi icono
    private fun cambioTamañoIcono(icono: Drawable): Drawable {
        val bitmap = (icono as BitmapDrawable).bitmap
        val bitmapCambiado = Bitmap.createScaledBitmap(bitmap, 50, 50, false)
        return BitmapDrawable(resources, bitmapCambiado)
    }

    //cargar el archivo JSON
    private fun loadJSONLocations(): String? {
        var json: String? = null
        try{
            val istream: InputStream = assets.open( "locations.json")
            val size: Int = istream.available()
            val buffer = ByteArray(size)
            istream.read(buffer)
            istream.close()
            json = String(buffer, Charsets. UTF_8)
        }catch (ex: IOException) {
            ex.printStackTrace()
            return null
        }
        return json
    }
    //usa la funcion de cargar el archivo y pone los puntos en el mapa
    private fun MarcadoresJSON(){
        val datosJSON = JSONObject(loadJSONLocations())
        val datosLocalizacion = datosJSON.getJSONObject("locations")

        for( i in datosLocalizacion.keys()){
            val dato = datosLocalizacion.getJSONObject(i)
            val latitudJson = dato.getDouble("latitude")
            val longitudJson = dato.getDouble("longitude")
            val nombreJson = dato.getString("name")

            val punto = GeoPoint(latitudJson, longitudJson)
            val marcadorJson = Marker(map).apply {
                position = punto
                title = nombreJson
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            map.overlays.add(marcadorJson)
        }
    }
    //Actualización ubicación en el Realtime database
    private fun actualizarUbicacionUsuarioDatabase(latitud: Double, longitud: Double){
        val database = Firebase.database
        val uid = auth.currentUser?.uid
        if (uid != null){
            val usuarioActual = database.getReference(PATH_USERS + uid)
            usuarioActual.child("latitud").setValue(latitud.toString())
            usuarioActual.child("longitud").setValue(longitud.toString())
        }
    }
    //Código relacionado a la ubicación
    @SuppressLint("MissingPermission")
    private fun ubicacionActual() {
        //Solicitud de ubicación cada 10 s de acuerdo al objeto locationListener
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000L, 10f, locationListener)
    }
    //Código relacionado al ciclo de vida
    override fun onResume() {
        super.onResume()
        map.onResume()
        ubicacionActual()
        val mapController = map.controller
        mapController.setZoom(18.0)
        mapController.setCenter(this.startPoint)
    }

    override fun onStart() {
        super.onStart()

    }
    override fun onPause() {
        super.onPause()
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        locationManager.removeUpdates(locationListener)
        map.onPause()
    }
}
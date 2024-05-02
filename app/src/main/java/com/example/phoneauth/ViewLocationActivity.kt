package com.example.phoneauth

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import org.osmdroid.config.Configuration
import org.osmdroid.library.BuildConfig
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import org.osmdroid.views.overlay.Polyline

class ViewLocationActivity : AppCompatActivity() {
    private lateinit var map: MapView
    private lateinit var locationOverlay: MyLocationNewOverlay
    private var latitud: Double = 0.0
    private var longitud: Double = 0.0
    private var latitud2: Double = 0.0
    private var longitud2: Double = 0.0
    private val startPoint = org.osmdroid.util.GeoPoint(4.628593, -74.065041)
    private lateinit var auth: FirebaseAuth
    val PATH_USERS = "users/"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_viewlocation)
        auth = FirebaseAuth.getInstance()
        val latitud = intent.getStringExtra("latitud")
        val longitud = intent.getStringExtra("longitud")
        Configuration.getInstance().setUserAgentValue(BuildConfig.BUILD_TYPE)
        map = findViewById(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setBuiltInZoomControls(true)
        map.setMultiTouchControls(true)
        latitud2 = latitud!!.toDouble()
        longitud2 = longitud!!.toDouble()
        Toast.makeText(this, "Latitud: $latitud2 y Longitud: $longitud2", Toast.LENGTH_SHORT).show()
    }
    private fun setRoute(latitud2: Double, longitud2: Double) {
        // Clear all overlays
        map.overlays.clear()

        val startPoint = GeoPoint(latitud.toDouble(), longitud.toDouble())
        val endPoint = GeoPoint(latitud2, longitud2)

        val mapController = map.controller
        mapController.setZoom(18.0)
        mapController.setCenter(startPoint)

        val startMarker = Marker(map)
        startMarker.position = startPoint
        map.overlays.add(startMarker)

        val endMarker = Marker(map)
        endMarker.position = endPoint
        map.overlays.add(endMarker)

        // Create a Polyline
        val line = Polyline()
        line.addPoint(startPoint)
        line.addPoint(endPoint)
        line.color = Color.RED
        map.overlays.add(line)

        map.invalidate() // Refresh the map
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
            setRoute(latitud2, longitud2)
            actualizarUbicacionUsuarioDatabase(latitud, longitud)
        }
    }
    //poniendo my locacion en el mapa
    private var marcador: Marker? = null

    //cambiando el color de mi icono
    private fun cambioTamañoIcono(icono: Drawable): Drawable {
        val bitmap = (icono as BitmapDrawable).bitmap
        val bitmapCambiado = Bitmap.createScaledBitmap(bitmap, 50, 50, false)
        return BitmapDrawable(resources, bitmapCambiado)
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

    override fun onPause() {
        super.onPause()
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        locationManager.removeUpdates(locationListener)
        map.onPause()
    }
}
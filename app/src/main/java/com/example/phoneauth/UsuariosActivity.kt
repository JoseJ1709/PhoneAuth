package com.example.phoneauth

import android.os.Bundle
import android.util.Log
import android.widget.ListView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.phoneauth.AdapterUser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage

class UsuariosActivity : AppCompatActivity() {

    private lateinit var adapterU: AdapterUser
    val PATH_USERS = "users/"
    private lateinit var users: MutableList<Usuario>
    private lateinit var auth: FirebaseAuth
    private lateinit var listener1 : ChildEventListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_usuarios)
        //cargar el JSON de usuarios y filtrarlo

        auth = Firebase.auth
        users = mutableListOf()
        UploadJSONUsers()
    }
    private fun UploadJSONUsers() {
        val database = Firebase.database
        val myRef = database.getReference("users/")



        listener1 = myRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val myUser = snapshot.getValue(Usuario::class.java)
                updateUserList(myUser)
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val myUser = snapshot.getValue(Usuario::class.java)
                updateUserList(myUser)
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                val myUser = snapshot.getValue(Usuario::class.java)
                updateUserList(myUser)
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                // Handle child movement
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FIREBASE", "Error al leer los datos", error.toException())
            }
        })
    }

    private fun updateUserList(myUser: Usuario?) {




        val name = myUser?.nombre
        val longitud = myUser?.longitud
        val latitud = myUser?.latitud
        val disp = myUser?.disponible
        val llave = myUser?.key
        if (name != null && longitud != null && latitud != null && disp != null) {
            var user = Usuario()
            user.nombre = name
            user.longitud = longitud
            user.latitud = latitud
            user.disponible = disp
            user.key = llave!!
            if (disp) {
                users.add(user)
                Toast.makeText(this@UsuariosActivity, "$name se ha conectado", Toast.LENGTH_SHORT).show()
            } else {
                users.removeAll { it.nombre == name }
                Toast.makeText(this@UsuariosActivity, "$name se ha desconectado", Toast.LENGTH_SHORT).show()
            }
            updateListView()
        }
    }

    private fun updateListView() {
        val list = findViewById<ListView>(R.id.lita_usuarios)
        val filteredUsers = users.filter { it.disponible }
            .distinctBy { it.nombre }
            .toMutableList()

        if(filteredUsers.isEmpty()){
            Toast.makeText(this@UsuariosActivity, "No hay usuarios disponibles", Toast.LENGTH_SHORT).show()
            return
        }
        adapterU = AdapterUser(this@UsuariosActivity, filteredUsers)
        list.adapter = adapterU
    }

    override fun onStop() {
        super.onStop()
        val database = Firebase.database
        val uid = auth.currentUser?.uid
        val myRef = database.getReference("users/$uid")
        myRef.removeEventListener(listener1)
    }
}
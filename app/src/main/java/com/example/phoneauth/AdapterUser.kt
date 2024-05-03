package com.example.phoneauth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity
import com.bumptech.glide.Glide
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.storage.FirebaseStorage
import android.util.Log

class AdapterUser(private val context: Context?, private var facts: MutableList<Usuario>): BaseAdapter(){
    var onDataChangeListener: OnDataChangeListener? = null
    override fun getCount(): Int {
        return facts.size
    }
    override fun getItemId(position: Int): Long {
        return position.toLong() // You can use a unique ID if available
    }
    override fun getItem(position: Int): Usuario {
        return facts[position]
    }
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.view_user1, parent, false)
        val imagen = view.findViewById<ImageView>(R.id.image)
        val nombre = view.findViewById<TextView>(R.id.nombre)
        val ver = view.findViewById<Button>(R.id.button_ver)
        val datoObject = getItem(position)
        val storage = FirebaseStorage.getInstance()
        val storageReference = storage.getReference().child("images/${datoObject.key}/${datoObject.key}")
        Log.d("Firebase", "URL de la imagen: images/${datoObject.key}/${datoObject.key}")
        storageReference.downloadUrl.addOnSuccessListener { imageUrl ->

            Glide.with(context!!)
                .load(imageUrl)
                .into(imagen)
        }.addOnFailureListener { exception ->
            Log.e("Firebase", "Error al descargar la imagen", exception)
        }
        nombre?.text = datoObject.nombre
        ver.setOnClickListener {
            val position = Bundle()
            position.putString("longitud", datoObject.longitud)
            position.putString("latitud", datoObject.latitud)
            val intent = Intent(context, ViewLocationActivity::class.java)
            intent.putExtras(position)
            context?.startActivity(intent)
        }


        return view
    }

    interface OnDataChangeListener {
        fun onDataChanged()
    }
}
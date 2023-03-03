package com.leoxtech.statussaver

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView

class StatusAdapter(private val context: Context, private val modelClass: ArrayList<ModelClass>, private val clickListener:(ModelClass)->Unit):
    RecyclerView.Adapter<StatusAdapter.StatusViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatusViewHolder {
        return StatusViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.status_item, parent, false))
    }

    override fun onBindViewHolder(holder: StatusViewHolder, position: Int) {
        if (modelClass[position].fileUri.endsWith(".mp4")){
            holder.cardStatusVid.visibility = View.VISIBLE
            holder.imgVidIcon.visibility = View.VISIBLE
        }else{
            holder.cardStatusVid.visibility = View.GONE
            holder.imgVidIcon.visibility = View.GONE
        }

        Glide.with(context).load(Uri.parse(modelClass[position].fileUri)).into(holder.imgStatus)

        holder.cardStatus.setOnClickListener {
            clickListener(modelClass[position])
        }

    }

    override fun getItemCount(): Int {
        return modelClass.size
    }

    class StatusViewHolder(itemView: View):RecyclerView.ViewHolder(itemView) {
        val imgStatus:ImageView = itemView.findViewById(R.id.imgStatus)
        val cardStatusVid:MaterialCardView = itemView.findViewById(R.id.cardStatusVid)
        val imgVidIcon:ImageView = itemView.findViewById(R.id.imgVidIcon)
        val cardStatus:MaterialCardView = itemView.findViewById(R.id.cardStatus)
    }

}
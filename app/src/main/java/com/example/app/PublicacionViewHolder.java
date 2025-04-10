package com.example.app;

import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;

public class PublicacionViewHolder extends RecyclerView.ViewHolder {

    public ImageView profileImage;
    public TextView tvNombre, tvPrecio, tvTitulo, tvUbicacion, tvFecha, tvDescripcionLarga;
    public Chip tvCategoria;
    public Button btnVerMas, btnHandymatch;

    public PublicacionViewHolder(@NonNull View itemView) {
        super(itemView);

        profileImage = itemView.findViewById(R.id.profileImage);
        tvNombre = itemView.findViewById(R.id.tvNombre);
        tvPrecio = itemView.findViewById(R.id.tvPrecio);
        tvTitulo = itemView.findViewById(R.id.tvTitulo);
        tvCategoria = itemView.findViewById(R.id.tvCategoria);
        tvUbicacion = itemView.findViewById(R.id.tvUbicacion);
        tvFecha = itemView.findViewById(R.id.tvFecha);
        tvDescripcionLarga = itemView.findViewById(R.id.tvDescripcionLarga);
        btnVerMas = itemView.findViewById(R.id.btnVerMas);
        btnHandymatch = itemView.findViewById(R.id.btnHandymatch);
    }
}


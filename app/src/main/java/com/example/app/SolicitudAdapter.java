package com.example.app;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class SolicitudAdapter extends RecyclerView.Adapter<SolicitudAdapter.ViewHolder> {

    private static final String TAG = "SolicitudAdapter";
    private final List<Solicitud> listaSolicitudes;
    private final Context context;

    public SolicitudAdapter(List<Solicitud> listaSolicitudes, Context context) {
        this.listaSolicitudes = listaSolicitudes;
        this.context = context;
        Log.d(TAG, "Adapter creado con listaSolicitudes size: " + listaSolicitudes.size());
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nombrePostulante;
        TextView tituloPublicacion;
        ImageView imagenPostulante;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            nombrePostulante = itemView.findViewById(R.id.tvNombre);
            tituloPublicacion = itemView.findViewById(R.id.tvTitulo);
            imagenPostulante = itemView.findViewById(R.id.profileImage);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.card_solicitud, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (position >= listaSolicitudes.size()) {
            Log.e(TAG, "Posición inválida: " + position + ", tamaño de lista: " + listaSolicitudes.size());
            return;
        }

        Solicitud solicitud = listaSolicitudes.get(position);

        // Inicializar vistas
        holder.nombrePostulante.setText("Cargando...");
        holder.tituloPublicacion.setText("Cargando...");
        holder.imagenPostulante.setImageResource(R.drawable.usuario);

        String idUsuarioPostulante = solicitud.getIdUsuarioPostulante();
        String idPublicacion = solicitud.getIdPublicacion();

        if (idUsuarioPostulante == null || idUsuarioPostulante.isEmpty()) {
            Log.e(TAG, "ID de usuario postulante es null o vacío en posición " + position);
            holder.nombrePostulante.setText("ID de usuario vacío");
            return;
        }

        if (idPublicacion == null || idPublicacion.isEmpty()) {
            Log.e(TAG, "ID de publicación es null o vacío para usuario: " + idUsuarioPostulante);
            holder.tituloPublicacion.setText("ID de publicación vacío");
            return;
        }

        // Obtener datos del usuario postulante
        FirebaseDatabase.getInstance().getReference("usuarios")
                .child(idUsuarioPostulante)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            Log.e(TAG, "El usuario con ID " + idUsuarioPostulante + " no existe");
                            holder.nombrePostulante.setText("Usuario no encontrado");
                            return;
                        }

                        String nombre = snapshot.child("nombre").getValue(String.class);
                        String imagenUrl = snapshot.child("imagenUrl").getValue(String.class);

                        holder.nombrePostulante.setText(
                                (nombre != null && !nombre.isEmpty()) ? nombre : "Sin nombre"
                        );

                        if (imagenUrl != null && !imagenUrl.isEmpty()) {
                            RequestOptions options = new RequestOptions()
                                    .placeholder(R.drawable.usuario)
                                    .error(R.drawable.usuario);

                            Glide.with(holder.itemView.getContext())
                                    .load(imagenUrl)
                                    .apply(options)
                                    .into(holder.imagenPostulante);
                        } else {
                            holder.imagenPostulante.setImageResource(R.drawable.usuario);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error al cargar usuario: " + error.getMessage());
                        holder.nombrePostulante.setText("Error al cargar usuario");
                    }
                });

        // Obtener datos de la publicación
        FirebaseDatabase.getInstance().getReference("publicaciones")
                .child(idPublicacion)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            Log.e(TAG, "Publicación con ID " + idPublicacion + " no existe");
                            holder.tituloPublicacion.setText("Publicación no encontrada");
                            return;
                        }

                        String titulo = snapshot.child("titulo").getValue(String.class);

                        if (titulo != null && !titulo.isEmpty()) {
                            holder.tituloPublicacion.setText(titulo);
                        } else {
                            holder.tituloPublicacion.setText("Sin título");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error al cargar título: " + error.getMessage());
                        holder.tituloPublicacion.setText("Error al cargar título");
                    }
                });
    }

    @Override
    public int getItemCount() {
        return listaSolicitudes.size();
    }
}

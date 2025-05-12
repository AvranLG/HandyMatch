package com.example.app;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ConversacionesAdapter extends RecyclerView.Adapter<ConversacionesAdapter.ConversacionViewHolder> {

    private List<Conversacion> conversaciones;
    private Context context;
    private OnItemClickListener listener;

    // Interfaz para manejar los clics en las conversaciones
    public interface OnItemClickListener {
        void onItemClick(Conversacion conversacion);
    }

    // Constructor que acepta el listener
    public ConversacionesAdapter(List<Conversacion> conversaciones, Context context, OnItemClickListener listener) {
        this.conversaciones = conversaciones;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ConversacionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.card_chat, parent, false);
        return new ConversacionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ConversacionViewHolder holder, int position) {
        Conversacion conversacion = conversaciones.get(position);

        // Rellenamos los datos del card
        holder.tvNombre.setText(conversacion.getNombre());
        holder.tvUltimoMensaje.setText(conversacion.getUltimoMensaje());
        holder.tvHora.setText(formatHora(conversacion.getTimestamp()));

        // Cargar imagen de perfil (por ejemplo con Glide o Picasso)
        Glide.with(context)
                .load(conversacion.getFotoUrl())
                .circleCrop()
                .into(holder.profileImage);

        // Click para abrir el detalle de la conversación
        holder.itemView.setOnClickListener(v -> abrirDetalleConversacion(conversacion));
    }

    @Override
    public int getItemCount() {
        return conversaciones.size();
    }

    // Metodo para actualizar una conversación específica
    public void actualizarConversacion(Conversacion nuevaConversacion) {
        for (int i = 0; i < conversaciones.size(); i++) {
            if (conversaciones.get(i).getUidOtro().equals(nuevaConversacion.getUidOtro())) {
                conversaciones.set(i, nuevaConversacion);
                notifyItemChanged(i); // Notifica el cambio en esa posición
                break;
            }
        }
    }

    public static class ConversacionViewHolder extends RecyclerView.ViewHolder {

        TextView tvNombre, tvUltimoMensaje, tvHora;
        ImageView profileImage;

        public ConversacionViewHolder(View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvNombre);
            tvUltimoMensaje = itemView.findViewById(R.id.tvMensaje);
            tvHora = itemView.findViewById(R.id.tvHora);
            profileImage = itemView.findViewById(R.id.profileImage);
        }
    }

    // Metodo para formatear la hora
    private String formatHora(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    // Metodo para abrir la actividad de detalle de conversación
    private void abrirDetalleConversacion(Conversacion conversacion) {
        Intent intent = new Intent(context, DetalleConversacionActivity.class);
        intent.putExtra("uidOtroUsuario", conversacion.getUidOtro());
        intent.putExtra("nombre", conversacion.getNombre());
        intent.putExtra("fotoUrl", conversacion.getFotoUrl());
        context.startActivity(intent);
    }
}

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
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ConversacionesAdapter extends RecyclerView.Adapter<ConversacionesAdapter.ConversacionViewHolder> {

    private List<Conversacion> conversaciones;
    private Context context;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Conversacion conversacion);
    }

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

        holder.tvNombre.setText(conversacion.getNombre());
        holder.tvUltimoMensaje.setText(conversacion.getUltimoMensaje());
        holder.tvHora.setText(formatHora(conversacion.getTimestamp()));

        // Imagen de perfil
        Glide.with(context)
                .load(conversacion.getFotoUrl())
                .circleCrop()
                .into(holder.profileImage);

        // Ocultar insignia por defecto
        holder.verifiedBadge.setVisibility(View.GONE);

        // Consultar si el usuario está verificado en Firebase
        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("usuarios")
                .child(conversacion.getUidOtro());

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean verificado = snapshot.child("verificado").getValue(Boolean.class);
                boolean mostrarInsignia = verificado != null && verificado;

                holder.verifiedBadge.setVisibility(mostrarInsignia ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // En caso de error, se deja la insignia oculta
                holder.verifiedBadge.setVisibility(View.GONE);
            }
        });

        // Evento clic
        holder.itemView.setOnClickListener(v -> abrirDetalleConversacion(conversacion));
    }

    @Override
    public int getItemCount() {
        return conversaciones.size();
    }

    public void actualizarConversacion(Conversacion nuevaConversacion) {
        for (int i = 0; i < conversaciones.size(); i++) {
            if (conversaciones.get(i).getUidOtro().equals(nuevaConversacion.getUidOtro())) {
                conversaciones.set(i, nuevaConversacion);
                notifyItemChanged(i);
                break;
            }
        }
    }

    public static class ConversacionViewHolder extends RecyclerView.ViewHolder {

        TextView tvNombre, tvUltimoMensaje, tvHora;
        ImageView profileImage, verifiedBadge;

        public ConversacionViewHolder(View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvNombre);
            tvUltimoMensaje = itemView.findViewById(R.id.tvMensaje);
            tvHora = itemView.findViewById(R.id.tvHora);
            profileImage = itemView.findViewById(R.id.profileImage);
            verifiedBadge = itemView.findViewById(R.id.verifiedBadge); // Asegúrate que este ID exista en card_chat.xml
        }
    }

    private String formatHora(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    private void abrirDetalleConversacion(Conversacion conversacion) {
        Intent intent = new Intent(context, DetalleConversacionActivity.class);
        intent.putExtra("uidOtroUsuario", conversacion.getUidOtro());
        intent.putExtra("nombre", conversacion.getNombre());
        intent.putExtra("fotoUrl", conversacion.getFotoUrl());
        context.startActivity(intent);
    }
}

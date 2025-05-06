package com.example.app;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ConversacionesAdapter extends RecyclerView.Adapter<ConversacionesAdapter.ConversacionViewHolder> {

    private List<Conversacion> conversaciones;
    private Context context;
    private OnItemClickListener listener;  // Agregamos el listener

    // Interfaz para manejar los clics en las conversaciones
    public interface OnItemClickListener {
        void onItemClick(Conversacion conversacion);
    }

    // Modificamos el constructor para aceptar el listener
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

        // Agregamos el OnClickListener al itemView para manejar el clic
        holder.itemView.setOnClickListener(v -> {
            // Obtener el uid del otro usuario
            String uidOtroUsuario = conversacion.getUidOtro();

            // Crear un Bundle y agregar el uidOtroUsuario
            Bundle bundle = new Bundle();
            bundle.putString("uidOtroUsuario", uidOtroUsuario);

            // Crear el fragmento y pasarle el Bundle
            DetalleConversacionFragment detalleFragment = new DetalleConversacionFragment();
            detalleFragment.setArguments(bundle);

            // Iniciar el fragmento
            ((AppCompatActivity) context).getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, detalleFragment)
                    .addToBackStack(null)
                    .commit();
        });
    }

    @Override
    public int getItemCount() {
        return conversaciones.size();
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

    // Metodo para formatear la hora (puedes personalizarlo)
    private String formatHora(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}

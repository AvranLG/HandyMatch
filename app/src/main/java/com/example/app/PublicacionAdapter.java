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
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class PublicacionAdapter extends RecyclerView.Adapter<PublicacionAdapter.PublicacionViewHolder> {

    private List<Publicacion> listaPublicaciones;
    private Context context;

    public PublicacionAdapter(List<Publicacion> listaPublicaciones, Context context) {
        this.listaPublicaciones = listaPublicaciones;
        this.context = context;
    }

    @NonNull
    @Override
    public PublicacionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.card_trabajo, parent, false);
        return new PublicacionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PublicacionViewHolder holder, int position) {
        Log.d("PublicacionAdapter", "Vinculando elemento en posición: " + position);
        Publicacion publicacion = listaPublicaciones.get(position);

        holder.tvTitulo.setText(publicacion.getTitulo());
        holder.tvDescripcion.setText(publicacion.getDescripcion());
        holder.tvFechaHora.setText(publicacion.getFechaHora());
        holder.tvUbicacion.setText(publicacion.getUbicacion());
        holder.tvPrecio.setText("$" + publicacion.getPago()); // Agregar signo de pesos
        holder.tvCategoria.setText(publicacion.getCategoria()); // Mostrar categoría en el chip

        int colorCategoria = getColorForCategoria(publicacion.getCategoria());
        holder.tvCategoria.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(colorCategoria));


        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("usuarios").child(publicacion.getIdUsuario());

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String nombreUsuario = dataSnapshot.child("nombre").getValue(String.class);
                    String tipoLogin = dataSnapshot.child("tipoLogin").getValue(String.class);
                    String fotoUrl = dataSnapshot.child("imagenUrl").getValue(String.class);

                    holder.tvNombre.setText(nombreUsuario);

                    if ("google".equals(tipoLogin)) {
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        if (user != null && user.getPhotoUrl() != null) {
                            Glide.with(context)
                                    .load(user.getPhotoUrl())
                                    .placeholder(R.drawable.usuario)
                                    .error(R.drawable.usuario)
                                    .circleCrop()
                                    .into(holder.profileImage);
                        }
                    } else {
                        Glide.with(context)
                                .load(fotoUrl)
                                .placeholder(R.drawable.usuario)
                                .error(R.drawable.usuario)
                                .circleCrop()
                                .into(holder.profileImage);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("PublicacionAdapter", "Error al obtener el usuario", databaseError.toException());
            }
        });
    }

    @Override
    public int getItemCount() {
        return listaPublicaciones.size();
    }

    public void agregarPublicacion(Publicacion publicacion) {
        listaPublicaciones.add(publicacion);
        notifyItemInserted(listaPublicaciones.size() - 1);
    }

    public static class PublicacionViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitulo, tvDescripcion, tvFechaHora, tvUbicacion, tvPrecio, tvNombre;
        ImageView profileImage;
        Chip tvCategoria; // <-- Chip para categoría

        public PublicacionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitulo = itemView.findViewById(R.id.tvTitulo);
            tvDescripcion = itemView.findViewById(R.id.tvDescripcionLarga);
            tvFechaHora = itemView.findViewById(R.id.tvFecha);
            tvUbicacion = itemView.findViewById(R.id.tvUbicacion);
            tvPrecio = itemView.findViewById(R.id.tvPrecio);
            tvNombre = itemView.findViewById(R.id.tvNombre);
            profileImage = itemView.findViewById(R.id.profileImage);
            tvCategoria = itemView.findViewById(R.id.tvCategoria); // <-- Asignación del chip
        }
    }
    private int getColorForCategoria(String categoria) {
        switch (categoria.toLowerCase()) {
            case "hogar":
                return android.graphics.Color.parseColor("#FFE5B4");
            case "mascotas":
                return android.graphics.Color.parseColor("#C1E1C1");
            case "tecnología":
                return android.graphics.Color.parseColor("#B3E5FC");
            case "eventos":
                return android.graphics.Color.parseColor("#FFD1DC");
            case "transporte":
                return android.graphics.Color.parseColor("#D1C4E9");
            case "salud y bienestar":
                return android.graphics.Color.parseColor("#C8E6C9");
            case "educación":
                return android.graphics.Color.parseColor("#FFF9C4");
            case "negocios":
                return android.graphics.Color.parseColor("#D7CCC8");
            case "reparaciones":
                return android.graphics.Color.parseColor("#FFCCBC");
            default:
                return android.graphics.Color.parseColor("#E0E0E0"); // gris por defecto
        }
    }

}

package com.example.app;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
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

        // Configurar los campos de la publicación
        holder.tvTitulo.setText(publicacion.getTitulo());
        holder.tvDescripcion.setText(publicacion.getDescripcion());
        holder.tvFechaHora.setText(publicacion.getFechaHora());
        holder.tvUbicacion.setText(publicacion.getUbicacion());

        // Añadir signo de pesos al precio
        String precioConSimbolo = "$" + publicacion.getPago();
        holder.tvPrecio.setText(precioConSimbolo);

        // Obtener el usuario para cargar su nombre y foto
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("usuarios").child(publicacion.getIdUsuario());

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String nombreUsuario = dataSnapshot.child("nombre").getValue(String.class);
                    String tipoLogin = dataSnapshot.child("tipoLogin").getValue(String.class); // Verificar tipoLogin
                    String fotoUrl = dataSnapshot.child("imagenUrl").getValue(String.class); // URL de la imagen en Supabase

                    holder.tvNombre.setText(nombreUsuario);

                    // Si el usuario se registró con Google
                    if ("google".equals(tipoLogin)) {
                        // Obtener la foto de perfil de Google usando el FirebaseUser
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        if (user != null && user.getPhotoUrl() != null) {
                            Glide.with(context)
                                    .load(user.getPhotoUrl())  // Foto de perfil de Google
                                    .placeholder(R.drawable.usuario)  // Icono por defecto mientras se carga la imagen
                                    .error(R.drawable.usuario)       // Icono por defecto si falla la carga de la imagen
                                    .circleCrop()                   // Redondear la imagen
                                    .into(holder.profileImage);
                        }
                    } else {
                        // Si no es de Google, usar la foto de Supabase
                        Glide.with(context)
                                .load(fotoUrl)  // Foto de perfil de Supabase
                                .placeholder(R.drawable.usuario)  // Icono por defecto mientras se carga la imagen
                                .error(R.drawable.usuario)       // Icono por defecto si falla la carga de la imagen
                                .circleCrop()                   // Redondear la imagen
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
        int count = listaPublicaciones.size();
        Log.d("PublicacionAdapter", "getItemCount llamado: " + count + " elementos");
        return count;
    }

    // Metodo para agregar una nueva publicación a la lista y notificar al adaptador
    public void agregarPublicacion(Publicacion publicacion) {
        listaPublicaciones.add(publicacion);
        notifyItemInserted(listaPublicaciones.size() - 1); // Notificar que se insertó un nuevo ítem
    }

    public static class PublicacionViewHolder extends RecyclerView.ViewHolder {

        TextView tvTitulo, tvDescripcion, tvFechaHora, tvUbicacion, tvPrecio, tvNombre;
        ImageView profileImage;

        public PublicacionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitulo = itemView.findViewById(R.id.tvTitulo);
            tvDescripcion = itemView.findViewById(R.id.tvDescripcionLarga);
            tvFechaHora = itemView.findViewById(R.id.tvFecha);
            tvUbicacion = itemView.findViewById(R.id.tvUbicacion);
            tvPrecio = itemView.findViewById(R.id.tvPrecio);
            tvNombre = itemView.findViewById(R.id.tvNombre);
            profileImage = itemView.findViewById(R.id.profileImage);
        }
    }
}

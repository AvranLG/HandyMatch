package com.example.app;

import android.content.Context;
import android.content.res.ColorStateList;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;

public class PublicacionAdapter extends RecyclerView.Adapter<PublicacionAdapter.PublicacionViewHolder> {

    private List<Publicacion> listaPublicaciones;
    private Context context;
    private int itemExpandedPosition = -1;

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
        holder.tvPrecio.setText("$" + publicacion.getPago());
        holder.tvCategoria.setText(publicacion.getCategoria());

        int colorCategoria = getColorForCategoria(publicacion.getCategoria());
        holder.tvCategoria.setChipBackgroundColor(ColorStateList.valueOf(colorCategoria));

        boolean isExpanded = (position == itemExpandedPosition);
        holder.tvDescripcion.setMaxLines(isExpanded ? Integer.MAX_VALUE : 2);
        holder.tvDescripcion.setEllipsize(isExpanded ? null : TextUtils.TruncateAt.END);
        holder.verMasButton.setText(isExpanded ? "Ver menos..." : "Ver más...");
        holder.mapView.setVisibility(isExpanded ? View.VISIBLE : View.GONE);

        if (isExpanded) {
            holder.mapView.onCreate(null);
            holder.mapView.onResume();

            double latitud = publicacion.getLatitud();
            double longitud = publicacion.getLongitud();
            LatLng coordenadas = new LatLng(latitud, longitud);

            holder.mapView.getMapAsync(googleMap -> {
                MapsInitializer.initialize(holder.mapView.getContext().getApplicationContext());
                googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

                googleMap.clear();
                googleMap.addMarker(new MarkerOptions().position(coordenadas).title("Ubicación del trabajo"));
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(coordenadas, 16));
            });
        }

        holder.verMasButton.setOnClickListener(v -> {
            int currentPosition = holder.getAdapterPosition();
            if (currentPosition == RecyclerView.NO_POSITION) return;

            if (itemExpandedPosition == currentPosition) {
                itemExpandedPosition = -1;
            } else {
                int previousExpandedPosition = itemExpandedPosition;
                itemExpandedPosition = currentPosition;
                if (previousExpandedPosition != -1) {
                    notifyItemChanged(previousExpandedPosition);
                }
            }
            notifyItemChanged(currentPosition);
        });

        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("usuarios").child(publicacion.getIdUsuario());
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String nombreUsuario = dataSnapshot.child("nombre").getValue(String.class);
                    String tipoLogin = dataSnapshot.child("tipoLogin").getValue(String.class);
                    String fotoUrl = dataSnapshot.child("imagenUrl").getValue(String.class);

                    holder.tvNombre.setText(nombreUsuario);

                    Glide.with(context)
                            .load(fotoUrl)
                            .placeholder(R.drawable.usuario)
                            .error(R.drawable.usuario)
                            .circleCrop()
                            .into(holder.profileImage);
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

    @Override
    public void onViewRecycled(@NonNull PublicacionViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder.mapView != null && holder.mapView.getVisibility()==View.VISIBLE) {
            try {
                holder.mapView.onPause();
                holder.mapView.onDestroy();
            } catch (Exception e) {
                Log.e("PublicacionAdapter", "Error al limpiar MapView", e);
            }
        }
    }

    public void agregarPublicacion(Publicacion publicacion) {
        listaPublicaciones.add(publicacion);
        notifyItemInserted(listaPublicaciones.size() - 1);
    }

    public static class PublicacionViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitulo, tvDescripcion, tvFechaHora, tvUbicacion, tvPrecio, tvNombre;
        ImageView profileImage;
        Chip tvCategoria;
        Button verMasButton;
        MapView mapView;

        public PublicacionViewHolder(@NonNull View itemView) {
            super(itemView);
            mapView = itemView.findViewById(R.id.mapView);
            tvTitulo = itemView.findViewById(R.id.tvTitulo);
            tvDescripcion = itemView.findViewById(R.id.tvDescripcionLarga);
            tvFechaHora = itemView.findViewById(R.id.tvFecha);
            tvUbicacion = itemView.findViewById(R.id.tvUbicacion);
            tvPrecio = itemView.findViewById(R.id.tvPrecio);
            tvNombre = itemView.findViewById(R.id.tvNombre);
            profileImage = itemView.findViewById(R.id.profileImage);
            tvCategoria = itemView.findViewById(R.id.tvCategoria);
            verMasButton = itemView.findViewById(R.id.btnVerMas);
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
                return android.graphics.Color.parseColor("#E0E0E0");
        }
    }
}

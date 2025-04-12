package com.example.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class UbicacionFragment extends Fragment implements OnMapReadyCallback {

    private static final String TAG = "UbicacionFragment";

    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationClient;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: Iniciando fragmento");

        View root = inflater.inflate(R.layout.fragment_ubicacion, container, false);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        Log.d(TAG, "onCreateView: Cliente de ubicación inicializado");

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);

        if (mapFragment != null) {
            Log.d(TAG, "onCreateView: MapFragment encontrado, llamando a getMapAsync");
            mapFragment.getMapAsync(this);
        } else {
            Log.e(TAG, "onCreateView: MapFragment es null");
        }

        return root;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG, "onMapReady: Mapa listo");
        this.googleMap = googleMap;

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "onMapReady: Permisos de ubicación no otorgados, solicitando...");
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            return;
        }

        googleMap.setMyLocationEnabled(true);
        Log.d(TAG, "onMapReady: Permisos concedidos, ubicación habilitada");

        Log.d(TAG, "onMapReady: Obteniendo última ubicación...");
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(requireActivity(), location -> {
                    if (location != null) {
                        double lat = location.getLatitude();
                        double lon = location.getLongitude();
                        LatLng miUbicacion = new LatLng(lat, lon);
                        Log.d(TAG, "Ubicación obtenida: " + lat + ", " + lon);

                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(miUbicacion, 15));
                        googleMap.addMarker(new MarkerOptions().position(miUbicacion).title("Mi ubicación"));

                        Log.d(TAG, "onMapReady: Cámara centrada y marcador añadido");

                        cargarMarcadoresDeTrabajos();
                    } else {
                        Log.e(TAG, "onMapReady: Ubicación obtenida es null");
                        Toast.makeText(requireContext(), "No se pudo obtener tu ubicación actual", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "onMapReady: Error al obtener la ubicación", e);
                    Toast.makeText(requireContext(), "Error al obtener ubicación", Toast.LENGTH_SHORT).show();
                });
    }

    private void cargarMarcadoresDeTrabajos() {
        if (googleMap == null) {
            Log.e(TAG, "cargarMarcadoresDeTrabajos: El mapa aún no está listo.");
            return;
        }

        Log.d(TAG, "cargarMarcadoresDeTrabajos: Cargando publicaciones desde Firebase...");
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("publicaciones");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "onDataChange: Se recibieron " + snapshot.getChildrenCount() + " publicaciones");

                for (DataSnapshot publicacionSnap : snapshot.getChildren()) {
                    Publicacion publicacion = publicacionSnap.getValue(Publicacion.class);

                    if (publicacion != null) {
                        Log.d(TAG, "Publicación encontrada: " + publicacion.getTitulo());

                        if (publicacion.getLatitud() != null && publicacion.getLongitud() != null) {
                            LatLng position = new LatLng(publicacion.getLatitud(), publicacion.getLongitud());
                            googleMap.addMarker(new MarkerOptions().position(position).title(publicacion.getTitulo()));
                            Log.d(TAG, "Marcador añadido: " + publicacion.getTitulo() + " en " + position);
                        } else {
                            Log.w(TAG, "Publicación sin coordenadas: " + publicacion.getTitulo());
                        }
                    } else {
                        Log.w(TAG, "Publicación null encontrada en el snapshot");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error al cargar publicaciones", error.toException());
                Toast.makeText(requireContext(), "Error al cargar publicaciones", Toast.LENGTH_SHORT).show();
            }
        });
    }
}

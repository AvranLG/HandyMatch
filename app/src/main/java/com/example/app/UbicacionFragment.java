package com.example.app;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class UbicacionFragment extends Fragment implements LocationListener {

    private MapView mapView;
    private MyLocationNewOverlay myLocationOverlay;
    private CompassOverlay compassOverlay;
    private LocationManager locationManager;
    private boolean hasSetInitialPosition = false;

    public UbicacionFragment() {
        // Constructor vacío requerido
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Configurar el User-Agent para OSMDroid (obligatorio)
        Configuration.getInstance().setUserAgentValue(requireActivity().getPackageName());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_ubicacion, container, false);
        mapView = root.findViewById(R.id.map);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Configuración básica del mapa
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(16.0); // Zoom más cercano para mostrar ubicación

        // Añadir overlay de ubicación (requiere permisos de ubicación)
        myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(requireActivity()), mapView);
        myLocationOverlay.setPersonIcon(null);
        myLocationOverlay.enableMyLocation();
        mapView.getOverlays().add(myLocationOverlay);

        // Añadir brújula
        compassOverlay = new CompassOverlay(requireActivity(), new InternalCompassOrientationProvider(requireActivity()), mapView);
        compassOverlay.enableCompass();
        mapView.getOverlays().add(compassOverlay);

        // Permitir rotación con gestos
        RotationGestureOverlay rotationGestureOverlay = new RotationGestureOverlay(mapView);
        rotationGestureOverlay.setEnabled(true);
        mapView.getOverlays().add(rotationGestureOverlay);

        // Intentar obtener la ubicación actual
        startLocationUpdates();
    }

    private void startLocationUpdates() {
        // Verificar permisos de ubicación
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            try {
                // Obtener el servicio de ubicación
                locationManager = (LocationManager) requireActivity().getSystemService(Context.LOCATION_SERVICE);

                // Intentar obtener la última ubicación conocida primero
                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (lastKnownLocation != null) {
                    onLocationChanged(lastKnownLocation);
                }

                // Solicitar actualizaciones de ubicación
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        1000,   // Milisegundos entre actualizaciones
                        10,     // Metros de cambio mínimo para actualizar
                        this);

                // También escuchar el proveedor de red como respaldo
                locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        1000,
                        10,
                        this);

            } catch (Exception e) {
                Toast.makeText(requireContext(),
                        "No se pudo acceder a la ubicación: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(requireContext(),
                    "Se requieren permisos de ubicación para mostrar tu posición actual",
                    Toast.LENGTH_LONG).show();
        }
    }

    // Metodo para añadir un marcador al mapa
    public void addMarker(double latitude, double longitude, String title) {
        Marker marker = new Marker(mapView);
        marker.setPosition(new GeoPoint(latitude, longitude));
        marker.setTitle(title);
        marker.setIcon(getResources().getDrawable(R.drawable.ic_marker));
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mapView.getOverlays().add(marker);
        mapView.postInvalidate(); // Redibujar el mapa
    }

    private void limpiarMarcadores() {
        // Borra solo los marcadores, deja otros overlays como la brújula y ubicación
        List<Overlay> overlaysParaEliminar = new ArrayList<>();
        for (Overlay overlay : mapView.getOverlays()) {
            if (overlay instanceof Marker) {
                overlaysParaEliminar.add(overlay);
            }
        }
        mapView.getOverlays().removeAll(overlaysParaEliminar);
    }

    private void cargarMarcadoresDeTrabajos() {
        limpiarMarcadores();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("publicaciones");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot publicacionSnap : snapshot.getChildren()) {
                    Publicacion publicacion = publicacionSnap.getValue(Publicacion.class);

                    if (publicacion != null && publicacion.getLatitud() != null && publicacion.getLongitud() != null) {
                        try {
                            double lat = publicacion.getLatitud();
                            double lon = publicacion.getLongitud();
                            String titulo = publicacion.getTitulo();

                            addMarker(lat, lon, titulo);
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(), "Error al cargar publicaciones", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        // Cuando se obtiene la ubicación, centrar el mapa allí
        GeoPoint myPosition = new GeoPoint(location.getLatitude(), location.getLongitude());

        // Solo centrar automáticamente la primera vez que obtenemos la ubicación
        if (!hasSetInitialPosition) {
            mapView.getController().animateTo(myPosition);
            mapView.getController().setZoom(16.0);
            hasSetInitialPosition = true;

            // Opcional: añadir un marcador en tu posición actual
            addMarker(location.getLatitude(), location.getLongitude(), "Mi ubicación");

            // Llamar a cargar los marcadores después de que se haya fijado la ubicación
            cargarMarcadoresDeTrabajos();
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // Metodo requerido por LocationListener pero deprecado en API más recientes
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
        // Se activa cuando el usuario habilita el GPS
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        // Se activa cuando el usuario deshabilita el GPS
        if (provider.equals(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(requireContext(),
                    "Por favor activa el GPS para ver tu ubicación",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void inicializarOverlays() {
        mapView.getOverlays().clear();

        // Brújula
        compassOverlay = new CompassOverlay(requireContext(), new InternalCompassOrientationProvider(requireContext()), mapView);
        compassOverlay.enableCompass();
        mapView.getOverlays().add(compassOverlay);

        // Ubicación
        myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(requireActivity()), mapView);
        myLocationOverlay.setPersonIcon(null);
        myLocationOverlay.enableMyLocation();
        myLocationOverlay.enableFollowLocation();
        mapView.getOverlays().add(myLocationOverlay);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reiniciar las actualizaciones de ubicación cuando se reanuda el fragmento
        mapView.onResume();
        startLocationUpdates();
        inicializarOverlays(); // <- Esto se vuelve a aplicar siempre
        cargarMarcadoresDeTrabajos();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            cargarMarcadoresDeTrabajos();
        }, 2000); // dos segundo después de onResume
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }

        if (myLocationOverlay != null) {
            myLocationOverlay.disableMyLocation();
        }
        if (compassOverlay != null) {
            compassOverlay.disableCompass();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (myLocationOverlay != null) {
            myLocationOverlay.disableMyLocation();
        }
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
    }
}

package com.example.app;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
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
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

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
        // Crear una vista simple para el mapa
        mapView = new MapView(requireActivity());
        mapView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        return mapView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Configuración básica del mapa
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(16.0); // Zoom más cercano para mostrar ubicación

        // Punto temporal mientras se obtiene la ubicación real
        GeoPoint startPoint = new GeoPoint(20.65953820, -103.349437603); // Jalisco como respaldo
        mapView.getController().setCenter(startPoint);

        // Añadir overlay de ubicación (requiere permisos de ubicación)
        myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(requireActivity()), mapView);
        myLocationOverlay.enableMyLocation();
        myLocationOverlay.enableFollowLocation(); // Seguir la ubicación del usuario
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
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mapView.getOverlays().add(marker);
        mapView.invalidate(); // Redibujar el mapa
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

    @Override
    public void onResume() {
        super.onResume();
        // Reiniciar las actualizaciones de ubicación cuando se reanuda el fragmento
        mapView.onResume();
        startLocationUpdates();
    }

    @Override
    public void onPause() {
        super.onPause();
        // Detener las actualizaciones de ubicación cuando se pausa el fragmento
        mapView.onPause();
        if (locationManager != null) {
            locationManager.removeUpdates(this);
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
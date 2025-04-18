package com.example.app;


import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;

import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Toast;
import android.widget.TextView;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.pm.PackageManager;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.Address;
import android.location.Geocoder;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import com.google.android.material.textfield.TextInputEditText;

import java.util.Calendar;

public class Publicar extends AppCompatActivity implements OnMapReadyCallback {

    private String direccionSeleccionada = "";

    private TextInputEditText fechaHoraEditText;
    private TextInputEditText ubicacionEditText;
    private GoogleMap mMap;
    private Marker marcadorSeleccionado;
    private double latitudSeleccionada;
    private double longitudSeleccionada;
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private static final String GOOGLE_MAPS_API_KEY = BuildConfig.MAPS_API_KEY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("DEBUG_TAG", "onCreate iniciado");
        setContentView(R.layout.activity_publicar);

        // Inicializar Places API
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), GOOGLE_MAPS_API_KEY);
        }

        fechaHoraEditText = findViewById(R.id.fechaHora);

        // Código para la lista seleccionable
        AutoCompleteTextView spinnerCategoria = findViewById(R.id.spinnerCategoria);
        String[] categorias = {"Hogar", "Mascotas", "Tecnología", "Eventos", "Transporte", "Salud y Bienestar", "Educación", "Negocios", "Reparaciones", "Otros"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, categorias);
        spinnerCategoria.setAdapter(adapter);

        spinnerCategoria.setOnTouchListener((v, event) -> {
            spinnerCategoria.showDropDown();
            return false;
        });



        // Lógica de fecha y hora
        fechaHoraEditText.setOnClickListener(v -> {
            final Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);

            android.app.DatePickerDialog datePickerDialog = new android.app.DatePickerDialog(Publicar.this,
                    (view, yearSelected, monthSelected, dayOfMonth) -> {
                        android.app.TimePickerDialog timePickerDialog = new android.app.TimePickerDialog(Publicar.this,
                                (timeView, hourOfDay, minuteOfHour) -> {
                                    String fechaHora = String.format("%02d/%02d/%04d %02d:%02d",
                                            dayOfMonth, monthSelected + 1, yearSelected, hourOfDay, minuteOfHour);
                                    fechaHoraEditText.setText(fechaHora);
                                }, hour, minute, true);
                        timePickerDialog.show();
                    }, year, month, day);

            datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
            datePickerDialog.show();
        });


        // Configuración de Google Maps
        TouchableMapFragment mapFragment = (TouchableMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);

            // Configura el listener de toques para el mapa
            mapFragment.setTouchListener(new TouchableMapFragment.OnTouchListener() {
                @Override
                public void onTouch() {
                    // Cuando el usuario toca el mapa, deshabilita el scroll de la vista padre
                    ScrollView scrollView = findViewById(R.id.scrollView); // Asegúrate de tener un ID para tu ScrollView
                    scrollView.requestDisallowInterceptTouchEvent(true);
                }

                @Override
                public void onRelease() {
                    // Cuando el usuario deja de tocar el mapa, habilita el scroll de la vista padre
                    ScrollView scrollView = findViewById(R.id.scrollView); // Asegúrate de tener un ID para tu ScrollView
                    scrollView.requestDisallowInterceptTouchEvent(false);
                }
            });
        }
        setupPlacesAutocomplete();
    }


    private void setupPlacesAutocomplete() {
        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        if (autocompleteFragment != null) {
            // Cambiar tamaño del texto del input
            View fragmentView = autocompleteFragment.getView();
            if (fragmentView != null) {
                EditText editText = fragmentView.findViewById(
                        fragmentView.getResources().getIdentifier(
                                "places_autocomplete_search_input", "id", getPackageName()
                        )
                );

                if (editText != null) {
                    editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14); // Cambia el 14 si quieres más chico o grande
                    editText.setHint("Ingresa una ubicación"); // opcional, ya lo tenías
                    editText.setTextColor(Color.BLACK); // opcional
                    editText.setHintTextColor(Color.GRAY); // opcional
                }
            }

            // Especificar los campos que quieres obtener
            autocompleteFragment.setPlaceFields(Arrays.asList(
                    Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS,
                    Place.Field.ADDRESS_COMPONENTS));


            autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
                @Override
                public void onPlaceSelected(@NonNull Place place) {
                    LatLng latLng = place.getLatLng();
                    if (latLng != null) {
                        // Guardar coordenadas
                        latitudSeleccionada = latLng.latitude;
                        longitudSeleccionada = latLng.longitude;

                        // Actualizar el marcador en el mapa
                        if (marcadorSeleccionado != null) {
                            marcadorSeleccionado.remove();
                        }
                        marcadorSeleccionado = mMap.addMarker(new MarkerOptions()
                                .position(latLng)
                                .title(place.getAddress())); // ← Aquí también usamos la dirección completa
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17));

                        // Establecer dirección completa en el campo de texto del AutocompleteSupportFragment
                        View fragmentView = autocompleteFragment.getView();
                        if (fragmentView != null) {
                            EditText actualInput = fragmentView.findViewById(
                                    com.google.android.libraries.places.R.id.places_autocomplete_search_input
                            );

                            if (actualInput != null) {
                                actualInput.setText(place.getAddress()); // ← Dirección completa
                            } else {
                                Log.e("PLACES", "No se encontró el AutoCompleteTextView interno");
                            }
                        } else {
                            Log.e("PLACES", "No se pudo obtener la vista del fragmento");
                        }

                        // Debug log
                        Log.d("PLACES", "Dirección completa insertada: " + place.getAddress());
                    }
                    direccionSeleccionada = place.getAddress();

                }

                @Override
                public void onError(@NonNull Status status) {
                    Log.e("PLACES", "Error: " + status);
                    Toast.makeText(Publicar.this,
                            "Error al seleccionar lugar: " + status.getStatusMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            });

        }
    }


    // Metodo para obtener la dirección completa a partir de coordenadas
    private String getCompleteAddressString(double latitude, double longitude) {
        String result = "";
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && addresses.size() > 0) {
                Address address = addresses.get(0);
                StringBuilder sb = new StringBuilder();

                // Construir la dirección completa
                for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                    sb.append(address.getAddressLine(i));
                    if (i < address.getMaxAddressLineIndex()) {
                        sb.append(", ");
                    }
                }
                result = sb.toString();
            }
        } catch (IOException e) {
            Log.e("GEOCODER", "Error al obtener dirección completa: " + e.getMessage());
        }

        return result;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Solicitar permisos de ubicación si es necesario
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSIONS_REQUEST_CODE);
        } else {
            mMap.setMyLocationEnabled(true); // Habilitar ubicación
            centerMapOnUserLocationOnce(); // Centrar el mapa en la ubicación del usuario una sola vez
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSIONS_REQUEST_CODE);
        } else {
            mMap.setMyLocationEnabled(true);
            centerMapOnUserLocationOnce();
        }

        // Modificar el listener de click en el mapa para incluir geocodificación inversa
        mMap.setOnMapClickListener(latLng -> {
            // Guardar las coordenadas seleccionadas para Firebase
            latitudSeleccionada = latLng.latitude;
            longitudSeleccionada = latLng.longitude;

            // Actualizar el marcador
            if (marcadorSeleccionado != null) {
                marcadorSeleccionado.remove();
            }
            marcadorSeleccionado = mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title("Ubicación seleccionada"));

            // Obtener la dirección
            getAddressFromLocation(latLng.latitude, latLng.longitude, address -> {
                // Buscar el AutocompleteSupportFragment
                AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                        getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);
                if (autocompleteFragment != null) {
                    // Actualizar el texto del fragment
                    autocompleteFragment.setText(address);
                    direccionSeleccionada = address;
                }
            });
        });
    }

    private void getAddressFromLocation(double latitude, double longitude, OnAddressFoundListener listener) {
        Thread thread = new Thread() {
            @Override
            public void run() {
                Geocoder geocoder = new Geocoder(Publicar.this, Locale.getDefault());
                String result = "";

                try {
                    List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
                    if (addresses != null && addresses.size() > 0) {
                        Address address = addresses.get(0);
                        StringBuilder sb = new StringBuilder();

                        for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                            sb.append(address.getAddressLine(i));
                            if (i < address.getMaxAddressLineIndex()) {
                                sb.append(", ");
                            }
                        }

                        result = sb.toString();
                    } else {
                        result = "No se encontró dirección para esta ubicación";
                    }
                } catch (IOException e) {
                    result = "Error al obtener la dirección: " + e.getMessage();
                    Log.e("GEOCODER", e.getMessage());
                }

                final String finalResult = result;
                runOnUiThread(() -> listener.onAddressFound(finalResult));
            }
        };

        thread.start();
    }

    // Interfaz para el callback de la dirección
    interface OnAddressFoundListener {
        void onAddressFound(String address);
    }


    // Metodo para centrar el mapa en la ubicación del usuario una sola vez
    private void centerMapOnUserLocationOnce() {
        try {
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

            if (locationManager != null) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    // Obtener la última ubicación conocida
                    Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                    if (lastKnownLocation != null) {
                        // Centrar el mapa en la última ubicación conocida
                        LatLng userLocation = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 17));
                    } else {
                        // Si no hay ubicación conocida, solicitar actualizaciones una vez
                        locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, new LocationListener() {
                            @Override
                            public void onLocationChanged(Location location) {
                                LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 17));
                            }

                            @Override
                            public void onStatusChanged(String provider, int status, Bundle extras) {}

                            @Override
                            public void onProviderEnabled(String provider) {}

                            @Override
                            public void onProviderDisabled(String provider) {}
                        }, null);
                    }
                }
            }
        } catch (Exception e) {
            Log.e("MAP_ERROR", "Error al centrar el mapa: " + e.getMessage());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(true);
                    centerMapOnUserLocationOnce(); // Centrar después de obtener permisos
                }
            } else {
                Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }


    public void publicarTrabajo(View v) {
        // Obtener los datos de los campos
        String titulo = ((TextInputEditText) findViewById(R.id.tituloText)).getText().toString().trim();
        String categoria = ((AutoCompleteTextView) findViewById(R.id.spinnerCategoria)).getText().toString().trim();
        String descripcion = ((TextInputEditText) findViewById(R.id.descripcionText)).getText().toString().trim();
        String fechaHora = ((TextInputEditText) findViewById(R.id.fechaHora)).getText().toString().trim();
        String pago = ((TextInputEditText) findViewById(R.id.pagoText)).getText().toString().trim();
        String ubicacion = direccionSeleccionada;
        String estadoPublicacion = "Publicada";

        // Verificar que todos los campos estén llenos
        if (titulo.isEmpty() || categoria.isEmpty() || descripcion.isEmpty() || fechaHora.isEmpty() || pago.isEmpty() || ubicacion.isEmpty()) {
            Toast.makeText(this, "Por favor, complete todos los campos.", Toast.LENGTH_SHORT).show();
            return;  // Detener la ejecución si algún campo está vacío
        }

        // Si tienes latitud y longitud
        double latitud = this.latitudSeleccionada;
        double longitud = this.longitudSeleccionada;

        // Verificar que el usuario esté autenticado
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Debe iniciar sesión", Toast.LENGTH_SHORT).show();
            return;
        }

        // Obtener el ID del usuario autenticado
        String idUsuario = user.getUid();

        // Crear el objeto de publicación
        Publicacion publicacion = new Publicacion(titulo, categoria, descripcion, fechaHora, pago, ubicacion, latitud, longitud, idUsuario, estadoPublicacion);

        // Guardar en la base de datos de Firebase Realtime Database
        FirebaseDatabase.getInstance().getReference("publicaciones")
                .push() // Crear un nuevo nodo con una clave única
                .setValue(publicacion)
                .addOnSuccessListener(aVoid -> {
                    // Acción después de guardar con éxito
                    Toast.makeText(this, "Trabajo publicado", Toast.LENGTH_SHORT).show();

                    // Obtener la instancia de HomeFragment y llamar agregarPublicacion()
                    HomeFragment homeFragment = (HomeFragment) getSupportFragmentManager().findFragmentByTag("HOME_FRAGMENT_TAG");
                    if (homeFragment != null) {
                        homeFragment.agregarPublicacion(publicacion); // Agregar la nueva publicación
                    }
                })
                .addOnFailureListener(e -> {
                    // Acción en caso de error
                    Toast.makeText(this, "Error al publicar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });

        // Redirigir a HomeActivity
        Intent intent = new Intent(Publicar.this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish(); // Para cerrar esta actividad y evitar volver con el botón "atrás"
    }
}

package com.example.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.config.Configuration;

import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.android.material.textfield.TextInputEditText;

import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class Publicar extends AppCompatActivity {

    private TextInputEditText fechaHoraEditText;
    private TextInputEditText ubicacionEditText;
    private static final int AUTOCOMPLETE_REQUEST_CODE = 1001;
    private final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private MapView map;
    private GestureDetector gestureDetector;
    private Marker marcadorSeleccionado;
    private double latitudSeleccionada;
    private double longitudSeleccionada;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("DEBUG_TAG", "onCreate iniciado");
        setContentView(R.layout.activity_publicar);

        Configuration.getInstance().load(getApplicationContext(), PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        Log.d("DEBUG_TAG", "onCreate: layout de la actividad cargado");

        fechaHoraEditText = findViewById(R.id.fechaHora);
        ubicacionEditText = findViewById(R.id.ubicacion);
        Log.d("DEBUG_TAG", "onCreate: componentes de UI encontrados");

        // Lógica de fecha y hora
        fechaHoraEditText.setOnClickListener(v -> {
            Log.d("DEBUG_TAG", "fechaHoraEditText clickado");
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

        // Código para la lista seleccionable
        AutoCompleteTextView spinnerCategoria = findViewById(R.id.spinnerCategoria);
        String[] categorias = {"Hogar", "Mascotas", "Tecnología", "Eventos", "Transporte", "Salud y Bienestar", "Educación", "Negocios", "Reparaciones", "Otros"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, categorias);
        spinnerCategoria.setAdapter(adapter);

        spinnerCategoria.setOnTouchListener((v, event) -> {
            Log.d("DEBUG_TAG", "spinnerCategoria clickado");
            spinnerCategoria.showDropDown();
            return false;
        });

        Configuration.getInstance().setUserAgentValue(getPackageName());
        map = findViewById(R.id.mapViewPublicar);
        map.setMultiTouchControls(true);

        // Overlay para ubicación
        MyLocationNewOverlay locationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), map);
        locationOverlay.setPersonIcon(null);
        locationOverlay.enableMyLocation();
        map.getOverlays().add(locationOverlay);

        locationOverlay.runOnFirstFix(new Runnable() {
            @Override
            public void run() {
                GeoPoint myLocation = locationOverlay.getMyLocation();
                if (myLocation != null) {
                    Log.d("DEBUG_TAG", "Ubicación obtenida correctamente: " + myLocation.getLatitude() + ", " + myLocation.getLongitude());
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (map != null && map.getController() != null && myLocation != null) {
                            map.getController().animateTo(myLocation);
                            map.getController().setZoom(17.0);
                        } else {
                            Log.e("DEBUG_TAG", "No se pudo mover el mapa, algo es null.");
                        }
                    });

                } else {
                    Log.e("DEBUG_TAG", "No se obtuvo la ubicación");
                }
            }
        });


// Para evitar conflictos con el ScrollView
        map.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                v.getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            }
        });

        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                GeoPoint geoPoint = (GeoPoint) map.getProjection().fromPixels((int) e.getX(), (int) e.getY());

                // Guardar las coordenadas
                latitudSeleccionada = geoPoint.getLatitude();
                longitudSeleccionada = geoPoint.getLongitude();

                // Ocultar el teclado
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                View currentFocus = getCurrentFocus();
                if (imm != null && currentFocus != null) {
                    imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
                }

                // Quitar el foco de todos los campos
                fechaHoraEditText.clearFocus();
                ubicacionEditText.clearFocus();
                findViewById(R.id.spinnerCategoria).clearFocus();
                findViewById(R.id.tituloText).clearFocus();
                findViewById(R.id.descripcionText).clearFocus();
                findViewById(R.id.pagoText).clearFocus();

                // Mostrar en el campo de ubicación
                ubicacionEditText.setText(
                        String.format("%.6f, %.6f", latitudSeleccionada, longitudSeleccionada)
                );

                Log.d("DEBUG_TAG", "Tocó en: " + latitudSeleccionada + ", " + longitudSeleccionada);

                // Eliminar marcador anterior si existe
                if (marcadorSeleccionado != null) {
                    map.getOverlays().remove(marcadorSeleccionado);
                }

                // Crear nuevo marcador
                marcadorSeleccionado = new Marker(map);
                marcadorSeleccionado.setPosition(geoPoint);
                marcadorSeleccionado.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                marcadorSeleccionado.setTitle("Ubicación seleccionada");

                map.getOverlays().add(marcadorSeleccionado);
                map.invalidate();

                return true;
            }
        });

// Asignar el detector al mapa sin interferir con zoom
        map.setOnTouchListener((v, event) -> {
            v.getParent().requestDisallowInterceptTouchEvent(true);
            gestureDetector.onTouchEvent(event);
            return false; // importante: dejar que otros gestos como zoom funcionen
        });



        Log.d("DEBUG_TAG", "onCreate finalizado");
    }

    public void publicarTrabajo(View v) {
        // Obtener los datos de los campos
        String titulo = ((TextInputEditText) findViewById(R.id.tituloText)).getText().toString().trim();
        String categoria = ((AutoCompleteTextView) findViewById(R.id.spinnerCategoria)).getText().toString().trim();
        String descripcion = ((TextInputEditText) findViewById(R.id.descripcionText)).getText().toString().trim();
        String fechaHora = ((TextInputEditText) findViewById(R.id.fechaHora)).getText().toString().trim();
        String pago = ((TextInputEditText) findViewById(R.id.pagoText)).getText().toString().trim();
        String ubicacion = ((TextInputEditText) findViewById(R.id.ubicacion)).getText().toString().trim();

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
        Publicacion publicacion = new Publicacion(titulo, categoria, descripcion, fechaHora, pago, ubicacion, latitud, longitud, idUsuario);

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

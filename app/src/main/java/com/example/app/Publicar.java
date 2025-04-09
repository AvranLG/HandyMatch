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

    // Recibir el resultado de la ubicación
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("DEBUG_TAG", "onActivityResult iniciado");

        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Place place = Autocomplete.getPlaceFromIntent(data);
                ubicacionEditText.setText(place.getAddress()); // o place.getName()
            } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                Log.e("DEBUG_TAG", "Error al seleccionar ubicación");
                Toast.makeText(this, "Error al seleccionar ubicación", Toast.LENGTH_SHORT).show();
            }
        }

        Log.d("DEBUG_TAG", "onActivityResult finalizado");
    }
}

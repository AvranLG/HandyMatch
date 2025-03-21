package com.example.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import android.view.inputmethod.InputMethodManager;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class DireccionActivity extends AppCompatActivity {

    // Declarar los EditText para la dirección
    private EditText direccionText;
    private EditText codigoPostalText;
    private EditText coloniaText;
    private EditText estadoText;
    private EditText ciudadText;
    private EditText referenciaText;

    // Variables para recibir datos de la actividad anterior
    private String nombre;
    private String apellidos;
    private String correo;
    private String contrasena;
    private String telefono;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_direccion);

        // Recibir los datos de la actividad anterior (RegistroActivity)
        Intent intent = getIntent();
        nombre = intent.getStringExtra("nombre");
        apellidos = intent.getStringExtra("apellidos");
        correo = intent.getStringExtra("correo");
        contrasena = intent.getStringExtra("contrasena");
        telefono = intent.getStringExtra("telefono");

        // Inicializar los EditText
        direccionText = findViewById(R.id.direccionText);
        codigoPostalText = findViewById(R.id.postalText);
        coloniaText = findViewById(R.id.coloniaText);
        estadoText = findViewById(R.id.estadoText);
        ciudadText = findViewById(R.id.ciudadText);
        referenciaText = findViewById(R.id.referenciaText);

        referenciaText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                referenciaText.scrollTo(0, 0); // Resetea el scroll al inicio
            }
        });

        referenciaText.setOnTouchListener((v, event) -> {
            if (v.getId() == R.id.referenciaText) {
                v.getParent().requestDisallowInterceptTouchEvent(true);
                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_UP:
                        v.getParent().requestDisallowInterceptTouchEvent(false);
                        break;
                }
            }
            return false;
        });
    }

    // Método para enviar los datos a Firebase
    public void enviarDatosFirebase(View v) {
        // Recoger los datos de los campos de dirección
        String direccion = direccionText.getText().toString();
        String codigoPostal = codigoPostalText.getText().toString();
        String colonia = coloniaText.getText().toString();
        String estado = estadoText.getText().toString();
        String ciudad = ciudadText.getText().toString();
        String referencia = referenciaText.getText().toString();

        // Validar si algún campo está vacío
        if (direccion.isEmpty() || codigoPostal.isEmpty() || colonia.isEmpty() || estado.isEmpty() || ciudad.isEmpty() || referencia.isEmpty()) {
            // Mostrar un Toast con el mensaje de error
            Toast.makeText(this, "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show();
        } else {
            String fechaRegistro = obtenerFechaHoraActual();
            // Crear un objeto de usuario con los datos
            Usuario usuario = new Usuario(nombre, correo, telefono, contrasena, fechaRegistro, direccion, codigoPostal, colonia, estado, ciudad, referencia);

            // Referencia a la base de datos Firebase
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference myRef = database.getReference("usuarios");

            // Subir los datos del usuario a Firebase con su clave única de push
            myRef.push().setValue(usuario);

            // Mostrar un mensaje de éxito
            Toast.makeText(this, "Datos registrados exitosamente", Toast.LENGTH_SHORT).show();

            // Opcional: Redirigir a otra actividad, por ejemplo LoginActivity
            Intent i = new Intent(this, LoginActivity.class);
            startActivity(i);
        }
    }

    private String obtenerFechaHoraActual() {
        // Obtener la fecha y hora actual usando Calendar
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // Formatear la fecha y hora y devolverla como un String
        return dateFormat.format(calendar.getTime());
    }

    // Sobrescribir el método onBackPressed para manejar el retroceso
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // Obtener el InputMethodManager
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

            // Verificar si el teclado está visible
            View view = this.getCurrentFocus();
            if (view != null && view instanceof EditText) {
                // Verificamos si el teclado está visible en este momento
                if (inputMethodManager.isAcceptingText()) {
                    // Si el teclado está visible, lo cerramos
                    inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);

                    // Además, quitamos el foco de todos los EditText
                    clearFocusFromAllEditTexts();

                    return true;  // Interceptamos el retroceso para no salir de la actividad
                }
            }

            // Si el teclado no está visible, dejamos que se ejecute el comportamiento estándar del retroceso
            return super.onKeyDown(keyCode, event);
        }
        return super.onKeyDown(keyCode, event);
    }

    // Método para quitar el foco de todos los EditTexts
    private void clearFocusFromAllEditTexts() {
        if (direccionText.hasFocus()) {
            direccionText.clearFocus();
        }
        if (codigoPostalText.hasFocus()) {
            codigoPostalText.clearFocus();
        }
        if (coloniaText.hasFocus()) {
            coloniaText.clearFocus();
        }
        if (estadoText.hasFocus()) {
            estadoText.clearFocus();
        }
        if (ciudadText.hasFocus()) {
            ciudadText.clearFocus();
        }
        if (referenciaText.hasFocus()) {
            referenciaText.clearFocus();
        }
    }


}

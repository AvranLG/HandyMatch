package com.example.app;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.EditText;
import android.widget.ImageView;

import com.google.android.material.textfield.TextInputLayout;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

public class RegistroActivity extends AppCompatActivity {

    private static final int PICK_IMAGE = 1;
    private ImageView profileImage;
    private ImageView logoImage;
    private Uri imageUri; // Guardar la URI de la imagen seleccionada

    private EditText nombreText, apellidosText, correoText, contrasenaText, telefonoText;
    private EditText fechaNacimientoText;
    private static final int PICK_IDENTITY_FILE = 2;
    private Uri identidadUri;

    // Constantes para log
    private static final String TAG = "RegistroActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro);

        // Cambiar el color de la barra de estado
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(Color.parseColor("#FFFFFF"));

        // Inicializar los EditText
        nombreText = findViewById(R.id.direccionText);
        apellidosText = findViewById(R.id.apellidosText);
        correoText = findViewById(R.id.tituloText);
        contrasenaText = findViewById(R.id.passwordText);
        telefonoText = findViewById(R.id.numeroText);

        fechaNacimientoText = findViewById(R.id.fechaNacimientoText);
        fechaNacimientoText.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePicker = new DatePickerDialog(RegistroActivity.this,
                    (view, year1, month1, dayOfMonth) -> {
                        String fecha = String.format("%02d/%02d/%04d", dayOfMonth, month1 + 1, year1);
                        fechaNacimientoText.setText(fecha);
                    }, year, month, day);
            datePicker.show();
        });

        // Referencias de ImageView
        profileImage = findViewById(R.id.profileImage);
        logoImage = findViewById(R.id.logoImagen);

        // Restaurar imagen si la actividad se reinicia
        if (savedInstanceState != null) {
            imageUri = savedInstanceState.getParcelable("imageUri");
            if (imageUri != null) {
                setImageToCircle(imageUri);
            }
        }

        profileImage.setOnClickListener(view -> openGallery());
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            startUCrop(imageUri);
        }

        if (requestCode == UCrop.REQUEST_CROP && resultCode == RESULT_OK) {
            Uri resultUri = UCrop.getOutput(data);
            setImageToCircle(resultUri);
            imageUri = resultUri;
        }
    }

    private void startUCrop(Uri imageUri) {
        Uri destinationUri = Uri.fromFile(new File(getCacheDir(), "cropped_image.jpg"));

        UCrop.of(imageUri, destinationUri)
                .withAspectRatio(1, 1)
                .withMaxResultSize(800, 800)
                .withOptions(getUCropOptions())
                .start(this);
    }

    private UCrop.Options getUCropOptions() {
        UCrop.Options options = new UCrop.Options();
        options.setCircleDimmedLayer(true);
        options.setShowCropFrame(false);
        options.setShowCropGrid(true);
        return options;
    }

    private void setImageToCircle(Uri imageUri) {
        try {
            InputStream imageStream = getContentResolver().openInputStream(imageUri);
            Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
            Bitmap circularImage = getCircularBitmap(selectedImage);

            profileImage.setImageBitmap(circularImage);
            logoImage.setVisibility(View.GONE);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al cargar la imagen", Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap getCircularBitmap(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int radius = Math.min(width, height) / 2;

        Bitmap output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);

        canvas.drawCircle(width / 2, height / 2, radius, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, 0, 0, paint);

        return output;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (imageUri != null) {
            outState.putParcelable("imageUri", imageUri);
        }
    }

    // Método para verificar si la persona es mayor de 18 años
    private boolean isAdult(String fechaNacimiento) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy"); // Cambiar el formato
            Date birthDate = sdf.parse(fechaNacimiento); // Parseamos la fecha de nacimiento
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.YEAR, -18); // Restamos 18 años a la fecha actual
            Date adultDate = calendar.getTime();

            // Comparamos si la fecha de nacimiento es menor o igual a la fecha de 18 años atrás
            return !birthDate.after(adultDate);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void abrirDireccion(View v) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Procesando...");
        progressDialog.show();

        boolean error = false;
        // Recoger los datos de los EditText
        String nombre = nombreText.getText().toString().trim();
        String apellidos = apellidosText.getText().toString().trim();
        String correo = correoText.getText().toString().trim();
        String contrasena = contrasenaText.getText().toString().trim();
        String telefono = telefonoText.getText().toString().trim();

        TextInputLayout nombreContainer = findViewById(R.id.nombreContainer);
        TextInputLayout apellidosContainer = findViewById(R.id.apellidosContainer);
        TextInputLayout emailContainer = findViewById(R.id.emailContainer);
        TextInputLayout pwdContainer = findViewById(R.id.pwdContainer);
        TextInputLayout numeroContainer = findViewById(R.id.numeroContainer);

        String fechaNacimiento = fechaNacimientoText.getText().toString().trim();

        if (fechaNacimiento.isEmpty()) {
            Toast.makeText(this, "La fecha de nacimiento es obligatoria", Toast.LENGTH_SHORT).show();
            progressDialog.dismiss();
            return;
        }

        // Validar los datos antes de enviarlos
        if (nombre.isEmpty() || apellidos.isEmpty() || correo.isEmpty() || contrasena.isEmpty() || telefono.isEmpty()) {
            progressDialog.dismiss();
            Toast.makeText(this, "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validar el nombre (Debe comenzar con letra y permitir espacios)
        if (!nombre.matches("^[A-Za-zÁÉÍÓÚáéíóúÑñ][A-Za-zÁÉÍÓÚáéíóúÑñ ]*$")) {
            nombreContainer.setError("Debe iniciar y contener sólo letras");
            nombreContainer.setErrorEnabled(true); // Habilita el mensaje de error
            error = true;
        } else {
            // Si el nombre es válido, se elimina el error
            nombreContainer.setErrorEnabled(false);
        }

        // Validar los apellidos (Debe comenzar con letra y permitir espacios)
        if (!apellidos.matches("^[A-Za-zÁÉÍÓÚáéíóúÑñ][A-Za-zÁÉÍÓÚáéíóúÑñ ]*$")) {
            apellidosContainer.setError("Debe iniciar y contener sólo letras");
            apellidosContainer.setErrorEnabled(true); // Habilita el mensaje de error
            error = true;
        } else {
            // Si el apellido es válido, se elimina el error
            apellidosContainer.setErrorEnabled(false);
        }

        if (!isAdult(fechaNacimiento)) {
            Toast.makeText(this, "Debes ser mayor de edad para registrarte", Toast.LENGTH_SHORT).show();
            error = true;
        }

        // Validar el correo electrónico
        if (!correo.matches("^([a-zA-Z][a-zA-Z0-9]*(?:[._-][a-zA-Z0-9]+)*)@(gmail\\.com|hotmail\\.com|yahoo\\.com|outlook\\.com|itocotlan\\.com|ocotlan\\.tecnm\\.mx)$"
        )) {
            emailContainer.setError("Formato inválido");
            emailContainer.setErrorEnabled(true); // Habilita el mensaje de error
            error = true;
        } else {
            // Si el email es válido, se elimina el error
            emailContainer.setErrorEnabled(false);
        }

        // Validar la contraseña (mínimo 8 caracteres, al menos una mayúscula, una minúscula y un número)
        if (contrasena.length() < 8 || !contrasena.matches(".*[A-Z].*") || !contrasena.matches(".*[a-z].*") || !contrasena.matches(".*\\d.*")) {
            pwdContainer.setError("Mínimo 8 caracteres, 1 mayúscula, 1 minúscula y 1 número");
            pwdContainer.setErrorEnabled(true); // Habilita el mensaje de error
            error = true;
        } else {
            // Si la contraseña es válida, se elimina el error
            pwdContainer.setErrorEnabled(false);
        }

        // Validar el teléfono (verificar que tenga exactamente 10 dígitos)
        if (telefono.length() != 10 || !telefono.matches("\\d{10}")) {
            numeroContainer.setError("Debe contener 10 dígitos");
            numeroContainer.setErrorEnabled(true); // Habilita el mensaje de error
            error = true;
        } else {
            // Si el numero es válido, se elimina el error
            numeroContainer.setErrorEnabled(false);
        }

        if (error) {
            progressDialog.dismiss();
            return;
        }

        // Verificar si el correo o teléfono ya existen
        verificarDatosExistentes(correo, telefono, progressDialog);
    }

    private void verificarDatosExistentes(String correo, String telefono, ProgressDialog progressDialog) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference usuariosRef = database.getReference("usuarios");

        // Flags para rastrear si hemos encontrado duplicados
        AtomicBoolean correoExiste = new AtomicBoolean(false);
        AtomicBoolean telefonoExiste = new AtomicBoolean(false);
        AtomicBoolean verificacionCompleta = new AtomicBoolean(false);

        // Verificar si el correo ya existe
        usuariosRef.orderByChild("email").equalTo(correo)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        // Log para verificar existencia en Realtime Database
                        Log.d(TAG, "Verificación de correo - Snapshot existe: " + snapshot.exists());

                        if (snapshot.exists()) {
                            // Correo encontrado en la base de datos
                            correoExiste.set(true);
                            correoText.setError("El correo electrónico ya está en uso");
                            Log.w(TAG, "Correo encontrado en Realtime Database");
                        }

                        // Verificamos si ambas comprobaciones han terminado para continuar
                        if (verificacionCompleta.get()) {
                            finalizarVerificacion(correoExiste.get(), telefonoExiste.get(), progressDialog);
                        } else {
                            verificacionCompleta.set(true);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        progressDialog.dismiss();
                        Log.e(TAG, "Error al verificar correo en Realtime Database", error.toException());
                        Toast.makeText(RegistroActivity.this,
                                "Error al verificar el correo",
                                Toast.LENGTH_SHORT).show();
                    }
                });

        // Verificar si el teléfono ya existe
        usuariosRef.orderByChild("telefono").equalTo(telefono)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        // Log para verificar existencia en Realtime Database
                        Log.d(TAG, "Verificación de teléfono - Snapshot existe: " + snapshot.exists());

                        if (snapshot.exists()) {
                            // Teléfono encontrado en la base de datos
                            telefonoExiste.set(true);
                            telefonoText.setError("El número de teléfono ya está en uso");
                            Log.w(TAG, "Teléfono encontrado en Realtime Database");
                        }

                        // Verificamos si ambas comprobaciones han terminado para continuar
                        if (verificacionCompleta.get()) {
                            finalizarVerificacion(correoExiste.get(), telefonoExiste.get(), progressDialog);
                        } else {
                            verificacionCompleta.set(true);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        progressDialog.dismiss();
                        Log.e(TAG, "Error al verificar teléfono en Realtime Database", error.toException());
                        Toast.makeText(RegistroActivity.this,
                                "Error al verificar el teléfono",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void finalizarVerificacion(boolean correoExiste, boolean telefonoExiste, ProgressDialog progressDialog) {
        progressDialog.dismiss();

        if (correoExiste && telefonoExiste) {
            Toast.makeText(this, "El correo y el teléfono ya están registrados en una cuena existente", Toast.LENGTH_LONG).show();
        } else if (correoExiste) {
            Toast.makeText(this, "El correo ya está registrado en una cuenta existente", Toast.LENGTH_LONG).show();
        } else if (telefonoExiste) {
            Toast.makeText(this, "El teléfono ya está registrado en una cuenta existente", Toast.LENGTH_LONG).show();
        } else {
            // Ni el correo ni el teléfono existen, continuar con el registro
            continuarRegistro();
        }
    }

    private void continuarRegistro() {
        String nombre = nombreText.getText().toString().trim();
        String apellidos = apellidosText.getText().toString().trim();
        String correo = correoText.getText().toString().trim();
        String contrasena = contrasenaText.getText().toString().trim();
        String telefono = telefonoText.getText().toString().trim();
        String fechaNacimiento = fechaNacimientoText.getText().toString().trim();

        Intent i = new Intent(this, DireccionActivity.class);
        i.putExtra("nombre", nombre);
        i.putExtra("apellidos", apellidos);
        i.putExtra("correo", correo);
        i.putExtra("contrasena", contrasena);
        i.putExtra("telefono", telefono);
        i.putExtra("fechaNacimiento", fechaNacimiento);
        i.putExtra("imagenUri", (imageUri != null) ? imageUri.toString() : "");

        startActivity(i);
    }

    public void abrirLogin(View v) {
        Intent i = new Intent(this, LoginActivity.class);
        startActivity(i);
    }

}
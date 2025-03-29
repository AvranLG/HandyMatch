package com.example.app;




import android.graphics.Bitmap;
import android.os.Bundle;
import android.app.ProgressDialog;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class VerificacionCorreoActivity extends AppCompatActivity {

    private Usuario usuario;
    private String imagenUri;
    private FirebaseAuth mAuth;
    private TextView timerTextView;
    private Button continuarButton;
    private CountDownTimer countDownTimer;
    private static final long TIEMPO_VERIFICACION = 3 * 60 * 1000; // minutos
    private static final long INTERVALO_TICK = 1000; // 1 segundo

    private static final String SUPABASE_URL = "https://yyaepcxpedvbkxsjldtf.supabase.co";
    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inl5YWVwY3hwZWR2Ymt4c2psZHRmIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDI5MjY5NTQsImV4cCI6MjA1ODUwMjk1NH0.B8WbrpGjiMWQxR2cNGKsJ_DXOQbmdA-DW8ygNfCbl_8";  // Coloca tu API key de Supabase aquí
    private static final String STORAGE_BUCKET_NAME = "imagenes-usuarios";  // Nombre del bucket en Supabase
    private ProgressDialog progressDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verificacion_correo);

        mAuth = FirebaseAuth.getInstance();

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Validando datos...");  // Mensaje que se mostrará
        progressDialog.setCancelable(false);  // No permite cancelar el ProgressDialog tocando fuera de él

        // Recuperar datos del intent
        Intent intent = getIntent();
        usuario = (Usuario) intent.getSerializableExtra("usuario");
        imagenUri = intent.getStringExtra("imagenUri");

        TextView correoTextView = findViewById(R.id.correoTextView);
        timerTextView = findViewById(R.id.timerTextView);
        correoTextView.setText("Te hemos enviado un correo de verificación a: " + usuario.getEmail());

        Button reenviarCorreoButton = findViewById(R.id.reenviarCorreoButton);
        continuarButton = findViewById(R.id.continuarButton);

        reenviarCorreoButton.setOnClickListener(v -> reenviarCorreoVerificacion());
        continuarButton.setOnClickListener(v -> verificarCorreo());

        iniciarCuentaRegresiva();
    }

    private void iniciarCuentaRegresiva() {
        countDownTimer = new CountDownTimer(TIEMPO_VERIFICACION, INTERVALO_TICK) {
            @Override
            public void onTick(long millisUntilFinished) {
                long minutos = millisUntilFinished / 60000;
                long segundos = (millisUntilFinished % 60000) / 1000;
                timerTextView.setText(String.format("Tiempo restante: %02d:%02d", minutos, segundos));
            }

            @Override
            public void onFinish() {
                eliminarUsuarioNoVerificado();
            }
        }.start();
    }

    private void reenviarCorreoVerificacion() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user.sendEmailVerification()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Correo de verificación reenviado", Toast.LENGTH_SHORT).show();
                            // Reiniciar cuenta regresiva
                            if (countDownTimer != null) {
                                countDownTimer.cancel();
                            }
                            iniciarCuentaRegresiva();
                        } else {
                            Toast.makeText(this, "Error al reenviar correo de verificación", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void verificarCorreo() {
        progressDialog.show();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user.reload().addOnCompleteListener(task -> {
                if (user.isEmailVerified()) {
                    // Cancelar cuenta regresiva
                    if (countDownTimer != null) {
                        countDownTimer.cancel();
                    }

                    // Subir la imagen a Supabase
                    if (imagenUri != null && !imagenUri.isEmpty()) {
                        Uri imageUri = Uri.parse(imagenUri);
                        subirImagenASupabase(imageUri, usuario);
                    } else {
                        // Si no hay imagen, subir datos directamente
                        subirDatosAFirebase(usuario);
                        progressDialog.dismiss();
                    }
                } else {
                    Toast.makeText(this, "Por favor, verifica tu correo electrónico", Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();
                }
            });
        }
    }
    @Override
    public void onBackPressed() {
        // Si el usuario presiona el botón de retroceso, eliminar el usuario no verificado
        super.onBackPressed();
        eliminarUsuarioNoVerificado();
    }
    private void eliminarUsuarioNoVerificado() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user.delete()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(VerificacionCorreoActivity.this,
                                    "Tiempo de verificación expirado. Usuario no creado.",
                                    Toast.LENGTH_LONG).show();

                            // Redirigir a la pantalla de registro
                            Intent intent = new Intent(VerificacionCorreoActivity.this, RegistroActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(VerificacionCorreoActivity.this,
                                    "Error al eliminar el usuario",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Asegurarse de eliminar el usuario si la actividad es destruida
        if (countDownTimer != null) {
            countDownTimer.cancel();
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null && !user.isEmailVerified()) {
                user.delete();
            }
        }
    }

    private void subirImagenASupabase(Uri imageUri, Usuario usuario) {
        // Verificar que las credenciales estén configuradas
        if (TextUtils.isEmpty(SUPABASE_URL) || TextUtils.isEmpty(STORAGE_BUCKET_NAME) || TextUtils.isEmpty(API_KEY)) {
            Log.e("Supabase", "Credenciales de Supabase no configuradas correctamente");
            return;
        }

        // Nombre de archivo único
        String nombreArchivo = "imagen_" + System.currentTimeMillis() + ".jpg";

        // Construir URL de subida de Supabase
        String url = SUPABASE_URL + "/storage/v1/object/" + STORAGE_BUCKET_NAME + "/" + nombreArchivo;

        // Construir URL pública de la imagen
        String imagenUrlPublica = SUPABASE_URL + "/storage/v1/object/public/" + STORAGE_BUCKET_NAME + "/" + nombreArchivo;

        try {
            // Leer bytes de la imagen
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                Log.e("Supabase", "No se pudo abrir el InputStream de la imagen");
                return;
            }

            // Comprimir la imagen para reducir tamaño
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            byte[] imageBytes = baos.toByteArray();

            // Crear cliente OkHttp con timeouts más largos
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .build();

            // Crear cuerpo de la solicitud
            RequestBody fileBody = RequestBody.create(imageBytes, MediaType.parse("image/jpeg"));
            MultipartBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", nombreArchivo, fileBody)
                    .build();

            // Crear solicitud POST
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("apikey", API_KEY)
                    .addHeader("Authorization", "Bearer " + API_KEY)
                    .addHeader("Content-Type", "multipart/form-data")
                    .post(requestBody)
                    .build();

            // Ejecutar solicitud de forma asíncrona
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseString = response.body().string();
                    Log.d("Supabase", "Respuesta completa de Supabase: " + responseString);

                    if (response.isSuccessful()) {
                        runOnUiThread(() -> {
                            // Guardar la URL pública en el usuario
                            usuario.setImagenUrl(imagenUrlPublica);
                            subirDatosAFirebase(usuario);
                            Toast.makeText(VerificacionCorreoActivity.this, "Imagen subida con éxito", Toast.LENGTH_SHORT).show();
                        });
                    } else {
                        Log.e("Supabase", "Error en la respuesta: " + response.code() + " - " + responseString);
                        mostrarErrorEnUI("Error al subir la imagen: " + response.code());
                    }
                }

                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e("Supabase", "Error en la solicitud", e);
                    mostrarErrorEnUI("Fallo al subir la imagen: " + e.getMessage());
                }

                private void mostrarErrorEnUI(String mensaje) {
                    runOnUiThread(() -> {
                        Toast.makeText(VerificacionCorreoActivity.this, mensaje, Toast.LENGTH_SHORT).show();
                    });
                }
            });

        } catch (Exception e) {
            Log.e("Supabase", "Error general al subir imagen", e);
            Toast.makeText(this, "Error al procesar la imagen", Toast.LENGTH_SHORT).show();
        }
    }


    private void subirDatosAFirebase(Usuario usuario) {
        // Referencia a la base de datos Firebase
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("usuarios");

        // Subir los datos del usuario a Firebase con su clave única de push
        myRef.push().setValue(usuario);

        // Mostrar un mensaje de éxito
        Toast.makeText(this, "Datos registrados exitosamente", Toast.LENGTH_SHORT).show();

        // Opcional: Redirigir a otra actividad, por ejemplo LoginActivity
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // Finaliza la actividad actual para que no pueda volver con el botón de retroceso
    }

}
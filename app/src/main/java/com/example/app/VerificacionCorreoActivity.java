package com.example.app;




import android.graphics.Bitmap;
import android.os.Bundle;
import android.app.ProgressDialog;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONException;
import org.json.JSONObject;

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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

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

                    if (imagenUri != null && !imagenUri.isEmpty()) {
                        Uri imageUri = Uri.parse(imagenUri);
                        subirImagenAFirebaseStorage(imageUri, usuario);
                    } else {
                        subirDatosAFirebase(usuario);
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

    private void subirImagenAFirebaseStorage(Uri imageUri, Usuario usuario) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();

        // Nombre único de archivo
        String nombreArchivo = "imagenes_usuarios/" + System.currentTimeMillis() + ".jpg";
        StorageReference imagenRef = storageRef.child(nombreArchivo);

        imagenRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    imagenRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String urlImagen = uri.toString();
                        usuario.setImagenUrl(urlImagen);
                        subirDatosAFirebase(usuario);
                        Toast.makeText(VerificacionCorreoActivity.this, "Imagen subida con éxito", Toast.LENGTH_SHORT).show();
                    }).addOnFailureListener(e -> {
                        Log.e("FirebaseStorage", "Error al obtener URL", e);
                        Toast.makeText(this, "Error al obtener URL de imagen", Toast.LENGTH_SHORT).show();
                        progressDialog.dismiss();
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e("FirebaseStorage", "Error al subir imagen", e);
                    Toast.makeText(this, "Fallo al subir imagen: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();
                });
    }



    private void subirDatosAFirebase(Usuario usuario) {
        // Obtener el usuario actual de Firebase Authentication
        FirebaseUser firebaseUser = mAuth.getCurrentUser();

        if (firebaseUser != null) {
            String uid = firebaseUser.getUid();

            // Referencia a la base de datos Firebase
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference myRef = database.getReference("usuarios");

            // Subir los datos del usuario usando su UID como clave
            myRef.child(uid).setValue(usuario)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Datos registrados exitosamente", Toast.LENGTH_SHORT).show();

                        // Redirigir a LoginActivity
                        Intent intent = new Intent(this, LoginActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                        progressDialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error al registrar datos: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        progressDialog.dismiss();
                    });
        } else {
            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show();
            progressDialog.dismiss();
        }
    }

}
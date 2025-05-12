package com.example.app;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.yalantis.ucrop.UCrop;

import java.io.IOException;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class ValidarIdentidadActivity extends AppCompatActivity {

    private static final int PICK_IMAGE = 1;
    private static final String TAG = "ValidarIdentidadActivity";

    // UI Components
    private ImageView identityImage;
    private LinearLayout identityPlaceholder;
    private Button btnSeleccionarIne, btnRemoveIdentity, btnEnviarVerificacion;
    private TextView identityStatusText;

    // Variables de estado
    private Uri identityUri;
    private boolean identityVerified = false;
    private AlertDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_validar_identidad);

        initializeUI();
        setupToolbar();
        setupListeners();

        // Restaurar estado si existe
        if (savedInstanceState != null) {
            identityUri = savedInstanceState.getParcelable("identityUri");
            identityVerified = savedInstanceState.getBoolean("identityVerified", false);
            updateUIFromState();
        }

        // Verificar si el usuario ya está verificado
        checkVerificationStatus();
    }

    private void checkVerificationStatus() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference()
                .child("usuarios")
                .child(userId);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean verificado = snapshot.child("verificado").getValue(Boolean.class);
                Log.d(TAG, "Estado de verificación actual: " + verificado);

                if (verificado != null && verificado) {
                    // El usuario ya está verificado, mostrar mensaje y finalizar actividad
                    new AlertDialog.Builder(ValidarIdentidadActivity.this)
                            .setTitle("Cuenta Verificada")
                            .setMessage("Tu cuenta ya se encuentra verificada. La insignia de verificación ya está visible en tu perfil.")
                            .setPositiveButton("Aceptar", (dialog, which) -> {
                                finish();
                            })
                            .setCancelable(false)
                            .show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error al verificar estado: " + error.getMessage());
            }
        });
    }

    private void initializeUI() {
        identityImage = findViewById(R.id.identityImage);
        identityPlaceholder = findViewById(R.id.identityPlaceholder);
        btnSeleccionarIne = findViewById(R.id.btnSeleccionarIne);
        btnRemoveIdentity = findViewById(R.id.btnRemoveIdentity);
        btnEnviarVerificacion = findViewById(R.id.btnEnviarVerificacion);
        identityStatusText = findViewById(R.id.identityStatusText);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupListeners() {
        btnSeleccionarIne.setOnClickListener(v -> openGallery());
        btnRemoveIdentity.setOnClickListener(v -> removeIdentityImage());
        btnEnviarVerificacion.setOnClickListener(v -> submitForVerification());
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE);
    }

    private void removeIdentityImage() {
        identityUri = null;
        identityVerified = false;

        // Actualizar UI
        identityImage.setImageDrawable(null);
        identityImage.setVisibility(View.GONE);
        identityPlaceholder.setVisibility(View.VISIBLE);
        btnRemoveIdentity.setEnabled(false);
        btnEnviarVerificacion.setEnabled(false);
        identityStatusText.setText("No se ha subido ninguna identificación");

        showToast("Identificación removida");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == PICK_IMAGE && data != null) {
                Uri sourceUri = data.getData();
                if (sourceUri != null) {
                    startCrop(sourceUri);
                }
            } else if (requestCode == UCrop.REQUEST_CROP && data != null) {
                Uri resultUri = UCrop.getOutput(data);
                if (resultUri != null) {
                    processIdentityImage(resultUri);
                }
            }
        } else if (resultCode == UCrop.RESULT_ERROR && data != null) {
            final Throwable cropError = UCrop.getError(data);
            Log.e(TAG, "Error al recortar imagen", cropError);
            showError("Error al procesar la imagen");
        }
    }

    private void startCrop(Uri sourceUri) {
        // Crear un destino temporal para la imagen recortada
        Uri destinationUri = Uri.fromFile(new File(getCacheDir(),
                "identity_" + System.currentTimeMillis() + ".jpg"));

        UCrop.Options options = new UCrop.Options();
        options.setCompressionQuality(80);
        options.setHideBottomControls(true);
        options.setShowCropFrame(true);

        // Iniciar UCrop con las proporciones adecuadas para una identificación
        UCrop.of(sourceUri, destinationUri)
                .withAspectRatio(16, 9) // Proporción típica de identificaciones
                .withMaxResultSize(1200, 800)
                .withOptions(options)
                .start(this);
    }

    private void processIdentityImage(Uri resultUri) {
        identityUri = resultUri;

        // Actualizar UI para mostrar la imagen seleccionada
        identityImage.setImageURI(resultUri);
        identityImage.setVisibility(View.VISIBLE);
        identityPlaceholder.setVisibility(View.GONE);
        btnRemoveIdentity.setEnabled(true);
        identityStatusText.setText("Analizando documento...");

        // Analizar la identificación
        analyzeIDDocument(resultUri);
    }

    private void analyzeIDDocument(Uri imageUri) {
        showProgress("Analizando identificación...");

        try {
            InputImage inputImage = InputImage.fromFilePath(this, imageUri);
            TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

            recognizer.process(inputImage)
                    .addOnSuccessListener(this::onTextRecognitionSuccess)
                    .addOnFailureListener(e -> {
                        dismissProgress();
                        Log.e(TAG, "Error en reconocimiento de texto", e);
                        identityStatusText.setText("Error al analizar el documento");
                        showError("No se pudo analizar la identificación");
                    });

        } catch (IOException e) {
            dismissProgress();
            Log.e(TAG, "Error al cargar imagen para OCR", e);
            identityStatusText.setText("Error al procesar el documento");
            showError("Error al procesar la imagen");
        }
    }

    private void onTextRecognitionSuccess(Text text) {
        dismissProgress();
        String recognizedText = text.getText().toUpperCase();
        Log.d(TAG, "Texto reconocido: " + recognizedText);

        // Verificar si el texto contiene patrones típicos de una identificación oficial
        boolean isValidID = verifyIdentityDocument(recognizedText);

        if (isValidID) {
            identityVerified = true;
            identityStatusText.setText("✅ Identificación válida");
            btnEnviarVerificacion.setEnabled(true);
            showToast("Identificación válida detectada");
        } else {
            identityVerified = false;
            identityStatusText.setText("❌ No parece una identificación válida");
            btnEnviarVerificacion.setEnabled(false);
            showToast("No se reconoce como identificación oficial");
        }
    }

    private boolean verifyIdentityDocument(String text) {
        // Patrones comunes en identificaciones mexicanas (INE, pasaporte, etc.)
        Pattern[] patterns = {
                // Clave de elector
                Pattern.compile("[A-Z]{6}\\d{8}[HM][A-Z]{3}"),
                Pattern.compile("[A-Z]{4}\\d{6}[A-Z]{6}\\d{2}"),
                // CURP
                Pattern.compile("[A-Z]{4}\\d{6}[HM][A-Z]{5}[A-Z0-9]{2}"),
                // Palabras clave comunes en identificaciones
                Pattern.compile("INSTITUTO NACIONAL ELECTORAL|CREDENCIAL PARA VOTAR|IFE|INE|REGISTRO NACIONAL")
        };

        // Verificar si el texto contiene alguno de los patrones
        for (Pattern pattern : patterns) {
            if (pattern.matcher(text).find()) {
                return true;
            }
        }

        // Verificar si contiene palabras clave comunes en identificaciones
        String[] keywords = {"MEXICO", "MEXICANO", "NACIONAL", "CREDENCIAL", "ELECTORAL",
                "FEDERAL", "REGISTRO", "INSTITUTO", "IDENTIFICACION", "OFICIAL"};

        int keywordsFound = 0;
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                keywordsFound++;
                if (keywordsFound >= 2) {  // Si encuentra al menos 2 palabras clave
                    return true;
                }
            }
        }

        return false;
    }

    private void submitForVerification() {
        if (!checkNetwork()) {
            return;
        }

        if (identityUri == null) {
            showError("Debes seleccionar una identificación");
            return;
        }

        showProgress("Procesando verificación...");

        // Obtener usuario actual
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // En lugar de subir la imagen, solo actualizar el estado de verificación
        updateUserVerificationStatus(userId);
    }

    private void updateUserVerificationStatus(String userId) {
        // Crear una referencia a la base de datos del usuario
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference()
                .child("usuarios")
                .child(userId);

        // Log para depuración
        Log.d(TAG, "Actualizando estado de verificación para usuario: " + userId);

        // Verificar primero si el nodo existe
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, Object> updates = new HashMap<>();

                // Agregar el campo verificado = true
                updates.put("verificado", true);
                updates.put("fechaVerificacion", System.currentTimeMillis());

                Log.d(TAG, "Nodo usuario existe: " + snapshot.exists());
                Log.d(TAG, "Actualizando con valores: " + updates);

                // Actualizar los datos
                userRef.updateChildren(updates)
                        .addOnSuccessListener(aVoid -> {
                            dismissProgress();
                            Log.d(TAG, "Estado de verificación actualizado exitosamente");

                            // Verificar que se haya actualizado correctamente
                            userRef.child("verificado").addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    Boolean verificado = dataSnapshot.getValue(Boolean.class);
                                    Log.d(TAG, "Verificación después de actualizar: " + verificado);
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Log.e(TAG, "Error al verificar actualización: " + error.getMessage());
                                }
                            });

                            showVerificationSuccess();
                        })
                        .addOnFailureListener(e -> {
                            dismissProgress();
                            showError("Error al actualizar el estado");
                            Log.e(TAG, "Error al actualizar estado usuario: " + e.getMessage());
                        });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                dismissProgress();
                showError("Error al verificar la existencia del usuario");
                Log.e(TAG, "Error al acceder a datos de usuario: " + error.getMessage());
            }
        });
    }

    private void showVerificationSuccess() {
        new AlertDialog.Builder(this)
                .setTitle("Verificación Exitosa")
                .setMessage("Tu cuenta ha sido verificada correctamente. Ahora se mostrará una insignia de verificación en tu perfil.")
                .setPositiveButton("Aceptar", (dialog, which) -> {
                    setResult(RESULT_OK);
                    finish();
                })
                .setCancelable(false)
                .show();
    }

    // Helpers UI
    private void showProgress(String message) {
        dismissProgress();
        progressDialog = new AlertDialog.Builder(this)
                .setMessage(message)
                .setCancelable(false)
                .show();
    }

    private void dismissProgress() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private boolean checkNetwork() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnected();

        if (!isConnected) {
            showError("No hay conexión a internet");
        }

        return isConnected;
    }

    private void updateUIFromState() {
        if (identityUri != null) {
            identityImage.setImageURI(identityUri);
            identityImage.setVisibility(View.VISIBLE);
            identityPlaceholder.setVisibility(View.GONE);
            btnRemoveIdentity.setEnabled(true);

            if (identityVerified) {
                identityStatusText.setText("✅ Identificación válida");
                btnEnviarVerificacion.setEnabled(true);
            } else {
                identityStatusText.setText("❌ No parece una identificación válida");
                btnEnviarVerificacion.setEnabled(false);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("identityUri", identityUri);
        outState.putBoolean("identityVerified", identityVerified);
    }

    @Override
    protected void onDestroy() {
        dismissProgress();
        super.onDestroy();
    }
}
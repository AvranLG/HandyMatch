package com.example.app;

import android.os.Bundle;
import android.view.WindowManager;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        // Para evitar que la barra de navegación se sobreponga al contenido
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        // Manejar el botón de retroceso con OnBackPressedDispatcher
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                mostrarDialogoSalida();
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Configurar la barra de navegación
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);

        // Aplicar colores de la configuración anterior
        bottomNavigationView.setItemIconTintList(ContextCompat.getColorStateList(this, R.color.fondosyletras));
        bottomNavigationView.setItemTextColor(ContextCompat.getColorStateList(this, R.color.fondosyletras));

        // Cargar fragment inicial (Home)
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        }

        // Configurar listener para la navegación
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                loadFragment(new HomeFragment());
                return true;
            } else if (id == R.id.nav_lupa) {
                loadFragment(new BuscarFragment());
                return true;
            } else if (id == R.id.nav_ubicacion) {
                //loadFragment(new UbicacionFragment());
                return true;
            } else if (id == R.id.nav_perfil) {
                //loadFragment(new PerfilFragment());
                return true;
            } else if (id == R.id.nav_mensaje) {
                //loadFragment(new MensajesFragment());
                return true;
            }
            return false;
        });
    }

    /**
     * Carga un fragmento en el contenedor principal
     * @param fragment Fragmento a cargar
     */
    private void loadFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        // Aplica las animaciones de transición (deslizar)
        transaction.setCustomAnimations(
                R.anim.slide_in_right,  // Animación para el fragmento que entra
                R.anim.slide_out_left   // Animación para el fragmento que sale
        );

        // Reemplazar el fragmento actual con el nuevo fragmento
        transaction.replace(R.id.fragmentContainer, fragment);
        transaction.addToBackStack(null); // Opcional: Añadir al back stack si deseas soportar el botón de retroceso
        transaction.commit();
    }


    private void mostrarDialogoSalida() {
        new AlertDialog.Builder(this)
                .setTitle("Salir de HandyMatch")
                .setMessage("¿Seguro que quieres salir de HandyMatch?")
                .setPositiveButton("Sí", (dialog, which) -> finishAffinity()) // Cierra la app
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss()) // Cancela el cierre
                .show();
    }
}
package com.example.atcarremote;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Passer à ConnectionActivity après un délai de 2 secondes
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(SplashActivity.this, ConnectionActivity.class);
                startActivity(intent);
                finish(); // Ferme cette activité pour éviter de revenir en arrière
            }
        }, 2000); // 2000ms = 2 secondes (modifie si nécessaire)
    }
}

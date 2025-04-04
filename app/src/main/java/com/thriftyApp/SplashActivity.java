package com.thriftyApp;

import android.content.Intent;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;


public class SplashActivity extends AppCompatActivity {

    private static int SPLASH_TIME_OUT = 3000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate (savedInstanceState);
        setContentView (R.layout.activity_splash);

        ImageView imageView = findViewById(R.id.imageViewLoading);

        /*from raw folder*/
        Glide.with(this)
                .load(R.raw.loading)
                .into(imageView);

        new Handler ().postDelayed (() -> {

            Intent intent = new Intent (getApplicationContext ( ), Dashboard.class);
            startActivity (intent);
            finish ( );
        },SPLASH_TIME_OUT);

    }
}

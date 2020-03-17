package com.example.recipemng;

import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.content.res.Resources;
import android.widget.Button;
import android.widget.TextView;


public class FirstActivity extends AppCompatActivity {
    private Button btn_make, btn_cake;
    TextView header;

    @Override
    protected void onCreate(Bundle saveInstanceState)
    {
        super.onCreate(saveInstanceState);
        setContentView(R.layout.activity_first);
        // header = findViewById(R.id.header);
        final Resources resources = getResources();

        btn_cake=(Button)findViewById(R.id.btn_cake);
        btn_cake.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                openGameActivity("cake");
            }
        });

        btn_make=(Button)findViewById(R.id.btn_make);
        btn_make.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                openGameActivity("make");
            }
        });
    }
    public void openGameActivity(String recipe){
        Intent intent=new Intent(FirstActivity.this,MainActivity.class);
        intent.putExtra("recipe",recipe);
        startActivity(intent);
    }
}

package com.example.recipemng;
//https://console.firebase.google.com/project/recipemanage/database/recipemanage/data
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.loader.content.CursorLoader;

import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    // main activity is the main page associated with creating new recipes

    private static final String TAG = MainActivity.class.getSimpleName();
    private TextView textInfo, textIngridients, urlString;
    private EditText inputName, inputInstructions;
    private Button btnSave, btnLoad, btnNext, btnShowMore;
    private ImageView imageView;
    private DatabaseReference mFirebaseDatabase;
    private FirebaseDatabase mFirebaseInstance;
    private String itemId;
    private Fragment fr;
    private Uri filePath;
    private final int PICK_IMAGE_REQUEST = 71;
    String recipe; //
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent intent = getIntent();
        recipe = intent.getStringExtra("recipe"); // are we handling bakes or makes?
        Log.e(TAG, "running main activity with recipe $" + recipe + "$");
        if (recipe.equals("cake"))
            getSupportActionBar().setSubtitle(getResources().getString(R.string.app_bake));
        else // "make"
            getSupportActionBar().setSubtitle(getResources().getString(R.string.app_make));

        setContentView(R.layout.activity_main);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(R.mipmap.ic_launcher);
        handleIntent(getIntent());

        // invoking fragment GetIngridients from MainActivity, passing "1" to denote Main page rather than Input page
        Bundle bundle=new Bundle();
        bundle.putString("activity", "1");
        bundle.putString("recipe", recipe);

        fr = new GetIngridients();
        fr.setArguments(bundle);
        FragmentManager fm = getFragmentManager();
        FragmentTransaction fragmentTransaction = fm.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_place, fr);
        fragmentTransaction.commit();

        // initializing display variables
        textInfo = findViewById(R.id.info);
        inputName = findViewById(R.id.name);
        textIngridients = findViewById(R.id.ingridients);
        urlString = findViewById(R.id.imageUrl);
        inputInstructions = findViewById(R.id.instructions);

        btnSave = findViewById(R.id.btn_save);
        btnLoad = findViewById(R.id.btn_load);
        btnNext = findViewById(R.id.btn_next);
        btnShowMore = (Button) findViewById(R.id.btn_Showmore);
        imageView = (ImageView) findViewById(R.id.imgView);

        // initializing "Realtime Datrabase" firebase RecipeManage - MayaRecipe
        mFirebaseInstance = FirebaseDatabase.getInstance();
        mFirebaseDatabase = mFirebaseInstance.getReference("recipemanage");
        mFirebaseInstance.getReference("app_title").setValue("Maya Recipe");
        mFirebaseInstance.getReference("app_title").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.e(TAG, "App title updated");
                String appTitle = dataSnapshot.getValue(String.class);
                getSupportActionBar().setTitle(appTitle);
            }
            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Failed to read app title value.", error.toException());
            }
        });

        // defining button - next to arrive to find a recipe
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                reLoadFragment();
                Intent intent=new Intent(MainActivity.this,InputActivity.class);
                intent.putExtra("recipe",recipe);
                startActivity(intent);
            }
        });

        // defining button - showmore to expand instructions tab
        btnShowMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (btnShowMore.getText().toString().equalsIgnoreCase("Show More"))
                {
                    inputInstructions.setMaxLines(Integer.MAX_VALUE);//your TextView
                    inputInstructions.setSelection(inputInstructions.getText().length());
                    btnShowMore.setText("Show Less");
                }
                else
                {
                    inputInstructions.setMaxLines(1);//collapse instructions to 1 line
                    inputInstructions.setSelection(0); // set focus on first character so that 1st line is shown
                    btnShowMore.setText("Show More");
                }
            }
        });

        // defining button - load an image from gallery - calls startActivity and sets urlString
        btnLoad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
            }
        });

        // defining button - save to save data to firebase (or update if itemId exists)
        // name must be not-null to save and item. the rest may be null.
        // calls createItem or updateItem
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.e(TAG, "btn save is called");
                String name = inputName.getText().toString();
                String ingridients = textIngridients.getText().toString();
                String imageUrl = urlString.getText().toString();
                String instructions = inputInstructions.getText().toString();

                if (TextUtils.isEmpty(itemId)) {
                    if (TextUtils.isEmpty(name))
                        textInfo.setText("cannot save an empty entry");
                    else {
                        //textInfo.setText("saving data");
                        createItem(name, ingridients, imageUrl, instructions);
                    }
                } else {
                    //textInfo.setText("updating data" + itemId);
                    updateItem(name, ingridients, imageUrl, instructions);
                }
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }
    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            //use the query to search your data somehow
        }
    }

    // function to reload fragment getIngridients in order too clear history of used ingridients view item (squares)
    public void reLoadFragment() {
        inputName.setText("");
        textIngridients.setText("");
        inputInstructions.setText("");
        itemId = "";

        Bundle bundle=new Bundle();
        bundle.putString("activity", "1");
        bundle.putString("recipe", recipe);
        fr = new GetIngridients();
        fr.setArguments(bundle);
        getFragmentManager().beginTransaction().replace(R.id.fragment_place, fr).commit();
        Log.e(TAG, "reloading fragment done");
    }

    // called by btnLoad onActivity - used to fetch image from gallery of pictures in cellphone
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null ) {
            filePath = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath);
                imageView.setImageBitmap(bitmap);
                Log.e(TAG, "setting bitmap "+filePath.toString());
                String fileLocation = getRealPathFromURI(filePath);
                urlString.setText(fileLocation);
                Log.e(TAG, "loading new image " + fileLocation);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            catch (Exception e) {
                Log.e(TAG, "Exception getRealPathFromURI " + filePath.toString());
            }
        }
    }
        // get full path of gallery pic
    public String getRealPathFromURI(Uri uri){
        Log.e(TAG,"build version "+Build.VERSION.SDK_INT);
        String filePath = "";
        if (DocumentsContract.isDocumentUri(this, uri)) {
            String wholeID = DocumentsContract.getDocumentId(uri);
            String id = wholeID.split(":")[1];
            String[] column = {MediaStore.Images.Media.DATA};
            String sel = MediaStore.Images.Media._ID + "=?";
            Log.e(TAG, "supported URI - before context" + uri.toString());
            Cursor cursor = getApplicationContext().getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    column, sel, new String[]{id}, null);
            Log.e(TAG, "after context ");

            int columnIndex = cursor.getColumnIndex(column[0]); //
            if (cursor.moveToFirst()) {
                filePath = cursor.getString(columnIndex);
            }
            Log.e(TAG, "converting camera path from " + uri + " to " + filePath);
            cursor.close();
            return filePath;
        }
        else
            Toast.makeText(MainActivity.this, "non supported image document", Toast.LENGTH_SHORT).show();
            return uri.toString();
    }

    // saves (with addItemChangeListener) and item into the firebase real-time database
    private void createItem(String name,String ingridients,String imageUrl, String instructions) {
        if (TextUtils.isEmpty(itemId)) {
            itemId = mFirebaseDatabase.push().getKey();
            //textInfo.setText("creating Recipe: " + itemId);
        }
        RecipeItem item = new RecipeItem(name, ingridients, imageUrl, instructions);
        mFirebaseDatabase.child(itemId).setValue(item);
        addItemChangeListener();
        Toast.makeText(MainActivity.this, "Recipe saved", Toast.LENGTH_SHORT).show();
        clear_text();
    }

    // clear text fields
    private void clear_text() {
        inputName.setText("");
        textIngridients.setText("");
        urlString.setText("");
        inputInstructions.setText("");
        imageView.setImageDrawable(null);
       textInfo.setText("please enter your recipes");
    }

    private void addItemChangeListener() {
        mFirebaseDatabase.child(itemId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                RecipeItem item = dataSnapshot.getValue(RecipeItem.class);
                if (item == null) {
                    Log.e(TAG, "RecipeItem data is null!");
                    return;
                }
                Log.e(TAG, "RecipeItem data is changed!" + item.name + ", " + item.ingridients + ", " + item.instructions);
                //textInfo.setText(item.name + ", " + item.ingridients + ", " + item.instructions);
                reLoadFragment();
            }
            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Failed to read Recipe Item", error.toException());
            }
        });
    }

    // updates an existing item into the firebase real-time database
    private void updateItem(String name, String ingridients, String imageUrl, String instructions) {
        // updating the user via child nodes
        if (!TextUtils.isEmpty(name))
            mFirebaseDatabase.child(itemId).child("name").setValue(name);
        if (!TextUtils.isEmpty(ingridients))
            mFirebaseDatabase.child(itemId).child("ingridients").setValue(ingridients);
        if (!TextUtils.isEmpty(imageUrl))
            mFirebaseDatabase.child(itemId).child("imageUrl").setValue(imageUrl);
        if (!TextUtils.isEmpty(instructions))
            mFirebaseDatabase.child(itemId).child("instructions").setValue(instructions);

        Toast.makeText(MainActivity.this, "Recipe updated", Toast.LENGTH_SHORT).show();
    }

    // public function called from fragment to update textIngridients with selected fragemnt ingridients
    public void update_ingridients(String cur_ingr) {
        textIngridients.setText(cur_ingr);
    }
}
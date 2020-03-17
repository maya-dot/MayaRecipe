package com.example.recipemng;
//https://console.firebase.google.com/project/recipemanage/database/recipemanage/data realtime database
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Environment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.ListView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

public class InputActivity extends AppCompatActivity {

    // this activity searches a recipe in db and displays to user
    private static final String TAG = MainActivity.class.getSimpleName();
    private TextView textInfo, textIngridients;
    private Button btnLoad, btnFind;
    private DatabaseReference mFirebaseDatabase;
    private FirebaseDatabase mFirebaseInstance;
    List<String> items;
    private ListView listView;
    private ImageView imageView;
    private boolean SearchWith = true; // default is search with item and not search from items
    String recipe;

    // state global parameters fragment manager in order to allow change of fragment from input activity
    Fragment fr;
    FragmentManager fm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        recipe = intent.getStringExtra("recipe"); // are we handling bakes or makes?
        setContentView(R.layout.activity_input);
        Log.e(TAG, "running input activity with recipe $" + recipe + "$");

        if (recipe.equals("cake"))
            getSupportActionBar().setSubtitle(getResources().getString(R.string.app_bake));
        else // "make"
            getSupportActionBar().setSubtitle(getResources().getString(R.string.app_make));

        // sets activity with fragment GetIngridients, this time by parameter 2
        Bundle bundle=new Bundle();
        bundle.putString("activity", "2");
        bundle.putString("recipe",recipe);
        fr = new GetIngridients();
        fr.setArguments(bundle);
        fm = getFragmentManager();
        FragmentTransaction fragmentTransaction = fm.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_place, fr);
        fragmentTransaction.commit();
        //getSupportActionBar().setDisplayShowHomeEnabled(true);
        //getSupportActionBar().setIcon(R.mipmap.ic_launcher);

        // defining display widgets
        textInfo = findViewById(R.id.info);
        textInfo.setText("Welcome. enter ingridients to find recipe");

        textIngridients = findViewById(R.id.ingridients);
        btnLoad = (Button) findViewById(R.id.btn_load);
        btnFind = (Button) findViewById(R.id.btn_find);
        imageView = (ImageView) findViewById(R.id.imgView);

        // listview shows the item that were selected by eitherload or find and enable their picture display
        // defining the search list of items from database, define activity of list item click
        // needs to be changed that when clicking a line - opens the ingridients / updates list and adds image
        listView = (ListView) findViewById(R.id.list_view);
        listView.setClickable(true);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                Object o = listView.getItemAtPosition(position);
                textInfo.setText("selecting "+o.toString());

                String textstr = items.get(position);
                int pos0 = textstr.lastIndexOf("%");
                int pos1 = textstr.lastIndexOf("$");
                int pos2 = textstr.lastIndexOf("@");
                String db_instructions = textstr.substring(pos0+1,pos1);
                String db_ingridients = textstr.substring(pos1+1,pos2);
                String pic = textstr.substring(pos2+1);
                Log.e(TAG, "selecting for display - " + pic);

                String usr_ingridients = textIngridients.getText().toString();
                changeFragmentGridView(usr_ingridients,db_ingridients); // mark in gray additional entries per selected recipe line

                // if item is content - fetch from image library, else fetches from the internet
                if (pic.startsWith("/storage")) { // gallery
                    File file_name = new File (pic);
                    if(file_name.exists()) // path does not exist. this is the problem.
                        Picasso.with(InputActivity.this).load("file://" + file_name).config(Bitmap.Config.RGB_565).
                                fit().centerCrop().into(imageView);
                    else
                        Log.e(TAG, "file does not exist " + file_name.toString());
                }
                else if (pic != null && !pic.isEmpty()) // internet works!
                    PicassoTrustAll.getInstance(InputActivity.this).load(pic).into(imageView);
                else // empty pic
                    textInfo.setText("cannot display empty picture");

                if (!db_instructions.isEmpty())
                    popup_instructions(db_instructions);
            }
        });

        // initializes database
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

         // defines load button - traverses firebase database,  loads all data
        btnLoad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.e(TAG, "btn load is called");
                Fragment frag = fm.findFragmentById(R.id.fragment_place);
                GridView gv = frag.getView().findViewById(R.id.gridview);
                GetIngridients.clear_selection(gv); // remove all marks from grid. fetching all data
                textIngridients.setText("");

                ValueEventListener valueEventListener = new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        fetch_data(dataSnapshot,true);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                };
                mFirebaseDatabase.addListenerForSingleValueEvent(valueEventListener);
            }
        });

        // defines find button - traverses firebase database,  loads only matched data (fetch_datea = false)
        btnFind.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.e(TAG, "btn find is called");

                ValueEventListener valueEventListener = new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        fetch_data(dataSnapshot,false);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                };
                mFirebaseDatabase.addListenerForSingleValueEvent(valueEventListener);
            }
        });
    }

    // fetches data from firebase either all or selected by ticked ingridients
    // called by btnLoad or btnFind
    void fetch_data(DataSnapshot dataSnapshot, boolean all) {
        items = new ArrayList<>();
        List<String> listview_items = new ArrayList<>();
        for(DataSnapshot ds : dataSnapshot.getChildren()) {
            String key = ds.getKey();
            String name = ds.child("name").getValue(String.class);
            String ingridients = ds.child("ingridients").getValue(String.class);
            String instructions = ds.child("instructions").getValue(String.class);
            String url = ds.child("imageUrl").getValue(String.class);
            boolean match = all || ingridients_match(ingridients,textIngridients.getText().toString());
            if (match) {
                items.add(name + "%" + instructions + "$" + ingridients + "@" + url); // complete version with urls (not for display)
                listview_items.add("recipe: " + name + ":[" + ingridients + "]"); // short version (for display)
                Log.d(TAG, "fetching " + name + " / " + ingridients + " / " + url);
            }
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(InputActivity.this, android.R.layout.simple_list_item_1, listview_items);
        if (listview_items.isEmpty()) {
            textInfo.setText("No matched Recipe found");
            adapter.clear();
            adapter.notifyDataSetChanged();
        }
        else {
            //textInfo.setText("");
        }
        listView.setAdapter(adapter);
    }

    // popup window to display instruction for selected listview line
    private void popup_instructions(String Instructions) {
        // inflate the layout of the popup window
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.popup_window, null);

        // create the popup window
        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        boolean focusable = true; // lets taps outside the popup also dismiss it
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, focusable);
        popupWindow.showAtLocation(this.findViewById(R.id.activity_input), Gravity.CENTER, 0, 0);
        TextView txt = (TextView) popupWindow.getContentView().findViewById(R.id.textMsg);
        txt.setText(Instructions);

        popupView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                popupWindow.dismiss();
                return true;
            }
        });
    }

    //utility to convert a string of ingridients to a set of ingridients for manipulation of found items
    Set convert_str_to_set (String str) {
        String[] array = str.split(" "); // convert db ingr via array ot strings to set1
        Set set = new HashSet();
        Collections.addAll(set, array);
        Log.d(TAG, "set "+ str + " " + set.toString());
        return set;
    }
    // tests if selected ingridients are subset of firebase ingridients
    boolean ingridients_match(String db_ingridients,String usr_ingridients) {
        boolean containing = true;
        Set db_set = convert_str_to_set (db_ingridients);
        Set usr_set = convert_str_to_set (usr_ingridients);

        if (SearchWith) { // find all db recipes that has the specified items in them
            textInfo.setText("The following recipes contain marked items");
            Iterator iter = usr_set.iterator();
            while (containing && iter.hasNext()) {
                containing = containing && db_set.contains(iter.next()); // => rational I'd like some chocolate
            }
            Log.d(TAG, "db set includes items? "+containing);
        }
        else {
            textInfo.setText("The following recipes could be made with marked items");
            Iterator iter = db_set.iterator();
            while (containing && iter.hasNext()) {
                containing = containing && usr_set.contains(iter.next()); // => rational I only have these ingridients at home
            }
            Log.d(TAG, "usr set includes items? "+containing);
        }
        return (containing); // returns if user ingridinets are in db ingridients
    }

    // handle search image. implemented but not intended for use
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        // Associate searchable configuration with the SearchView
        //SearchManager searchManager = (SearchManager) getSystemService(this.SEARCH_SERVICE);
        //SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
        //searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        //searchView.setIconifiedByDefault(false);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // clear all selections when changing options
        Fragment frag = fm.findFragmentById(R.id.fragment_place);
        GridView gv = frag.getView().findViewById(R.id.gridview);
        GetIngridients.clear_selection(gv);
        textIngridients.setText("");

        //Handle item selection
        switch (item.getItemId()) {
            //case R.id.search:
            //    Toast.makeText(getApplicationContext(), "Search button clicked", Toast.LENGTH_SHORT).show();
            //    return true;
            case R.id.recipeWith:
                Toast.makeText(getApplicationContext(), "Setting search  - recipe with ingridients", Toast.LENGTH_SHORT).show();
                SearchWith = true;
                return true;
            case R.id.recipeFrom:
                Toast.makeText(getApplicationContext(), "Setting search  - recipe from ingridients", Toast.LENGTH_SHORT).show();
                SearchWith = false;
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    // set missing ingridients in fragment from main activity according to difference in sets: db .vs. selection
    public void changeFragmentGridView(String usr_ingridients, String  db_ingridients) {
        Fragment frag = fm.findFragmentById(R.id.fragment_place);
        GridView gv = frag.getView().findViewById(R.id.gridview);
        Set db_set = convert_str_to_set (db_ingridients);
        Set usr_set = convert_str_to_set (usr_ingridients);
        GetIngridients.mark_missing(gv,usr_set,db_set,SearchWith);
    }

    // public function called from fragment to update textIngridients with selected fragemnt ingridients
    public void update_ingridients(String cur_ingr) {
        textIngridients.setText(cur_ingr);
    }

    private static File getGalleryPath() {
        return  Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
    }
    /*
    private void OpenGallery(){
        Intent getImageIntent = new Intent(Intent.ACTION_GET_CONTENT);
        getImageIntent .setType("image/*");
        startActivityForResult(getImageIntent , IMAGE_PICKER );
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode== IMAGE_PICKER  && resultCode == RESULT_OK) {
            Uri fullPhotoUri = data.getData();
            imageView.setImageURI(fullPhotoUri);
        }
    }
     */
}
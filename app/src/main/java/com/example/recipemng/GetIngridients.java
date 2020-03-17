package com.example.recipemng;

import android.app.Fragment;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;

import java.util.Set;

import static android.content.ContentValues.TAG;

public class GetIngridients extends Fragment {

    public static String[] Titles_bake =
            {"Flour", "Eggs", "Milk", "Honey", "Sugar", "Oil", "Chocolate", "Cacao", "Butter", "Cream", "Baking-powder", "Nuts"};
    public static String[] Titles_make =
            {"Vegetables", "Herbs", "Tomato-sauce", "onions", "Eggs", "Oil", "Pasta", "Flour", "Garlic", "Bread-crumbs", "Rice","Fruits"};
    public static String[] Titles = new String[12];
    private Boolean[] pressed;
    String father = "1"; // container activity - mainActivity (1) or inputActivity (2)
    String recipe = "bake"; // container titles - bake or make
    GridView gv;
    String cur_ingr = ""; // this string will be passed from the fragment to the mainactivity by public function
    ArrayAdapter adapterCB;
    View view;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            father = getArguments().getString("activity");
            recipe = getArguments().getString("recipe");
        }
        if (recipe.equals("cake")) {
            Log.e(TAG, "running get ingredients fragment with cake recipe $" + recipe + "$ activity "+ father);
            for (int i = 0; i < Titles_bake.length; i++)
                Titles[i] = Titles_bake[i];
        }
        else {
            Log.e(TAG, "running get ingredients fragment with make recipe $" + recipe + "$ activity "+ father);
            for (int i = 0; i < Titles_make.length; i++)
                Titles[i] = Titles_make[i];
        }
        pressed = new Boolean[Titles.length];
        Log.e(TAG, "setting titles and pressed");

    }

    // clears all selections, called from inputActivity when changing selection option, and internally
    public static void clear_gray_marks(GridView gridV) {
        for (int i = 0; i < Titles.length; i++) {
            gridV.getChildAt(i).setBackgroundColor(Color.TRANSPARENT); // clean all markers
        }
    }

    // clears all selections, called from inputActivity when changing selection option, and internally
    public static void clear_selection(GridView gridV) {
        for (int i = 0; i < Titles.length; i++) {
            gridV.setItemChecked(i, false);
            gridV.getChildAt(i).setTag("notselected"); // DOES NOT WORK!
        }
    }
    // if searchwith, it would highlight (gray) all the additional ingridients
    // otherwise, it would highlight (gray) the required ingridients out of those marked.
    public static void mark_missing(GridView gridV, Set usr_set, Set db_set, boolean SearchWith) {
        String ing;
        clear_gray_marks(gridV);

        if (SearchWith) { // highlight the additional ingridients that are in db_set but not in usr_set
            db_set.removeAll(usr_set);
        }
        else { // highlight the ingridients that are in both db_set and usr_set - only they are required
            db_set.retainAll(usr_set);
        }

        // traverse list and for each ingridient that is in working set set light gray background coloe
        for (int i = 0; i < Titles.length; i++) {
            ing = gridV.getItemAtPosition(i).toString();
            if (db_set.contains(ing))
                gridV.getChildAt(i).setBackgroundColor(Color.LTGRAY);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.get_ingridients, container, false);
        gv = (GridView) view.findViewById(R.id.gridview);
        gv.setChoiceMode(GridView.CHOICE_MODE_MULTIPLE);

        adapterCB = new ArrayAdapter(getActivity(), android.R.layout.simple_list_item_multiple_choice, Titles);
        gv.setAdapter(adapterCB);
        for (int i = 0; i < Titles.length; i++)
            pressed[i] = false;
        //for (int i = 0; i < adapterCB.getCount(); i++)
        //    gv.setItemChecked(i, false);

        gv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                for (int i = 0; i < Titles.length; i++)
                    gv.getChildAt(i).setBackgroundColor(Color.TRANSPARENT); // clean all markers

                String selectedItem = parent.getItemAtPosition(position).toString();
                if (!pressed[position]) { // add grid element
                    cur_ingr = cur_ingr + selectedItem + " ";
                    v.setTag("selected");
                    gv.setItemChecked(position, true);
                    // gv.getChildAt(position).setBackgroundColor(Color.LTGRAY);
                    pressed[position] = true;
                } else { // remove grid element
                    cur_ingr = cur_ingr.replace(selectedItem+" ","");
                    gv.setItemChecked(position, false);
                    v.setTag("notselected");
                    // gv.getChildAt(position).setBackgroundColor(Color.TRANSPARENT);
                    pressed[position] = false;
                }
                if (father == "1")
                    ((MainActivity)getActivity()).update_ingridients(cur_ingr);
                else
                    ((InputActivity)getActivity()).update_ingridients(cur_ingr);
            }
        });
        return view;
    }
}

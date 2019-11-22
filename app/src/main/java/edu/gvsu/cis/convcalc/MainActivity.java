package edu.gvsu.cis.convcalc;

import static edu.gvsu.cis.convcalc.WeatherService.BROADCAST_WEATHER;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import edu.gvsu.cis.convcalc.UnitsConverter.LengthUnits;
import edu.gvsu.cis.convcalc.UnitsConverter.VolumeUnits;
import edu.gvsu.cis.convcalc.dummy.HistoryContent;
import edu.gvsu.cis.convcalc.dummy.HistoryContent.HistoryItem;
import java.util.ArrayList;
import java.util.List;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

public class MainActivity extends AppCompatActivity {

    public static int SETTINGS_RESULT = 1;
    public static int HISTORY_RESULT = 2;
    private HistoryItem item;

    private enum Mode {Length, Volume};

    public DatabaseReference topRef;

    public static List<HistoryContent.HistoryItem> allHistory;

    private Mode mode = Mode.Length;
    private Button calcButton;
    private Button clearButton;
    private Button modeButton;

    private EditText toField;
    private EditText fromField;


    private TextView toUnits;
    private TextView fromUnits;
    private TextView title;
    private TextView current;
    private TextView temperature;

    private ImageView weatherIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        allHistory = new ArrayList<HistoryContent.HistoryItem>();

        calcButton = findViewById(R.id.calcButton);
        clearButton = findViewById(R.id.clearButton);
        modeButton = findViewById(R.id.modeButton);

        toField = findViewById(R.id.to);
        fromField = findViewById(R.id.from);

        fromUnits = findViewById(R.id.fromUnits);
        toUnits = findViewById(R.id.toUnits);

        title = findViewById(R.id.title);

        current = findViewById(R.id.current);
        temperature = findViewById(R.id.temperature);
        weatherIcon = findViewById(R.id.weatherIcon);

        weatherIcon.setImageResource(android.R.color.transparent);

        calcButton.setOnClickListener(v -> {
            doConversion();
        });

        clearButton.setOnClickListener(v -> {
            toField.setText("");
            fromField.setText("");
            hideKeyboard();
        });

        modeButton.setOnClickListener(v -> {
            toField.setText("");
            fromField.setText("");
            hideKeyboard();
            switch(mode) {
                case Length:
                    mode = Mode.Volume;
                    fromUnits.setText(VolumeUnits.Gallons.toString());
                    toUnits.setText(VolumeUnits.Liters.toString());
                    break;
                case Volume:
                    mode = Mode.Length;
                    fromUnits.setText(LengthUnits.Yards.toString());
                    toUnits.setText(LengthUnits.Meters.toString());
                    break;
            }
            title.setText(mode.toString() + " Converter");
        });

        toField.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                fromField.getText().clear();
            }
        });

        fromField.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                toField.getText().clear();
            }
        });

//
    }

    private void doConversion() {
        EditText dest = null;
        String val = "";
        String fromVal = fromField.getText().toString();
        DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
        WeatherService.startGetWeather(this, "42.963686", "-85.888595", "p1");
        if (fromVal.intern() != "" ) {
            val = fromVal;
            dest = toField;
        }
        String toVal = toField.getText().toString();
        if (toVal.intern() != "") {
            val = toVal;
            dest = fromField;
        }

        if (dest != null) {
            switch(mode) {
                case Length:
                    LengthUnits tUnits, fUnits;
                    if(dest == toField) {
                        fUnits = LengthUnits.valueOf(fromUnits.getText().toString());
                        tUnits = LengthUnits.valueOf(toUnits.getText().toString());
                    } else {
                        fUnits = LengthUnits.valueOf(toUnits.getText().toString());
                        tUnits = LengthUnits.valueOf(fromUnits.getText().toString());
                    }
                    Double dVal = Double.parseDouble(val);
                    Double cVal = UnitsConverter.convert(dVal, fUnits, tUnits);
                    dest.setText(Double.toString(cVal));

                    // remember the calculation.
                    HistoryContent.HistoryItem item = new HistoryContent.HistoryItem(dVal, cVal, mode.toString(),
                        fUnits.toString(), tUnits.toString(), fmt.print(DateTime.now()));
                    HistoryContent.addItem(item);
                    topRef.push().setValue(item);
                    break;
                case Volume:
                    VolumeUnits vtUnits, vfUnits;
                    if(dest == toField) {
                        vfUnits = VolumeUnits.valueOf(fromUnits.getText().toString());
                        vtUnits = VolumeUnits.valueOf(toUnits.getText().toString());
                    } else {
                        vfUnits = VolumeUnits.valueOf(toUnits.getText().toString());
                        vtUnits = VolumeUnits.valueOf(fromUnits.getText().toString());
                    }
                    Double vdVal = Double.parseDouble(val);
                    Double vcVal = UnitsConverter.convert(vdVal, vfUnits, vtUnits);
                    dest.setText(Double.toString(vcVal));

                    // remember the calculation.

                    HistoryContent.HistoryItem item2 = new HistoryContent.HistoryItem(vdVal, vcVal, mode.toString(),
                        vfUnits.toString(), vtUnits.toString(), fmt.print(DateTime.now()));
                    HistoryContent.addItem(item2);
                    topRef.push().setValue(item2);
                    break;
            }
        }
        hideKeyboard();

    }

    private void hideKeyboard()
    {
        // Check if no view has focus:
        View view = this.getCurrentFocus();
        if (view != null) {
            //this.getSystemService(Context.INPUT_METHOD_SERVICE);
            InputMethodManager imm = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.action_settings) {
            Intent intent = new Intent(MainActivity.this, MySettingsActivity.class);
            intent.putExtra("mode", mode.toString());
            intent.putExtra("fromUnits", fromUnits.getText().toString());
            intent.putExtra("toUnits", toUnits.getText().toString());
            startActivityForResult(intent, SETTINGS_RESULT );
            return true;
        }else if(item.getItemId() == R.id.action_history) {
            Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
            startActivityForResult(intent, HISTORY_RESULT );
            return true;
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == SETTINGS_RESULT) {
            this.fromUnits.setText(data.getStringExtra("fromUnits"));
            this.toUnits.setText(data.getStringExtra("toUnits"));
        }else if (resultCode == HISTORY_RESULT) {
            String[] vals = data.getStringArrayExtra("item");
            this.fromField.setText(vals[0]);
            this.toField.setText(vals[1]);
            this.mode = Mode.valueOf(vals[2]);
            this.fromUnits.setText(vals[3]);
            this.toUnits.setText(vals[4]);
            this.title.setText(mode.toString() + " Converter");
        }

    }

    @Override
    public void onResume(){
        super.onResume();
        topRef = FirebaseDatabase.getInstance().getReference("history");
        topRef.addChildEventListener (chEvListener);
        IntentFilter weatherFilter = new IntentFilter(BROADCAST_WEATHER);
        LocalBroadcastManager.getInstance(this).registerReceiver(weatherReceiver, weatherFilter);
    }

    @Override
    public void onPause(){
        super.onPause();
        topRef.removeEventListener(chEvListener);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(weatherReceiver);
    }

    private ChildEventListener chEvListener = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            HistoryContent.HistoryItem entry =
                (HistoryContent.HistoryItem) dataSnapshot.getValue(HistoryContent.HistoryItem.class);
            entry._key = dataSnapshot.getKey();
            allHistory.add(entry);
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {
        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {
            HistoryContent.HistoryItem entry =
                (HistoryContent.HistoryItem) dataSnapshot.getValue(HistoryContent.HistoryItem.class);
            List<HistoryContent.HistoryItem> newHistory = new ArrayList<HistoryContent.HistoryItem>();
            for (HistoryContent.HistoryItem t : allHistory) {
                if (!t._key.equals(dataSnapshot.getKey())) {
                    newHistory.add(t);
                }
            }
            allHistory = newHistory;
        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }

    };

    private BroadcastReceiver weatherReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            double temp = bundle.getDouble("TEMPERATURE");
            String summary = bundle.getString("SUMMARY");
            String icon = bundle.getString("ICON").replaceAll("-", "_");
            String key = bundle.getString("KEY");
            int resID = getResources().getIdentifier(icon , "drawable", getPackageName());
            //setWeatherViews(View.VISIBLE);
            if (key.equals("p1"))  {
                current.setText(summary);
                temperature.setText(Double.toString(temp));
                weatherIcon.setImageResource(resID);
            }
        }
    };
}

package co.a_r_i_a.aria;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.*;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.mikepenz.google_material_typeface_library.*;
import com.mikepenz.materialdrawer.*;
import com.mikepenz.materialdrawer.model.*;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    public enum eWho {
        YOU ("You"),
        ARIA ("A.R.I.A.");

        private String name = "";

        eWho(String name){
            this.name = name;
        }

        public String toString(){
            return name;
        }
    }

    private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
    private EditText eText;
    private ArrayList<String> listItems = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private RequestQueue queue;
    private Speaker speaker;
    private ListView listView;
    private Drawer drawer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setNavigationBarColor(getResources().getColor(R.color.colorPrimary));
            getWindow().setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));
        }

        eText = (EditText) findViewById(R.id.editText);
        eText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE)
                    return sendText();
                return false;
            }
        });
        if (savedInstanceState != null) {
            listItems = (ArrayList<String>) savedInstanceState.getSerializable("listItems");
        }
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listItems);
        listView = (ListView) findViewById(R.id.listView);
        listView.setAdapter(adapter);
        AccountHeader headerResult = new AccountHeaderBuilder()
                .withActivity(this)
                .withHeaderBackground(R.drawable.bg_gradient)
                .addProfiles(
                        new ProfileDrawerItem().withName("John Doe").withEmail("john.doe@gmail.com").withIcon(getResources().getDrawable(R.drawable.johndoe))
                )
                .withOnAccountHeaderListener(new AccountHeader.OnAccountHeaderListener() {
                    @Override
                    public boolean onProfileChanged(View view, IProfile profile, boolean currentProfile) {
                        return false;
                    }
                })
                .build();
        drawer = new DrawerBuilder()
                .withActivity(this)
                .withAccountHeader(headerResult)
                .addDrawerItems(
                        new PrimaryDrawerItem().withName("Home").withIcon(GoogleMaterial.Icon.gmd_home),
                        new DividerDrawerItem(),
                        new SecondaryDrawerItem().withName("Settings").withIcon(GoogleMaterial.Icon.gmd_settings)
                )
                .build();
        queue = Volley.newRequestQueue(this);
        checkTTS();
    }

    public void openDrawer(View view) {
        drawer.openDrawer();
    }

    private Boolean sendText() {
        addTextToListItems(eWho.YOU, eText.getText().toString());
        sendTextPostRequest(eText.getText().toString());
        eText.setText("");
        return true;
    }

    private Boolean sendText(String text) {
        addTextToListItems(eWho.YOU, text);
        sendTextPostRequest(text);
        return true;
    }

    private void sendTextPostRequest(String query) {
        Map<String, String> postParam= new HashMap<>();
        postParam.put("query", query);

        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.POST, SharedProperty.API_URL, new JSONObject(postParam),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d("Volley", response.toString());
                        try {
                            addTextToListItems(eWho.ARIA, response.getString("answer"));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.d("Volley", "Error: " + error.getMessage());
                Toast.makeText(getApplicationContext(), error.toString(), Toast.LENGTH_SHORT).show();
                addTextToListItems(eWho.ARIA, getString(R.string.aria_error));
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put( "charset", "utf-8");
                headers.put( "X-Api-Key", SharedProperty.API_KEY);
                return headers;
            }
        };

        jsonObjReq.setRetryPolicy(new DefaultRetryPolicy(
                SharedProperty.VOLLEY_TIMEOUT,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        queue.add(jsonObjReq);
    }

    public void getAudio(View view) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, SharedProperty.LANGUAGE);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.speech_prompt));
        try {
            startActivityForResult(intent, SharedProperty.REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(), getString(R.string.speech_not_supported), Toast.LENGTH_SHORT).show();
        }
    }

    private void addTextToListItems(eWho who, String text) {
        listItems.add("[" + sdf.format(new Date()) + "] " + who.toString() + ": " + text);
        adapter.notifyDataSetChanged();
        if (who == eWho.ARIA)
            speaker.speak(text);
    }

    private void checkTTS(){
        Intent check = new Intent();
        check.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(check, SharedProperty.CHECK_CODE);
    }

    @Override
    protected void onSaveInstanceState(Bundle savedState) {
        super.onSaveInstanceState(savedState);

        savedState.putSerializable("listItems", listItems);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case SharedProperty.REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {
                    ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    sendText(result.get(0));
                }
                break;
            }
            case SharedProperty.CHECK_CODE: {
                if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS){
                    speaker = new Speaker(this);
                    speaker.allow(true);
                } else {
                    Intent install = new Intent();
                    install.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                    startActivity(install);
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        speaker.destroy();
    }
}

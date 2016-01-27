package co.a_r_i_a.aria;

import co.a_r_i_a.aria.other.Message;
import co.a_r_i_a.aria.other.SharedProperty;
import co.a_r_i_a.aria.other.Speaker;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.*;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.materialdrawer.*;
import com.mikepenz.materialdrawer.model.*;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;
import com.mikepenz.materialdrawer.util.AbstractDrawerImageLoader;
import com.mikepenz.materialdrawer.util.DrawerImageLoader;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AriaActivity extends Activity {

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
    private MessagesListAdapter adapter;
    private List<Message> listMessages;
    private ListView listViewMessages;
    private RequestQueue queue;
    private Speaker speaker;
    private Drawer drawer;
    private AccountHeader headerResult;

    private Boolean exit = false;

    private final String EXTRA_TOKEN = "fb_token";
    private String fbToken = null;

    private JSONObject userDatas = null;

    private void switchToSettingsActivity() {
        Intent intent = new Intent(AriaActivity.this, SettingsActivity.class);
        if (SharedProperty.user != null && SharedProperty.user.language != null)
            startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.aria_activity);

        Intent intent = getIntent();
        if (intent != null) {
            this.fbToken = intent.getStringExtra(EXTRA_TOKEN);
            Log.d("Facebook", this.fbToken);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setNavigationBarColor(getResources().getColor(R.color.colorPrimary));
            getWindow().setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));
        }

        this.eText = (EditText) findViewById(R.id.editText);
        this.eText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE)
                    return sendText();
                return false;
            }
        });
        this.listViewMessages = (ListView) findViewById(R.id.listViewMessages);
        this.listMessages = new ArrayList<Message>();
        this.adapter = new MessagesListAdapter(this, this.listMessages);
        this.listViewMessages.setAdapter(this.adapter);
        headerResult = new AccountHeaderBuilder()
                .withActivity(this)
                .withHeaderBackground(R.drawable.bg_gradient)
                .withSelectionListEnabledForSingleProfile(false)
                .withOnAccountHeaderListener(new AccountHeader.OnAccountHeaderListener() {
                    @Override
                    public boolean onProfileChanged(View view, IProfile profile, boolean currentProfile) {
                        return false;
                    }
                })
                .build();
        this.drawer = new DrawerBuilder()
                .withActivity(this)
                .withAccountHeader(headerResult)
                .addDrawerItems(
                        new PrimaryDrawerItem().withName("Home").withIcon(GoogleMaterial.Icon.gmd_home),
                        new DividerDrawerItem(),
                        new SecondaryDrawerItem().withName("Settings").withIcon(GoogleMaterial.Icon.gmd_settings)
                )
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        if (position == 3)
                            switchToSettingsActivity();
                        return false;
                    }
                })
                .build();
        this.queue = Volley.newRequestQueue(this);
        if (this.fbToken != null)
            authAPIPostRequest(this.fbToken);
    }

    private void serializeJsonUser() {
        try {
            JSONObject user = this.userDatas.getJSONObject("user");
            SharedProperty.user.name = user.getString("name");
            SharedProperty.user.email = user.getString("email");
            SharedProperty.user.image = user.getString("image");
            String language = user.getString("language");
            if (language.equalsIgnoreCase("English"))
                SharedProperty.user.language = Locale.UK;
            else if (language.equalsIgnoreCase("French"))
                SharedProperty.user.language = Locale.FRENCH;
            JSONArray apiKeys = user.getJSONArray("api_keys");
            for (int i = 0; i < apiKeys.length(); i++) {
                JSONObject apiKey = apiKeys.getJSONObject(i);
                if (apiKey.getString("device_type").equalsIgnoreCase("Android")) {
                    SharedProperty.user.apiKey = apiKey.getString("token");
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void customDrawer() {
        DrawerImageLoader.init(new AbstractDrawerImageLoader() {
            @Override
            public void set(ImageView imageView, Uri uri, Drawable placeholder) {
                Picasso.with(imageView.getContext()).load(uri).resize(200, 200).centerCrop().into(imageView);
            }

            @Override
            public void cancel(ImageView imageView) {
                Picasso.with(imageView.getContext()).cancelRequest(imageView);
            }

        });
        this.headerResult.addProfiles(new ProfileDrawerItem().withName(SharedProperty.user.name).withEmail(SharedProperty.user.email).withIcon(SharedProperty.user.image));
    }

    public void openDrawer(View view) {
        drawer.openDrawer();
    }

    private Boolean sendText() {
        addTextToListItems(eWho.YOU, eText.getText().toString());
        sendTextPostRequest(eText.getText().toString());
        eText.setText("");
        eText.clearFocus();
        //hide keyboard
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getApplicationWindowToken(), 0);
        }
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

        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.POST, SharedProperty.API_URL_REQUEST, new JSONObject(postParam),
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
                VolleyLog.e("Volley", "Error: " + error.getMessage());
                Toast.makeText(getApplicationContext(), error.toString(), Toast.LENGTH_SHORT).show();
                addTextToListItems(eWho.ARIA, getString(SharedProperty.user.language == Locale.FRENCH ? R.string.aria_error_fr : R.string.aria_error_en));
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put( "charset", "utf-8");
                headers.put( "X-Api-Key", SharedProperty.user.apiKey);
                return headers;
            }
        };

        jsonObjReq.setRetryPolicy(new DefaultRetryPolicy(
                SharedProperty.VOLLEY_TIMEOUT,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        queue.add(jsonObjReq);
    }

    private void authAPIPostRequest(String fb_token) {
        Map<String, String> postParam= new HashMap<>();
        postParam.put("fb_token", fb_token);

        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.POST, SharedProperty.API_URL_AUTH, new JSONObject(postParam),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d("Volley", response.toString());
                        userDatas = response;
                        serializeJsonUser();
                        customDrawer();
                        checkTTS();
                        eText.setHint(SharedProperty.user.language.getDisplayLanguage(Locale.ENGLISH).equalsIgnoreCase("French") ? R.string.aria_hint_fr : R.string.aria_hint_en);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.e("Volley", "Error: " + error.getMessage());
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put( "charset", "utf-8");
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
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, SharedProperty.user.language);
        intent.putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", new String[]{"en", "fr"});
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(SharedProperty.user != null
                && SharedProperty.user.language.getDisplayLanguage(Locale.ENGLISH).equalsIgnoreCase("French") ? R.string.speech_prompt_fr : R.string.speech_prompt_en));
        try {
            startActivityForResult(intent, SharedProperty.REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(), getString(R.string.speech_not_supported), Toast.LENGTH_SHORT).show();
        }
    }

    private void addTextToListItems(eWho who, String text) {
//        listItems.add("[" + sdf.format(new Date()) + "] " + who.toString() + ": " + text);
//        listItems.add(who.toString() + ": " + text);
//        adapter.notifyDataSetChanged();
        Message m = new Message(who.toString(), text, who == eWho.YOU ? true : false);
        appendMessage(m);
        if (who == eWho.ARIA)
            speaker.speak(text);
    }

    private void appendMessage(final Message m) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                listMessages.add(m);

                adapter.notifyDataSetChanged();
            }
        });
    }

    private void checkTTS() {
        Intent check = new Intent();
        check.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(check, SharedProperty.CHECK_CODE);
    }

    @Override
    protected void onSaveInstanceState(Bundle savedState) {
        super.onSaveInstanceState(savedState);

//        savedState.putSerializable("listItems", listItems);
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
    protected void onResume() {
        super.onResume();

        if (this.drawer != null) {
            this.drawer.setSelectionAtPosition(1);
            if (this.headerResult != null && SharedProperty.user.name != null && this.headerResult.getActiveProfile() != null
                    && !this.headerResult.getActiveProfile().getName().toString().equals(SharedProperty.user.name)) {
                this.headerResult.clear();
                this.headerResult.addProfiles(new ProfileDrawerItem().withName(SharedProperty.user.name).withEmail(SharedProperty.user.email).withIcon(SharedProperty.user.image));
            }
        }
        if (this.speaker != null && this.speaker.getLanguage() != null && this.speaker.getLanguage() != null
                && !this.speaker.getLanguage().getDisplayLanguage(Locale.ENGLISH).equalsIgnoreCase(SharedProperty.user.language.getDisplayLanguage(Locale.ENGLISH))) {
            checkTTS();
            this.eText.setHint(SharedProperty.user.language.getDisplayLanguage(Locale.ENGLISH).equalsIgnoreCase("French") ? R.string.aria_hint_fr : R.string.aria_hint_en);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speaker != null)
            speaker.destroy();
    }

    @Override
    public void onBackPressed() {
        if (exit) {
            Intent a = new Intent(Intent.ACTION_MAIN);
            a.addCategory(Intent.CATEGORY_HOME);
            a.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(a);
        } else {
            Toast.makeText(this, "Press Back again to Exit.", Toast.LENGTH_SHORT).show();
            exit = true;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    exit = false;
                }
            }, 3 * 1000);
        }

    }
}

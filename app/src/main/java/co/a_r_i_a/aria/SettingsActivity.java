package co.a_r_i_a.aria;

import android.app.Activity;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.SocketHandler;

import co.a_r_i_a.aria.other.SharedProperty;
import co.a_r_i_a.aria.other.User;

/**
 * Created by KOALA on 25/01/2016.
 */

public class SettingsActivity extends Activity implements AdapterView.OnItemSelectedListener {

    private Locale language;
    private Spinner languageSpinner;
    private EditText nameEditText;
    private ArrayAdapter<CharSequence> adapterLanguageSpinner;
    private RequestQueue queue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        this.nameEditText = (EditText) findViewById(R.id.nameEditText);

        this.languageSpinner = (Spinner) findViewById(R.id.languageSpinner);
        this.adapterLanguageSpinner = ArrayAdapter.createFromResource(this, R.array.languages_array, android.R.layout.simple_spinner_item);
        this.adapterLanguageSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.languageSpinner.setAdapter(this.adapterLanguageSpinner);
        this.languageSpinner.setOnItemSelectedListener(this);

        ImageView ariaImage = (ImageView) findViewById(R.id.imageView);
        this.greyScale(ariaImage);

        this.fillInfos();
        this.queue = Volley.newRequestQueue(this);
    }

    private void fillInfos() {
        nameEditText.setText(SharedProperty.user.name);
        if (SharedProperty.user.language.getDisplayLanguage(Locale.ENGLISH).equalsIgnoreCase("French")) {
            this.languageSpinner.setSelection(0);
        } else if (SharedProperty.user.language.getDisplayLanguage(Locale.ENGLISH).equalsIgnoreCase("English")) {
            this.languageSpinner.setSelection(1);
        }
    }

    public void save(View view) {
        SharedProperty.user.name = this.nameEditText.getText().toString();
        SharedProperty.user.language = this.language;
        this.updateProfilePostRequest(SharedProperty.user.language.getDisplayLanguage(Locale.ENGLISH), SharedProperty.user.name);
        finish();
    }

    private void updateProfilePostRequest(String language, String name) {
        Map<String, String> user = new HashMap<>();
        user.put("language", language);
        user.put("name", name);

        Map<String, Map<String, String>> postParam = new HashMap<>();
        postParam.put("user", user);

        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.POST, SharedProperty.API_URL_PROFILE, new JSONObject(postParam),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d("Volley", response.toString());
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
                headers.put("charset", "utf-8");
                headers.put("X-Api-Key", SharedProperty.user.apiKey);
                return headers;
            }
        };

        jsonObjReq.setRetryPolicy(new DefaultRetryPolicy(
                SharedProperty.VOLLEY_TIMEOUT,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        queue.add(jsonObjReq);
    }

    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        String itemSelected = parent.getItemAtPosition(pos).toString();

        if (Locale.FRENCH.getDisplayLanguage(Locale.ENGLISH).equalsIgnoreCase(itemSelected)) {
            this.language = Locale.FRENCH;
        } else if (Locale.ENGLISH.getDisplayLanguage(Locale.ENGLISH).equalsIgnoreCase(itemSelected)) {
            this.language = Locale.ENGLISH;
        }
        //Log.d("onItemSelected", userTmp.language.getDisplayName(Locale.ENGLISH));
    }

    public void onNothingSelected(AdapterView<?> parent) {
        // Another interface callback
    }

    private static void  greyScale(ImageView v)
    {
        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(0);  //0 means grayscale
        ColorMatrixColorFilter cf = new ColorMatrixColorFilter(matrix);
        v.setColorFilter(cf);
        v.setAlpha(128);   // 128 = 0.5
    }

}

package co.a_r_i_a.aria.other;

import android.content.Context;
import android.media.AudioManager;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.HashMap;
import java.util.Locale;

/**
 * Created by KOALA on 01/11/2015.
 */

public class Speaker implements TextToSpeech.OnInitListener {

    private TextToSpeech tts;

    private boolean ready = false;

    private boolean allowed = false;

    public Speaker(Context context){
        tts = new TextToSpeech(context, this);
    }

    public boolean isAllowed(){
        return allowed;
    }

    public void allow(boolean allowed){
        this.allowed = allowed;
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS){
            tts.setLanguage(SharedProperty.user.language);
            ready = true;
        } else {
            ready = false;
        }
    }

    public void speak(String text) {
        //Log.d("D", "ready:" + ready);
        //Log.d("D", "allowed:" + allowed);
        //Log.d("D", "text:" + text);
        if (ready && allowed) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    public Locale getLanguage() {
        if (ready && allowed)
            return this.tts.getLanguage();
        return null;
    }

    public void pause(int duration){
        tts.playSilence(duration, TextToSpeech.QUEUE_ADD, null);
    }

    public void destroy(){
        tts.shutdown();
    }
}
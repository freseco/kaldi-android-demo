// Copyright 2019 Alpha Cephei Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.kaldi.demo;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.FaceDetector;
import android.media.ToneGenerator;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONObject;
import org.kaldi.Assets;
import org.kaldi.KaldiRecognizer;
import org.kaldi.Model;
import org.kaldi.RecognitionListener;
import org.kaldi.SpeechRecognizer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.text.DecimalFormat;

import static java.lang.Math.round;

public class KaldiActivity extends Activity implements RecognitionListener {

    private static final String TAG = "Kaldi";

    static {
        System.loadLibrary("kaldi_jni");
    }

    static private final int STATE_START = 0;
    static private final int STATE_READY = 1;
    static private final int STATE_FILE = 2;
    static private final int STATE_MIC  = 3;



    /* Used to handle permission request */
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;

    private Model model;
    private SpeechRecognizer recognizer;
    TextView resultView;


    private String backupText;

//region Posicionamiento GPS
    private LocationManager locationManager;
    private LocationListener locationListener;
    private String altitud="";
    private String longitud="";
    DecimalFormat df=new DecimalFormat("###.####");
//endregion

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.main);

        //region Posición GPS
        locationManager=(LocationManager)getSystemService(LOCATION_SERVICE);
        locationListener=new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                altitud  =df.format(location.getAltitude());
                longitud =df.format(location.getLongitude());

                resultView.append("Alt: "+altitud+"   -   Long: "+longitud+"\n");
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {
                resultView.append("Provider: "+provider+"\n");
            }

            @Override
            public void onProviderDisabled(String provider) {
                Intent intent=new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        };
        //endregion


        backupText="";

        // Setup layout
        resultView = findViewById(R.id.result_text);
        setUiState(STATE_START);

//
//        findViewById(R.id.recognize_file).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                recognizeFile();
//            }
//        });
//
        findViewById(R.id.recognize_mic).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                recognizeMicrophone();
            }
        });

        // Check if user has given permission to record audio
        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
            return;
        }
//        // Recognizer initialization is a time-consuming and it involves IO,
//        // so we execute it in async task
        new SetupTask(this).execute();
        Log.i(TAG,"Setting kaldi.");



    }

    private static class SetupTask extends AsyncTask<Void, Void, Exception> {
        WeakReference<KaldiActivity> KaldiReference;

        SetupTask(KaldiActivity activity) {
            this.KaldiReference = new WeakReference<>(activity);
        }

        @Override
        protected Exception doInBackground(Void... params) {
            try {
                Assets assets = new Assets(KaldiReference.get());
                File assetDir = assets.syncAssets();
                Log.d("!!!!", assetDir.toString());
                KaldiReference.get().model = new Model(assetDir.toString() + "/model-android");
            } catch (IOException e) {
                return e;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Exception result) {
            if (result != null) {
                KaldiReference.get().setErrorState(String.format(KaldiReference.get().getString(R.string.failed), result));
            } else {
                KaldiReference.get().setUiState(STATE_READY);
                KaldiReference.get().recognizeMicrophone();
                Log.i(TAG,"Kaldi ready.");

            }
        }
    }

    private static class RecognizeTask extends AsyncTask<Void, Void, String> {
        WeakReference<KaldiActivity> KaldiReference;
        WeakReference<TextView> resultView;

        RecognizeTask(KaldiActivity activity, TextView resultView) {
            this.KaldiReference = new WeakReference<>(activity);
            this.resultView = new WeakReference<>(resultView);
        }

        @Override
        protected String doInBackground(Void... params) {
            KaldiRecognizer rec;
            long startTime = System.currentTimeMillis();
            StringBuilder result = new StringBuilder();
            try {
                rec = new KaldiRecognizer(KaldiReference.get().model, 16000.f);

                InputStream ais = KaldiReference.get().getAssets().open("10001-90210-01803.wav");
                if (ais.skip(44) != 44) {
                    return "";
                }
                byte[] b = new byte[4096];
                int nbytes;
                while ((nbytes = ais.read(b)) >= 0) {
                    if (rec.AcceptWaveform(b, nbytes)) {
                        result.append(rec.Result());
                    } else {
                        result.append(rec.PartialResult());
                    }
                }
                result.append(rec.FinalResult());
            } catch (IOException e) {
                return "";
            }
            return String.format(KaldiReference.get().getString(R.string.elapsed), result.toString(), (System.currentTimeMillis() - startTime));
        }

        @Override
        protected void onPostExecute(String result) {
            KaldiReference.get().setUiState(STATE_READY);
            resultView.get().append(result + "\n");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Recognizer initialization is a time-consuming and it involves IO,
                // so we execute it in async task
                new SetupTask(this).execute();

            } else {
                finish();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (recognizer != null) {
            recognizer.cancel();
            recognizer.shutdown();
        }
    }


    @Override
    public void onResult(String hypothesis) {

        try
        {
            JSONObject obj = new JSONObject(hypothesis);

            String texto= obj.getString("text");

        if(texto.length()>0) {

            String[] words = texto.split(" ");

            StringBuilder tx = new StringBuilder();

            for (String a : words) {
                tx.append(" " + textToNumber(a));
            }



            resultView.append("->" + tx + "\n");


            Log.i(TAG, "Result: " + tx);
        }


    }catch (Exception ex) {
            Log.e(TAG,ex.getMessage());
    }
    }

    @Override
    public void onPartialResult(String hypothesis) {

        try{

            JSONObject obj=new JSONObject(hypothesis);

            String texto=obj.getString("partial");

            if(!backupText.equals(texto)) {

                if (texto.length()>0)
                {
                    resultView.append(texto + "...\n");
                    comando(texto);
                }

                backupText=texto;
            }else {

                Log.i(TAG, "Result igual. ");
            }
        }
        catch (Exception ex){
            Log.e(TAG,ex.getMessage());
        }


        Log.i(TAG,"Partial: "+hypothesis);

    }

    @Override
    public void onError(Exception e) {
        setErrorState(e.getMessage());
        Log.e(TAG,"Error: "+e.getMessage());
    }


    public  boolean comando(String tx){

        boolean encontrado=true;

        switch (tx){
            case "detener aplicación":
            case "cerrar la aplicación":
            case "cerrar aplicación":finish(); System.exit(0); break;

            case "borrar última palabra":
            case "borrar texto":resultView.setText("");beep(); break;

            case "repetir texto":
            case "repetir números":
            case "repetir número":Vibrar(); break;

            case "mostrar ficha":beep(); break;
            case "borrar último":Vibrar(); break;

            case "posición geográfica":
            case "posición gps":Vibrar();
              try{
                  locationManager.requestLocationUpdates("gps",5000,0,locationListener);
              }
              catch (SecurityException ex){
                  Log.e(TAG,"Sin permiso para la localización");
              }
            break;

            case "detener posiciones geográficas":
                locationManager.removeUpdates(locationListener);
                break;

            case "escanear código":
            case "tomar foto":
            case "tomar fotos":
            case "grabar vídeo":beep(); break;//duracción

            case "parar de escuchar":recognizeMicrophone(); break;

            default:encontrado= false;
        }
return encontrado;
    }

    public  void beep(){
    ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC,ToneGenerator.MAX_VOLUME);
    toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP,200);
}

    public  void Vibrar(){
        // Get instance of Vibrator from current Context
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

// Vibrate for 400 milliseconds
        v.vibrate(400);
    }

    @Override
    public void onTimeout() {
        recognizer.cancel();
        recognizer = null;
        setUiState(STATE_READY);
    }

    public  void addtexto(String txt){
        resultView.append(txt + "\n");
    }

    public void setUiState(int state) {

        switch (state) {
            case STATE_START:
                resultView.setText(R.string.preparing);
//                findViewById(R.id.recognize_file).setEnabled(false);
                findViewById(R.id.recognize_mic).setEnabled(false);
                Log.i(TAG,"State: Start");
                break;
            case STATE_READY:
                resultView.setText(R.string.ready);
                ((Button) findViewById(R.id.recognize_mic)).setText(R.string.recognize_microphone);
//                findViewById(R.id.recognize_file).setEnabled(true);
                findViewById(R.id.recognize_mic).setEnabled(true);
                Log.i(TAG,"State: Ready");
                break;
            case STATE_FILE:
                resultView.append(getString(R.string.starting));
                findViewById(R.id.recognize_mic).setEnabled(false);
//                findViewById(R.id.recognize_file).setEnabled(false);
                Log.i(TAG,"State: File");
                break;
            case STATE_MIC:
                ((Button) findViewById(R.id.recognize_mic)).setText(R.string.stop_microphone);
//                findViewById(R.id.recognize_file).setEnabled(false);
                findViewById(R.id.recognize_mic).setEnabled(true);
                Log.i(TAG,"State: MIC");
                break;
        }
    }

    private void setErrorState(String message) {
        resultView.setText(message);
        ((Button) findViewById(R.id.recognize_mic)).setText(R.string.recognize_microphone);
        findViewById(R.id.recognize_file).setEnabled(false);
        findViewById(R.id.recognize_mic).setEnabled(false);
    }

    public void recognizeFile() {
        Log.i(TAG,"File");
        setUiState(STATE_FILE);
        new RecognizeTask(this, resultView).execute();
    }

    public void recognizeMicrophone() {

        Log.i(TAG,"Microphone.");

//
        if (recognizer != null) {
            setUiState(STATE_READY);
            recognizer.cancel();
            recognizer = null;
        } else {
            setUiState(STATE_MIC);
            try {
                recognizer = new SpeechRecognizer(model);
                recognizer.addListener(this);
                recognizer.startListening();
            } catch (IOException e) {
                setErrorState(e.getMessage());
            }
        }
    }

    /*
     * Convierte el número expresado en texto a número expresado en decimal.
     *
     * ej: uno -> 1
     *
     * */
    public String textToNumber(String numbertext){

        String numero=numbertext;

        switch (numbertext){
            case "uno"      :numero="1";break;
            case "dos"      :numero="2";break;
            case "tres"     :numero="3";break;
            case "cuatro"   :numero="4";break;
            case "cinco"    :numero="5";break;
            case "seis"     :numero="6";break;
            case "siete"    :numero="7";break;
            case "ocho"     :numero="8";break;
            case "nueve"    :numero="9";break;
            case "cero"     :numero="0";break;
            case "diez"     :numero="10";break;
            case "once"     :numero="11";break;
            case "doce"     :numero="12";break;
            case "trece"    :numero="13";break;
            case "catorce"  :numero="14";break;
            case "quince"   :numero="15";break;

//            case "veinte"   :numero="20";break;
//            case "treinta"  :numero="30";break;
//            case "cuarenta" :numero="40";break;
//            case "cincuenta":numero="50";break;
//            case "sesenta"  :numero="60";break;
//            case "setenta"  :numero="70";break;
//            case "ochenta"  :numero="80";break;
//            case "noventa"  :numero="90";break;
//            case "cien"     :numero="100";break;
        }

        return numero;
    }


}

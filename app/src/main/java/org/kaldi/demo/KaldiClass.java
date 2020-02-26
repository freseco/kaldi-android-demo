package org.kaldi.demo;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;
import org.kaldi.Assets;
import org.kaldi.Model;
import org.kaldi.RecognitionListener;
import org.kaldi.SpeechRecognizer;

import java.io.File;
import java.io.IOException;

public class KaldiClass implements RecognitionListener {

    private static final String TAG = "Kaldi";

    static private final int STATE_START = 0;
    static private final int STATE_READY = 1;
    static private final int STATE_FILE = 2;
    static private final int STATE_MIC  = 3;


    private static Context context;

    static {
        System.loadLibrary("kaldi_jni");
    }

    public boolean escuchando=false;

    private Model model;
    private SpeechRecognizer recognizer;

    public String assetpath="";

    private static KaldiClass Instance = null;



    private KaldiClass(Context contxt) {


        context=contxt;


        // Recognizer initialization is a time-consuming and it involves IO,
        // so we execute it in async task
        new Thread(new Runnable(){
            public void run() {

                try {
                Assets assets = new Assets(context);
                File assetDir = assets.syncAssets();

                assetpath=assetDir.toString();

                Log.d("!!!!", assetDir.toString());

                model=new Model(assetpath+ "/model-android");



            }catch (Exception ex)
            {
                Log.e(TAG,ex.getMessage());
            }

            }
        }).start();


        Log.i(TAG,"Setting kaldi.");

    }

    public static KaldiClass getInstance(Context contxt) {
        if (Instance == null) {
            Instance = new KaldiClass(contxt);
        }else {
            context = contxt;
        }
        return(Instance);
    }


    @Override
    public void onPartialResult(String hypothesis) {
        try{

            JSONObject obj=new JSONObject(hypothesis);

            String texto=obj.getString("partial");

            ((KaldiActivity)context).addtexto(texto);

//            if (texto.length()==0)
//            {
//                resultView.append(".\n");
//            }
//            else {
//                resultView.append(texto + "\n");
//            }
        }
        catch (Exception ex){
            Log.e(TAG,ex.getMessage());
        }


        Log.i(TAG,"Partial: "+hypothesis);
    }

    @Override
    public void onError(Exception e) {
        Log.e(TAG,"Error: "+e.getMessage());
        escuchando=false;
    }

    @Override
    public void onResult(String hypothesis) {
        try
        {
            JSONObject obj = new JSONObject(hypothesis);

            String texto= obj.getString("text");

            String[] words=texto.split(" ");

            StringBuilder tx=new StringBuilder();



            for(String a:words){
                tx.append(" "+textToNumber(a));
            }

            ((KaldiActivity)context).addtexto(tx.toString());

//            resultView.append(tx + "\n");
            Log.i(TAG, "Result: " + tx);

        }catch (Exception ex) {
            Log.e(TAG,ex.getMessage());
        }
    }

    @Override
    public void onTimeout() {
        recognizer.cancel();
        recognizer = null;
        escuchando=false;
    }

    public void empezaEscuchar(){
        try {
            if (recognizer == null) {
                recognizer = new SpeechRecognizer(model);
                recognizer.addListener(this);
                recognizer.startListening();
                escuchando=true;
            }
        } catch (IOException e) {
           // setErrorState(e.getMessage());
            Log.e(TAG,"Error: "+e.getMessage());
            escuchando=false;
        }
    }

    public  void terminarEscuchar(){
        if (recognizer != null) {
        recognizer.cancel();
        recognizer = null;
        escuchando=false;
        }
    }

    public void onDestroy() {

        if (recognizer != null) {
            recognizer.cancel();
            recognizer.shutdown();
            escuchando=false;
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

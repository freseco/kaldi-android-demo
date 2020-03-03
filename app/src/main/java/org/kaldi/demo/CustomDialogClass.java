package org.kaldi.demo;


import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.view.Window;

/*
* Muestra un dialogo durante un tiempo determinado
*
* https://stackoverflow.com/questions/13341560/how-to-create-a-custom-dialog-box-in-android
* */
public class CustomDialogClass {


    public CustomDialogClass(Activity a,int segundos) {
        final Dialog dialog = new Dialog(a);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.custom_dialog);

        //hacemos la ventana transparente para que se sean los bordes transparentes.
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        dialog.show();


        Handler handler = new Handler();

        handler.postDelayed(new Runnable() {
            public void run() {
               dialog.dismiss();
            }
        }, segundos*1000);
    }




}
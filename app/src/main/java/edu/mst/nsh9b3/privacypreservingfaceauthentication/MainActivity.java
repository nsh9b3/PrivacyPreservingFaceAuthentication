package edu.mst.nsh9b3.privacypreservingfaceauthentication;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.util.UUID;

public class MainActivity extends Activity
{
    private static final String TAG = "PPFA::MainActivity";

    private ImageView faceImage;
    private Mat faceMat;
    private Bitmap faceBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set a default Image for the ImageView
        faceImage = (ImageView) findViewById(R.id.PPFA_Main_ImageView_Face);
        faceImage.setImageResource(R.drawable.matrix);

        // Set the Mat that contains the face to null initially
        faceMat = null;

        // Set arrays in Options class so if a person does NOT go to the Options screen, the app won't crash for null pointers
        Options.servers = getResources().getStringArray(R.array.PPFA_SharedPref_Servers);
        Options.serverInfo = getResources().getStringArray(R.array.PPFA_SharedPref_ServerInfo);
    }

    // Take the user to the Options Activity
    public void onClickOptions(View view)
    {
        Intent intent = new Intent(this, Options.class);
        startActivity(intent);
    }

    // Take the user to the Camera Activity
    public void onClickTakePicture(View view)
    {
        Intent intent = new Intent(this, TakePicture.class);
        startActivityForResult(intent, 1);
    }

    // Run a service in the background to create the histogram and send the encrypted information to the servers
    public void onClickAuthenticate(View view)
    {
        if(faceMat != null)
        {
//            Intent intent = new Intent(this, AuthenticateFace.class);

            Intent intent = new Intent(this, Authenticate.class);

            intent.putExtra("faceMat", faceMat.getNativeObjAddr());
            startService(intent);
        }
        else
        {
            Toast.makeText(this, "Take a picture first", Toast.LENGTH_LONG).show();
        }
    }

    // This is called when certain activities are returned from
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if(resultCode == RESULT_OK)
        {
            Bundle extras = data.getExtras();
            String from = extras.getString("class");
            // If this intent came from the TakePicture class
            if(from != null && from.equals("TakePicture"))
            {
                // Get the raw data for the matrix which represents the user's face
                long croppedLong = extras.getLong("savedMat");
                if(croppedLong != -1l)
                {
                    // Set that matrix to the ImageView
                    faceMat = new Mat(croppedLong);
                    faceBitmap = Bitmap.createBitmap(faceMat.width(), faceMat.height(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(faceMat, faceBitmap);
                    faceImage.setImageBitmap(faceBitmap);

//                    MyLBP lbp = new MyLBP(croppedLong);
                }
            }
        }
    }

    // Reset the picture if the screen's orientation changes
    // This won't happen for now since the orientation is locked
    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);

        // If there is a picture already saved
        if(faceMat != null)
        {
            faceImage.setImageBitmap(faceBitmap);
        }
        // Otherwise draw the default picture
        else
        {
            faceImage = (ImageView) findViewById(R.id.PPFA_Main_ImageView_Face);
            faceImage.setImageResource(R.drawable.matrix);
        }
    }
}

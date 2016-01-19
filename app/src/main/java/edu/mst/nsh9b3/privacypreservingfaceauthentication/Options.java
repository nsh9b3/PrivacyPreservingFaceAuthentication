package edu.mst.nsh9b3.privacypreservingfaceauthentication;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;;
import android.util.Log;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.NumberPicker;

import java.util.ArrayList;

public class Options extends Activity implements NumberPicker.OnValueChangeListener
{
    private static String TAG = "PPFA::Options";

    public static SharedPreferences sharedPreferences;
    public static SharedPreferences.Editor sharedPreferencesEditor;

    private NumberPicker numberPicker;
    private final int MAXNUMBER = 10;
    private final int MINNUMBER = 1;

    public static  String[] servers;
    public static String[] serverInfo;

    public static final String SHARED_PREF = "sharedPreferences";

    ArrayList<ParentServerInfo> parents;

    private ExpandableListView expListView;
    private ServerExpandableListAdapter expListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_options);
        setupSharedPref();
        setupNumberPicker();
        setupAdapter();
    }

    @Override
    public void onValueChange(NumberPicker picker, int oldVal, int newVal)
    {
        if(newVal != oldVal)
        {
            expListAdapter = new ServerExpandableListAdapter(this, parents.subList(0, newVal));

            expListView.setAdapter(expListAdapter);

            sharedPreferencesEditor.putInt(getString(R.string.PPFA_SharedPref_NumServers), newVal);
            sharedPreferencesEditor.apply();
        }
    }

    private void setupNumberPicker()
    {
        numberPicker = (NumberPicker) findViewById(R.id.PPFA_Options_NumberPicker);
        numberPicker.setMinValue(MINNUMBER);
        numberPicker.setMaxValue(MAXNUMBER);
        numberPicker.setValue(sharedPreferences.getInt(getString(R.string.PPFA_SharedPref_NumServers), 1));
        numberPicker.setWrapSelectorWheel(false);

        numberPicker.setOnValueChangedListener(this);
    }

    private void setupAdapter()
    {
        expListView = (ExpandableListView)findViewById(R.id.PPFA_Options_ExpandableListView);

        parents = new ArrayList<>();
        for(int i = 0; i < servers.length; i++)
        {
            ParentServerInfo parent = new ParentServerInfo(sharedPreferences.getString(servers[i], ""));
            parent.setFtpAddress(sharedPreferences.getString(servers[i] + serverInfo[0], ""));
            parent.setFtpPort(sharedPreferences.getString(servers[i] + serverInfo[1], ""));
            parent.setFtpUsername(sharedPreferences.getString(servers[i] + serverInfo[2], ""));
            parent.setFtpPassword(sharedPreferences.getString(servers[i] + serverInfo[3], ""));
            parents.add(parent);
        }

        expListAdapter = new ServerExpandableListAdapter(this, parents.subList(0, sharedPreferences.getInt(getString(R.string.PPFA_SharedPref_NumServers), 1)));

        expListView.setAdapter(expListAdapter);
    }

    private void setupSharedPref()
    {
        sharedPreferences = this.getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        sharedPreferencesEditor = sharedPreferences.edit();

        servers = getResources().getStringArray(R.array.PPFA_SharedPref_Servers);
        serverInfo = getResources().getStringArray(R.array.PPFA_SharedPref_ServerInfo);

        int num = sharedPreferences.getInt(getString(R.string.PPFA_SharedPref_NumServers), 0);
        if(num == 0)
        {
            sharedPreferencesEditor.putInt(getString(R.string.PPFA_SharedPref_NumServers), 1);
        }

        for(int i = 0; i < servers.length; i++)
        {
            String prefix = servers[i];
            String temp = sharedPreferences.getString(servers[i], "null");

            if(temp.equals("null"))
            {
                sharedPreferencesEditor.putString(prefix, prefix);
            }

            for(int k = 0; k < serverInfo.length; k++)
            {
                String info = servers[i] + serverInfo[k];
                temp = sharedPreferences.getString(info, "");

//                if(temp.equals("null"))
//                {
//                    sharedPreferencesEditor.putString(info, info);
//                }
            }
        }

        sharedPreferencesEditor.apply();
    }

    public static void setSharedPreferences(ParentServerInfo parent, String ftpAddress, String ftpPort, String ftpUsername, String ftpPassword)
    {
        parent.setFtpAddress(ftpAddress);
        parent.setFtpPort(ftpPort);
        parent.setFtpUsername(ftpUsername);
        parent.setFtpPassword(ftpPassword);

        sharedPreferencesEditor.putString(parent.getServerName() + serverInfo[0], parent.getFtpAddress());
        sharedPreferencesEditor.putString(parent.getServerName() + serverInfo[1], parent.getFtpPort());
        sharedPreferencesEditor.putString(parent.getServerName() + serverInfo[2], parent.getFtpUsername());
        sharedPreferencesEditor.putString(parent.getServerName() + serverInfo[3], parent.getFtpPassword());

        sharedPreferencesEditor.apply();
    }

    public void onClickClearSettings(View view)
    {
        sharedPreferencesEditor.clear();
        sharedPreferencesEditor.apply();

        setupSharedPref();
        setupNumberPicker();
        setupAdapter();
    }

}

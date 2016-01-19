package edu.mst.nsh9b3.privacypreservingfaceauthentication;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.telephony.TelephonyManager;
import android.util.Log;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.opencv.core.Mat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.UUID;

/**
 * Created by nick on 1/8/16.
 */
public class Authenticate extends IntentService
{
    private final String TAG = "PPBA::Authenticate";

    private FTPClient[] ftpClients;
    private SharedPreferences sharedPreferences;
    private HashMap<Integer, String[]> servers;

    public Authenticate()
    {
        super("Authenticate");
    }
    @Override
    protected void onHandleIntent(Intent intent)
    {
        Log.d(TAG, "onHandleIntent");

        // Get the Mat location from the intent
        long croppedLong = intent.getLongExtra("faceMat", -1l);

        // And then create a new clone from the one sent in to avoid null pointer exceptions
        Mat croppedFace = new Mat(croppedLong).clone();

        // Get the sharedPreferences
        sharedPreferences = this.getSharedPreferences(Options.SHARED_PREF, MODE_PRIVATE);

        // Get the number of servers being used currently
        int numServers = sharedPreferences.getInt(getString(R.string.PPFA_SharedPref_NumServers), 0);

        // Hashmap used for each server's information
        servers = new HashMap<>();

        // Array used for each different ftpclient (for each different server)
        ftpClients = new FTPClient[numServers];

        // For each server grab the informatino from the sharedPreferences and create a new ftpclient
        for (int i = 0; i < numServers; i++)
        {
            servers.put(i, new String[]{
                    sharedPreferences.getString(Options.servers[i] + Options.serverInfo[0], "error"),
                    sharedPreferences.getString(Options.servers[i] + Options.serverInfo[1], "error"),
                    sharedPreferences.getString(Options.servers[i] + Options.serverInfo[2], "error"),
                    sharedPreferences.getString(Options.servers[i] + Options.serverInfo[3], "error")});
            ftpClients[i] = new FTPClient();

            if (servers.get(i)[2].equals("") || servers.get(i)[2].equals("error"))
            {
                servers.get(i)[2] = "anonymous";
            }
        }


        // Create lbp
        Log.d(TAG, "Creating LBP");
        MyLBP lbp = new MyLBP(croppedFace.getNativeObjAddr());

        // Get the histogram
        HashMap<Integer, Integer> histogram = lbp.getHistogram();

        // Get the values from the histogram into a String array
        String[] histogram_array = convertHistToStringArray(histogram);

        // Get the unique timestampedID
        String[] timestampedID = getTimestampedID();

        // Combine histogram with timestamped Array
        String[] timesstampedHistogram = new String[histogram_array.length + timestampedID.length];
        System.arraycopy(histogram_array, 0, timesstampedHistogram, 0, histogram_array.length);
        System.arraycopy(timestampedID, 0, timesstampedHistogram, histogram_array.length, timestampedID.length);

        // Only connecting to 1 server for now
        Log.d(TAG, "Connecting to Server");
        if (connectToServer(ftpClients[0], servers.get(0)))
        {
            // Get public key
            Log.d(TAG, "Getting Public Key");
            String[] publicKey = getPublicKey(ftpClients[0]);

            // Encrypt histogram
            Log.d(TAG, "Encrypting histogram");
            String encryptedFilename = null;
            try
            {
                encryptedFilename = encryptHistogram(publicKey, timesstampedHistogram);
                Log.d(TAG, "Done encrypting");

                // Send the file to the server for further processing
                Log.d(TAG, "Transferring file to Server");
                if (sendFileToServer(ftpClients[0], encryptedFilename))
                {

                }
            } catch (Exception e)
            {
                Log.e(TAG, "Error: " + e);
            }
            ftpDisconnect(ftpClients[0]);
        }

    }

    /***
     * Disconnects from the FTP Server
     * @param ftpClient information regarding the connected server
     */
    private void ftpDisconnect(FTPClient ftpClient)
    {
        try
        {
            // Logout and then disconnect
            ftpClient.logout();
            ftpClient.disconnect();
        } catch (Exception e)
        {
            Log.e(TAG, "Error: " + e);
        }
    }

    /**
     * Send a file from the Android phone to the FTP server
     * @param ftpClient information regarding the connected server
     * @param filename name of the file to be sent
     * @return boolean whether the file was successfully sent
     */
    private boolean sendFileToServer(FTPClient ftpClient, String filename)
    {
        try
        {
            InputStream input = new FileInputStream(filename);
            String name = filename.split("/")[filename.split("/").length - 1];
            name = "/ftp/".concat(name);
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            ftpClient.setFileTransferMode(FTP.STREAM_TRANSFER_MODE);
            ftpClient.storeFile(name, input);

            int reply = ftpClient.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply))
            {
                Log.e(TAG, "Apache Error - Reply Code: " + reply);

                ftpClient.logout();
                ftpClient.disconnect();

                return false;
            } else
            {
                Log.d(TAG, "Successfully transferred file");

                input.close();

                return true;
            }
        } catch (Exception e)
        {
            Log.e(TAG, "Error: " + e);

            return false;
        }
    }

    /**
     * Encrypts the unique identifier and the histogram
     * @param publicKey  String[] containing the public key for Paillier encryption.
     *                   Index 0 is n, index 1 is g, index 2 is the bitlength
     * @param histogram String[] containing the histogram and unique ID
     * @return String for the name of the created encrypted file
     * @throws Exception
     */
    private String encryptHistogram(String[] publicKey, String[] histogram) throws Exception
    {
        // Create a cryptosystem for encryption
        PaillierEncryption paillerCryptosystem =  new PaillierEncryption(publicKey[0], publicKey[1], publicKey[2]);

        // m = message, c = ciphertext
        BigInteger[] m = new BigInteger[histogram.length];
        BigInteger[] c = new BigInteger[histogram.length];

        // Encrypt each value in the histogram
        for (int i = 0; i < histogram.length; i++)
        {
            m[i] = new BigInteger(histogram[i]);
            c[i] = paillerCryptosystem.Encryption(m[i]);
        }

        // Write the ciphertext to a File
        File outputDir = this.getExternalCacheDir(); // context being the Activity pointer
        File outputFile = null;
        try
        {
            outputFile = File.createTempFile("encryptedHistogram", ".txt", outputDir);
        } catch (Exception e)
        {
            Log.e(TAG, "Error: " + e);

            return null;
        }

        BufferedWriter writer = null;
        try
        {
            writer = new BufferedWriter(new FileWriter(outputFile.getAbsoluteFile()));
            for (int i = 0; i < c.length; i++)
            {
                writer.write(c[i] + " ");
            }
        } catch (Exception e)
        {
            Log.e(TAG, "Error: " + e);
            return null;
        } finally
        {
            try
            {
                if (writer != null)
                {
                    writer.close();
                }
            } catch (IOException e)
            {
                Log.e(TAG, "Error: " + e);
            }
        }
        return outputFile.getAbsolutePath();
    }

    private String[] getTimestampedID()
    {
        String[] timestampedID = new String[2];

        // Get unique ID
        final TelephonyManager tm = (TelephonyManager) getBaseContext().getSystemService(Context.TELEPHONY_SERVICE);

        final String tmDevice, tmSerial, androidId;
        tmDevice = "" + tm.getDeviceId();
        tmSerial = "" + tm.getSimSerialNumber();
        androidId = "" + android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);

        UUID deviceUuid = new UUID(androidId.hashCode(), ((long)tmDevice.hashCode() << 32) | tmSerial.hashCode());
        String deviceId = deviceUuid.toString();

        // Get timestamp
        String timestamp = "" + System.nanoTime();

        timestampedID[0] = deviceId;
        timestampedID[1] = timestamp;

        return timestampedID;
    }

    private String[] convertHistToStringArray(HashMap<Integer, Integer> hist)
    {
        String[] histArray = new String[256];

        for(int i = 0; i < 256; i++)
        {
            Integer value = hist.get(i);
            if(value != null)
                histArray[i] = value.toString();
            else
                histArray[i] = "0";
        }

        return histArray;
    }

    private boolean connectToServer(FTPClient ftpClient, String[] serverInfo)
    {
        try
        {
            Log.d(TAG, serverInfo[0] + "\t" + serverInfo[1]);
            ftpClient.connect(serverInfo[0], Integer.parseInt(serverInfo[1]));

            if (!ftpClient.login(serverInfo[2], serverInfo[3]))
            {
                Log.e(TAG, "Unable to log in");

                ftpClient.logout();
                ftpClient.disconnect();

                return false;
            } else
            {
                int reply = ftpClient.getReplyCode();
                if (!FTPReply.isPositiveCompletion(reply))
                {
                    Log.e(TAG, "Apache Error - Reply Code: " + reply);

                    ftpClient.logout();
                    ftpClient.disconnect();

                    return false;
                } else
                {
                    Log.d(TAG, "Successfully connected + logged into server");

                    return true;
                }
            }
        } catch (Exception e)
        {
            Log.e(TAG, "Error: " + e);

            return false;
        }
    }

    private String[] getPublicKey(FTPClient ftpClient)
    {
        String[] publicKey = null;

        try
        {
            OutputStream output = new FileOutputStream(this.getExternalCacheDir() + "/public_key.txt");
            ftpClient.retrieveFile("/ftp/public_key.txt", output);
            int reply = ftpClient.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply))
            {
                Log.e(TAG, "Apache Error - Reply Code: " + reply);

                ftpClient.logout();
                ftpClient.disconnect();

                return null;
            } else
            {
                publicKey = readValuesInFile(this.getExternalCacheDir() + "/public_key.txt");
            }
        } catch (Exception e)
        {
            Log.e(TAG, "Error: " + e);
        }

        return publicKey;
    }

    private String[] readValuesInFile(String filename)
    {
        String[] tokens = null;
        String line = null;

        BufferedReader reader;
        try
        {
            reader = new BufferedReader(new FileReader(filename));

            line = reader.readLine();
            tokens = line.split(" ");
        } catch (Exception e)
        {

        }

        return tokens;
    }
}

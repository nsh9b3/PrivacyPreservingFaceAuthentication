package edu.mst.nsh9b3.privacypreservingfaceauthentication;

import android.app.IntentService;
import android.app.VoiceInteractor;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.imgproc.Imgproc;

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
import java.util.Arrays;
import java.util.HashMap;


public class AuthenticateFace extends IntentService
{
    private static final String TAG = "PPFA::AuthenticateFace";

    private FTPClient[] ftpClients;
    private SharedPreferences sharedPreferences;
    private HashMap<Integer, String[]> servers;
    private final int HISTOGRAMSIZE = 25;

    public AuthenticateFace()
    {
        super("AuthenticateFace");
    }

    @Override
    protected void onHandleIntent(Intent intent)
    {
        // Get the Mat location from the intent
        long croppedLong = intent.getLongExtra("faceMat", -1l);

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


        LBP lbp = new LBP(croppedLong, 1, 4, 8, 8, Double.MAX_VALUE);
        Mat hist = lbp.getHistogram();
        String filename = writeMatToTempFile(hist);

        for (int i = 0; i < numServers; i++)
        {
            if (connectToServer(ftpClients[i], servers.get(i)))
            {
                String[] publicKey = getPublicKey(ftpClients[i]);
                Log.d(TAG, "Done getting key");
                String[] tokens = readValuesInFile(filename);
                String encryptedFilename = null;
                if(publicKey != null)
                {
                    try
                    {
                        encryptedFilename = encryptHistogramSegment(publicKey, tokens);
                        Log.d(TAG, "Done encrypting");
                    } catch (Exception e)
                    {
                        Log.e(TAG, "Error: " + e);
                    }
                    if(sendFileToServer(ftpClients[i], filename))
                    {

                    }
                    if (sendFileToServer(ftpClients[i], encryptedFilename))
                    {

                    }
                    ftpDisconnect(ftpClients[i]);
                }
            }
        }

//        // Create a histogram based off the Mat given through the intent
//        Mat hist = createHistogram(croppedLong);
//        String filename = writeMatToTempFile(hist);
//
//        //TODO: Breakup histogram here
//
//        for (int i = 0; i < numServers; i++)
//        {
//            if (connectToServer(ftpClients[i], servers.get(i)))
//            {
//                String[] publicKey = getPublicKey(ftpClients[i]);
//                String[] tokens = readValuesInFile(filename);
//                String encryptedFilename = null;
//                if(publicKey != null)
//                {
////                    try
////                    {
////                        encryptedFilename = encryptHistogramSegment(publicKey, tokens);
////                    } catch (Exception e)
////                    {
////                        Log.e(TAG, "Error: " + e);
////                    }
////                    if(sendFileToServer(ftpClients[i], filename))
////                    {
////
////                    }
////                    if (sendFileToServer(ftpClients[i], encryptedFilename))
////                    {
////
////                    }
//                    ftpDisconnect(ftpClients[i]);
//                }
//            }
//        }
    }

    private Mat createHistogram(long longFace)
    {
        Mat src = new Mat(longFace);
        Mat dst = new Mat(src.rows() - 2 * 1, src.cols() - 2 * 1, CvType.CV_32SC1);
        nativeCreateLBPHistorgram(src.getNativeObjAddr(), dst.getNativeObjAddr(), 1, 8);

//        for (int i = 0; i < dst.rows(); i++)
//        {
//            String line = i + "\t";
//            for (int j = 0; j < dst.cols(); j++)
//            {
//                line.concat(dst.get(i, j)[0] + "\t");
//            }
//            Log.d(TAG, line);
//        }

        return dst;
//
//        Mat result = Mat.zeros(8 * 8, (int) Math.pow(2.0, 8.0), CvType.CV_32FC1);
//
//        //TODO: Fix this JNI function
////        nativeSpatialHistogram(dst.getNativeObjAddr(), result.getNativeObjAddr(), (int) Math.pow(2.0, 8.0), 8, 8);
//
//        //This returns 64 values all zero
////        for(int i = 0; i < result.rows(); i++)
////        {
////            double[] data2 = result.get(i, 0);
////            for(int k = 0; k < data2.length; k++)
////            {
////                String log = "result:\t" + data2[k];
////                Log.d(TAG, log);
////            }
////        }
//
//        MatOfInt channels = new MatOfInt(0);
//        Mat hist = new Mat();
//        MatOfInt mHistSize = new MatOfInt(HISTOGRAMSIZE);
//        MatOfFloat mRanges = new MatOfFloat(0f, 255f);
//
//        Imgproc.calcHist(Arrays.asList(dst), channels, new Mat(), result, mHistSize, mRanges);
//
//        for (int i = 0; i < result.rows(); i++)
//        {
//            for (int k = 0; k < result.cols(); k++)
//            {
//                double[] test = result.get(i, k);
//                for(int l = 0; l < test.length; l++)
//                {
//                    String log = "hist_data:\t" + test[l];
//                    Log.d(TAG, log);
//                }
//            }
//        }

//        return dst;

////        Mat tmp = new Mat(longFace);
//        MatOfInt channels = new MatOfInt(0);
//        Mat hist = new Mat();
//        MatOfInt mHistSize = new MatOfInt(HISTOGRAMSIZE);
//        MatOfFloat mRanges = new MatOfFloat(0f, 255f);
//
//        for(int i = 0; i < dst.rows(); i++)
//        {
//            double[] data2 = dst.get(i, 0);
//            for(int k = 0; k < data2.length; k++)
//            {
//                String log = "dst:\t" + data2[k];
//                Log.d(TAG, log);
//            }
//        }
//
//        Imgproc.calcHist(Arrays.asList(result), channels, new Mat(), hist, mHistSize, mRanges);
//
//        for(int i = 0; i < hist.rows(); i++)
//        {
//            double[] data2 = hist.get(i, 0);
//            for(int k = 0; k < data2.length; k++)
//            {
//                String log = "hist:\t" + data2[k];
//                Log.d(TAG, log);
//            }
//        }

//        return dst;

//        for(int i = 0; i < result.rows(); i++)
//        {
//            double[] data2 = result.get(i, 0);
//            for(int k = 0; k < data2.length; k++)
//            {
//                String log = "data:\t" + data2[k];
//                Log.d(TAG, log);
//            }
//        }

//        Mat tmp = new Mat(longFace);
//        MatOfInt channels = new MatOfInt(0);
//        Mat hist = new Mat();
//        MatOfInt mHistSize = new MatOfInt(HISTOGRAMSIZE);
//        MatOfFloat mRanges = new MatOfFloat(0f, 255f);
//
//        Imgproc.calcHist(Arrays.asList(tmp), channels, new Mat(), hist, mHistSize, mRanges);
//
//        return result;
//        return null;
    }

    private String[] createHistogramSegments(String[] tokens, int start, int end)
    {
        String[] tokenSegments = null;


        return tokenSegments;
    }

    private String encryptHistogramSegment(String[] publicKey, String[] tokens) throws Exception
    {
//        PaillierEncryption paillerCryptosystem = new PaillierEncryption(2048, 64);
//        paillerCryptosystem.publicKey();
//        paillerCryptosystem.privateKey();

        PaillierEncryption paillerCryptosystem = new PaillierEncryption(publicKey[0], publicKey[1], publicKey[2]);
//        PaillierEncryption paillerCryptosystem = new PaillierEncryption(publicKey[0], publicKey[1], publicKey[2],
//                "14197434053761188776213925246618324956576141364455755786743734537082582698123645000892509381098875964783602799750727162581470714108282797265441515651813658909680454276130502731726753318679564961460104769699214613825014717133856797579233521884721914631024956120020932156094302448887611568721871131586939076395961977556124523996129963507859817492250633413926809989100945576487364399066691345318787514113862923780353144999739201310381097202460031647442663497220759964995021308682197585333088346588919935470854661240136935559612076410672271443461938960197897951276601721330079338386923091366483382297567392727102632681984",
//                "28160004706332241270008244983322019563477754357443148373214587924703793316035131668632093281689794799552010203036979072745998228887977016967560137289944175879981450443010362243635953713487751097339808735766407946766530433717399616690357414073763857396683264953847812766740959153826576920343287260193512322145933337152804433157752301419133959323789051018321488728983950841632016494748875834963997030892162768637215091130700719449256762516334947154327083207272311653728733630125458903159361822004062081388411039214564237987205848580717624351172165289460158985314136492448861103129430554195152248018886964577555511013222"
//        );

        BigInteger[] m = new BigInteger[tokens.length];
        BigInteger[] c = new BigInteger[tokens.length];
//        BigInteger[] d = new BigInteger[tokens.length];

        for (int i = 0; i < tokens.length; i++)
        {
            m[i] = new BigInteger(tokens[i]);
            c[i] = paillerCryptosystem.Encryption(m[i]);
//            d[i] = paillerCryptosystem.Decryption(c[i]);
        }

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

    private boolean connectToServer(FTPClient ftpClient, String[] serverInfo)
    {
        try
        {
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

    private void ftpDisconnect(FTPClient ftpClient)
    {
        try
        {
            ftpClient.logout();
            ftpClient.disconnect();
        } catch (Exception e)
        {
            Log.e(TAG, "Error: " + e);
        }
    }

    private String writeHistogramToTempFile(HashMap<Integer, Integer> histogram)
    {
        File outputDir = this.getExternalCacheDir(); // context being the Activity pointer
        File outputFile = null;
        try
        {
            outputFile = File.createTempFile("histogram", ".txt", outputDir);
        } catch (Exception e)
        {
            Log.e(TAG, "Error: " + e);

            return null;
        }

        BufferedWriter writer = null;
        try
        {
            writer = new BufferedWriter(new FileWriter(outputFile.getAbsoluteFile()));
            for (int i = 0; i < histogram.size(); i++)
            {
                if(histogram.get(i) != null)
                    writer.write(histogram.get(i) + " ");
                else
                    writer.write(0 + " ");
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

    private String writeMatToTempFile(Mat mat)
    {
        File outputDir = this.getExternalCacheDir(); // context being the Activity pointer
        File outputFile = null;
        try
        {
            outputFile = File.createTempFile("histogram", ".txt", outputDir);
        } catch (Exception e)
        {
            Log.e(TAG, "Error: " + e);

            return null;
        }

        BufferedWriter writer = null;
        try
        {
            writer = new BufferedWriter(new FileWriter(outputFile.getAbsoluteFile()));
//            for (int i = 0; i < mat.rows(); i++)
//            {
//                double[] histValues = mat.get(i, 0);
//                for (int j = 0; j < histValues.length; j++)
//                {
//                    writer.write((int) histValues[j] + " ");
//                }
//            }

            for (int i = 0; i < mat.rows(); i++)
            {
                for (int j = 0; j < mat.cols(); j++)
                {
                    double[] histValues = mat.get(i, j);
                    for(int k = 0; k < histValues.length; k++)
                    {
                        writer.write((int) histValues[k] + " ");
                    }
                }
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

    private static native void nativeCreateLBPHistorgram(long src, long dst, int radius, int neighbors);

    private static native void nativeSpatialHistogram(long src, long dst, int numPatterns, int gridx, int gridy);
}

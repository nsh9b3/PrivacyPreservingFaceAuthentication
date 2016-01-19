package edu.mst.nsh9b3.privacypreservingfaceauthentication;

import android.content.Context;

/**
 * Created by nick on 11/26/15.
 */
public class Utilities
{
    public static int getStringIdentifier(Context context, String name)
    {
        return context.getResources().getIdentifier(name, "string", context.getPackageName());
    }
}

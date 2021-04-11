package de.planetcat.protopietomidi;

import android.app.Application;
import android.content.Intent;
import android.util.Log;

import org.jetbrains.annotations.NotNull;

public class TestApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {

            @Override
            public void uncaughtException(@NotNull Thread thread, @NotNull Throwable ex) {
                handleUncaughtException(thread, ex);

            }
        });
    }

    public void handleUncaughtException (Thread thread, Throwable e)
    {
        String stackTrace = Log.getStackTraceString(e);
        String message = e.getMessage();


        Intent intent = new Intent (Intent.ACTION_SEND);
        intent.setType("message/rfc822");
        intent.putExtra (Intent.EXTRA_EMAIL, new String[] {"ptm-crash@planetcat.de"});
        intent.putExtra (Intent.EXTRA_SUBJECT, "ProtoPieToMidiCrash");
        intent.putExtra (Intent.EXTRA_TEXT, stackTrace);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // required when starting from Application
        startActivity(intent);


    }



}

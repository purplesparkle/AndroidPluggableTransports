package info.pluggabletransports.aptds;

import android.app.Application;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.jrummyapps.android.shell.CommandResult;
import com.jrummyapps.android.shell.Shell;

import java.io.File;

import info.pluggabletransports.aptds.util.ResourceInstaller;

/**
 * Created by n8fr8 on 10/18/17.
 */

public class DispatchService extends Service implements DispatchConstants {
    @Override
    public void onCreate() {
        super.onCreate();

        initBinaries();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        new Thread(new IncomingIntentRouter(intent)).start();

        return super.onStartCommand(intent, flags, startId);

    }

    private class IncomingIntentRouter implements Runnable
    {
        Intent mIntent;

        public IncomingIntentRouter (Intent intent)
        {
            mIntent = intent;
        }

        public void run() {

            String action = mIntent.getAction();

            if (action != null) {
                if (action.equals(ACTION_START)) {

                    String transportType = mIntent.getStringExtra(EXTRA_TRANSPORT_TYPE);

                    //launch transport here
                    int transportPort = startTransport (transportType);

                    replyWithStatus(mIntent,STATUS_ON,transportType,transportPort);
                }
                else if (action.equals(ACTION_STATUS)) {
                   // replyWithStatus(mIntent,STATUS_ON,"http",1234);
                }
            }
        }
    }

    private int startTransport (String type)
    {
        int port = -1;

        //start transport here

        //call the piedispatcher command line here
        String cmd = "piedispacthcher --type=" + type;
        CommandResult shellResult = Shell.run(cmd);
        shellResult.isSuccessful();


        return port;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void initBinaries ()
    {
        File appBinHome = getDir(DIRECTORY_BINARIES, Application.MODE_PRIVATE);
        ResourceInstaller installer = new ResourceInstaller(this, appBinHome);

        try {
            String arch = "armeabi-v7a";
            String runtime = "pidispatcher.armv7";
            installer.installResource('/' + arch + '/' + runtime, false);
        }
        catch (Exception e)
        {
            Log.e(TAG,"Unable to install dispatcher binaries",e);
        }
    }
    /**
     * Send Orbot's status in reply to an
     * {@link DispatchConstants#ACTION_START} {@link Intent}, targeted only to
     * the app that sent the initial request. If the user has disabled auto-
     * starts, the reply {@code ACTION_START Intent} will include the extra
     */
    private void replyWithStatus(Intent startRequest, String status, String type, int port) {

        String packageName = startRequest.getStringExtra(EXTRA_PACKAGE_NAME);

        Intent reply = new Intent(ACTION_STATUS);
        reply.putExtra(EXTRA_STATUS, status);
        reply.putExtra(EXTRA_TRANSPORT_TYPE, type);
        reply.putExtra(EXTRA_TRANSPORT_PORT, port);

        if (packageName != null)
        {
            reply.setPackage(packageName);
            sendBroadcast(reply);
        }


    }
}

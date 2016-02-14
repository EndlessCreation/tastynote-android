package kr.re.ec.android.tastynote.common;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by slhyv on 2/14/2016.
 */
public final class FileUtil {
    private static final String TAG = FileUtil.class.getSimpleName();

    public static void saveDataToLocal(String body) {
        Log.v(TAG, "saveDataToLocal() called. body.Length(): " + body.length());

        try {
            final File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/"
                    + Constants.File.WORK_FOLDER_NAME + "/" );
            Log.v(TAG, "dir.getAbsolutePath(): " + dir.getAbsolutePath());

            if (!dir.exists())
            {
                dir.mkdirs();
            }

            final File myFile = new File(dir, Constants.File.WORK_FILE_NAME);

            if (!myFile.exists())
            {
                myFile.createNewFile();
            }

            FileOutputStream fos = new FileOutputStream(myFile);

            fos.write(body.getBytes());
            fos.close();
        } catch (IOException e) {
            Log.e(TAG, "IOException occured. " + e.getMessage());
            e.printStackTrace();
        }
    }

}

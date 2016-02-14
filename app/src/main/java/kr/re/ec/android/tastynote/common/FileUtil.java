package kr.re.ec.android.tastynote.common;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

/**
 * Created by slhyv on 2/14/2016.
 */
public final class FileUtil {
    private static final String TAG = FileUtil.class.getSimpleName();

    //TODO: make this asynchronous
    public static void saveDataToLocal(String body) {
        Log.v(TAG, "saveDataToLocal() called. body.Length(): " + body.length());

        try {
            final File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/"
                    + Constants.File.WORK_FOLDER_NAME + "/" );
            Log.v(TAG, "dir.getAbsolutePath(): " + dir.getAbsolutePath());

            if (!dir.exists()) {
                dir.mkdirs();
            }

            final File file = new File(dir, Constants.File.WORK_FILE_NAME);

            if (!file.exists()) {
                file.createNewFile();
            }

            //convert unix style newlines to windows.
            Log.v(TAG, "start converting unix style newlines to windows.");
            body = body.replace("\n", "\r\n");
            Log.v(TAG, "end converting.");

            FileOutputStream fos = new FileOutputStream(file);

            fos.write(body.getBytes());
            fos.close();

            Log.v(TAG, "saveDataToLocal() write completed"); //TODO: check time
        } catch (IOException e) {
            Log.e(TAG, "IOException occured. " + e.getMessage());
            e.printStackTrace();
        }
    }

    //TODO: make this asynchronous
    public static String loadDataFromLocal() {
        Log.v(TAG, "loadDataFromLocal() called");

        try {
            final File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/"
                    + Constants.File.WORK_FOLDER_NAME + "/");
            final File file = new File(dir, Constants.File.WORK_FILE_NAME);

            //Read text from file
            StringBuilder text = new StringBuilder();


            BufferedReader br = new BufferedReader(new FileReader(file)); //TODO: check if there is no file
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);

                //convert windows style newline to unix style
                text.append(Constants.File.NEWLINE_UNIX_STYLE);
            }
            br.close();

            Log.v(TAG, "loadDataFromLocal() read completed. size: " + text.length()); //TODO: check time
            return text.toString();

        } catch (IOException e) {
            Log.e(TAG, "IOException occured. " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
}

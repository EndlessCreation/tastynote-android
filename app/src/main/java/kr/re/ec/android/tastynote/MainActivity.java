package kr.re.ec.android.tastynote;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import kr.re.ec.android.tastynote.googledrive.GoogleDriveCreateFolderActivity;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onShowDriveClicked(View view) {
        Log.d(TAG, "onShowDriveClicked clicked");
        Intent intent = new Intent(this, GoogleDriveCreateFolderActivity.class);
        startActivity(intent);

    }
}

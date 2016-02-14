/**
 * Copyright 2013 Google Inc. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kr.re.ec.android.tastynote;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveFolder.DriveFolderResult;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.query.Filter;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;

import java.io.IOException;
import java.io.OutputStream;

import kr.re.ec.android.tastynote.common.Constants;
import kr.re.ec.android.tastynote.common.FileUtil;

/**
 * An activity to illustrate how to create a new folder.
 */
public class MainActivity extends GoogleDriveBaseActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    //google drive
    private ResultsAdapter mResultsAdapter;
    private DriveFolder mRootFolder;

    /* work folder must be child of root folder. */
    private DriveFolder mWorkFolder;
    private DriveFile mWorkFile;

    //ui components
    private TextView mTextView;
    private EditText mEditText;
    private FloatingActionButton mFab;
    private boolean mEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mResultsAdapter = new ResultsAdapter(this);

        String text = FileUtil.loadDataFromLocal();

        initUiComponents(text);

    }

    private void initUiComponents(String text) {
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mFab = (FloatingActionButton) findViewById(R.id.fab);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.v(TAG, "fab onClick() invoked");
                enableEditMode();
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG) //TODO: use this when notify to user
//                        .setAction("Action", null).show();
            }
        });

        mTextView = (TextView) findViewById(R.id.text_view);
        mEditText = (EditText) findViewById(R.id.edit_text);
        mTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO: make this twice click
                enableEditMode();
            }
        });

        mEditText.setText(text);
        disableEditMode();
    }

    //NOTE: connect need 0.2 ~ 3 sec
    @Override
    public void onConnected(Bundle connectionHint) {
        super.onConnected(connectionHint);

        //fetch root folder
        Drive.DriveApi.fetchDriveId(getGoogleApiClient(),
                Drive.DriveApi.getRootFolder(getGoogleApiClient()).getDriveId().getResourceId())
                .setResultCallback(fetchRootCallback);
    }

    final private ResultCallback<DriveApi.DriveIdResult> fetchRootCallback = new ResultCallback<DriveApi.DriveIdResult>() {
        @Override
        public void onResult(DriveApi.DriveIdResult result) {
            if (!result.getStatus().isSuccess()) {
                showMessage("Cannot find DriveId. Are you authorized to view this file?");
                return;
            }

            Log.v(TAG, "fetchRootCallback onResult() invoked. start query...");
            mRootFolder = Drive.DriveApi.getRootFolder(getGoogleApiClient());
            Log.v(TAG, "result Folder Id: " + result.getDriveId().getResourceId());

            //request sync (ignore cache)
            Log.v(TAG, "start requestSync()");
            //TODO: is this slow? yes it is. about 1.8 ~ 2 sec.
            Drive.DriveApi.requestSync(getGoogleApiClient())
                    .setResultCallback(requestSyncCallback);
        }
    };

    final private ResultCallback<Status> requestSyncCallback = new ResultCallback<Status>() {

        @Override
        public void onResult(Status status) {
            Log.v(TAG, "onResult() invoked by requestSync()");

            //query WORK_FOLDER_NAME on root exists.
            Filter nameFilter = Filters.eq(SearchableField.TITLE, Constants.GoogleDrive.WORK_FOLDER_NAME);
            Filter notTrashedFilter = Filters.eq(SearchableField.TRASHED, Boolean.FALSE);

            Query query = new Query.Builder()
                    .addFilter(notTrashedFilter)
                    .addFilter(nameFilter)
                    .build(); //logical AND operate between two filters
            //NOTE: queryChildren 0.3 sec
            Log.v(TAG, "start queryChildren() for find working folder");
            mRootFolder.queryChildren(getGoogleApiClient(), query).setResultCallback(queryExistFolderOnRootCallback);
        }
    };


    final private ResultCallback<DriveApi.MetadataBufferResult> queryExistFolderOnRootCallback = new
            ResultCallback<DriveApi.MetadataBufferResult>() {
                @Override
                public void onResult(DriveApi.MetadataBufferResult result) {
                    Log.v(TAG, "onResult() invoked by queryChilderen()");
                    if (!result.getStatus().isSuccess()) {
                        showMessage("Problem while retrieving results");
                        return;
                    }
                    mResultsAdapter.clear();
                    mResultsAdapter.append(result.getMetadataBuffer());

                    int count = mResultsAdapter.getCount();
                    //log
                    Log.v(TAG, "count: " + count);
                    for (int i = 0; i < count; ++i) {
                        Metadata item = mResultsAdapter.getItem(i);
                        Log.v(TAG, "i:" + i
                                        + ", title: " + item.getTitle()
                                        + ", createdDate: " + item.getCreatedDate()
                                        + ", driveId: " + item.getDriveId()
                                        + ", isTrashed: " + item.isTrashed()
                                        + ", getContentAvailability: " + item.getContentAvailability()
                                        + ", getDescription: " + item.getDescription()
                        );
                    }

                    if (count == 0) { //if not exist, create folder on root
                        Log.v(TAG, "count is 0. create new working folder.");
                        MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                                .setTitle(Constants.GoogleDrive.WORK_FOLDER_NAME).build();

                        //NOTE: createFolder need 0.1 sec
                        Log.v(TAG, "createFolder()");
                        Drive.DriveApi.getRootFolder(getGoogleApiClient()).createFolder(
                                getGoogleApiClient(), changeSet).setResultCallback(createFolderCallback);
                    } else if (count == 1) { //it's OK
                        Log.v(TAG, "count is 1. start getting working file id");
                        mWorkFolder = mResultsAdapter.getItem(0).getDriveId().asDriveFolder();

                        //query children working file.
                        Filter nameFilter = Filters.eq(SearchableField.TITLE, Constants.GoogleDrive.WORK_FILE_NAME);

                        Query query = new Query.Builder().addFilter(nameFilter)
                                .build(); //logical AND operate between two filters
                        Log.v(TAG, "start queryChildren() for find working file");
                        mWorkFolder.queryChildren(getGoogleApiClient(), query).setResultCallback(queryExistFileOnWorkingFolderCallback);

                    } else if (count > 1) {
                        Log.v(TAG, "count is more than 1. FATAL ERROR");
                        throw new ArrayIndexOutOfBoundsException("FATAL: count of Work Folder is more than 1. plz make 1 working folder in your Google Drive");
                    }

                    result.release(); //NOTE: have to release
                    Log.v(TAG, "metadataBuffer released.");
                }
            };

    final private ResultCallback<DriveApi.MetadataBufferResult> queryExistFileOnWorkingFolderCallback = new
            ResultCallback<DriveApi.MetadataBufferResult>() {
                @Override
                public void onResult(DriveApi.MetadataBufferResult result) {
                    Log.v(TAG, "onResult() invoked by queryChilderen()");
                    if (!result.getStatus().isSuccess()) {
                        showMessage("Problem while retrieving results");
                        return;
                    }
                    mResultsAdapter.clear();
                    mResultsAdapter.append(result.getMetadataBuffer());

                    int count = mResultsAdapter.getCount();
                    //log
                    Log.v(TAG, "count: " + count);

                    for (int i = 0; i < count; ++i) {
                        Metadata item = mResultsAdapter.getItem(i);
                        Log.v(TAG, "i:" + i
                                        + ", title: " + item.getTitle()
                                        + ", createdDate: " + item.getCreatedDate()
                                        + ", driveId: " + item.getDriveId()
                                        + ", isTrashed: " + item.isTrashed()
                                        + ", getContentAvailability: " + item.getContentAvailability()
                                        + ", getDescription: " + item.getDescription()
                        );
                    }

                    if (count == 0) {
                        //TODO: make file and get File ID
                    } else if(count == 1) {
                        mWorkFile = mResultsAdapter.getItem(0).getDriveId().asDriveFile();
                        Log.v(TAG, "mWorkFile ID: " + mWorkFile.getDriveId());
                    } else if(count > 1) {
                        Log.v(TAG, "count is more than 1. FATAL ERROR");
                        throw new ArrayIndexOutOfBoundsException("FATAL: count of Work File is more than 1. plz make 1 working file in your Google Drive");
                    }
                }
            };

    final ResultCallback<DriveFolderResult> createFolderCallback = new ResultCallback<DriveFolderResult>() {
        @Override
        public void onResult(DriveFolderResult result) {
            Log.v(TAG, "onResult() invoked by createFolder()");
            if (!result.getStatus().isSuccess()) {
                showMessage("Error while trying to create the folder");
                return;
            }
            mWorkFolder = result.getDriveFolder();
            showMessage("Created a folder: " + mWorkFolder.getDriveId());

            //do upload empty text(txt, 0 byte).
            //create new contents resource
            Log.v(TAG, "start newDriveContents()");
            Drive.DriveApi.newDriveContents(getGoogleApiClient())
                    .setResultCallback(newDriveContentsCallback);
        }
    };

    // [START drive_contents_callback]
    final private ResultCallback<DriveApi.DriveContentsResult> newDriveContentsCallback =
            new ResultCallback<DriveApi.DriveContentsResult>() {
                @Override
                public void onResult(DriveApi.DriveContentsResult result) {
                    Log.v(TAG, "onResult() invoked by newDriveContents()");
                    if (!result.getStatus().isSuccess()) {
                        showMessage("Error while trying to create new file contents");
                        return;
                    }

                    MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                            .setTitle(Constants.GoogleDrive.WORK_FILE_NAME)
                            .setMimeType("text/plain")
                            .build();
                    Log.v(TAG, "start createFile()");
                    mWorkFolder.createFile(getGoogleApiClient(), changeSet, result.getDriveContents())
                            .setResultCallback(createFileCallback);
                }
            };
    // [END drive_contents_callback]

    final private ResultCallback<DriveFolder.DriveFileResult> createFileCallback = new
            ResultCallback<DriveFolder.DriveFileResult>() {
                @Override
                public void onResult(DriveFolder.DriveFileResult result) {
                    Log.v(TAG, "onResult() invoked by createFile()");
                    if (!result.getStatus().isSuccess()) {
                        showMessage("Error while trying to create the file");
                        return;
                    }
                    showMessage("Created a file in Working Folder. id: " + result.getDriveFile().getDriveId());
                }
            };

    private void enableEditMode() {
        Log.v(TAG, "enableEditMode() called");

        //set visibilities
        mTextView.setVisibility(View.INVISIBLE);
        mEditText.setVisibility(View.VISIBLE);
        mFab.setVisibility(View.INVISIBLE);

        //set focus and show keyboard
        mEditText.setFocusableInTouchMode(true);
        mEditText.requestFocus();
        showKeyboard(mEditText);

        mEditMode = true;
    }

    private void showKeyboard(EditText et) {
        final InputMethodManager inputMethodManager = (InputMethodManager) this
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.showSoftInput(et, InputMethodManager.SHOW_IMPLICIT);
    }

    private void disableEditMode() {
        Log.v(TAG, "disableEditMode() called");

        //set visibilities
        mTextView.setVisibility(View.VISIBLE);
        mEditText.setVisibility(View.INVISIBLE);
        mFab.setVisibility(View.VISIBLE);

        mTextView.setText(mEditText.getText());

        FileUtil.saveDataToLocal(mEditText.getText().toString());

        Log.v(TAG, "start syncDataToRemote()");
        if(mWorkFile != null) {
            syncDataToRemote();
        }

        mEditMode = false;
    }

    public void syncDataToRemote() {
        new EditContentsAsyncTask(MainActivity.this).execute(mWorkFile);
    }

    @Override
    public void onBackPressed() {
        Log.v(TAG, "onBackPressed() invoked. mEditMode: " + mEditMode);

        if(mEditMode) {
            disableEditMode();
        } else {
            super.onBackPressed();
        }

        //TODO: on view mode, to exit back button twice
    }

    //TODO: showProgressDialog
    //TODO: hideProgressDialog

    public class EditContentsAsyncTask extends GoogleDriveApiClientAsyncTask<DriveFile, Void, Boolean> {
        private final String TAG = EditContentsAsyncTask.class.getSimpleName();

        public EditContentsAsyncTask(Context context) {
            super(context);
        }

        @Override
        protected Boolean doInBackgroundConnected(DriveFile... args) {
            DriveFile file = args[0];
            try {
                DriveApi.DriveContentsResult driveContentsResult = file.open(
                        getGoogleApiClient(), DriveFile.MODE_WRITE_ONLY, null).await();
                if (!driveContentsResult.getStatus().isSuccess()) {
                    return false;
                }
                DriveContents driveContents = driveContentsResult.getDriveContents();
                OutputStream outputStream = driveContents.getOutputStream();
                outputStream.write(mEditText.getText().toString().getBytes()); //TODO: is this work?
                com.google.android.gms.common.api.Status status =
                        driveContents.commit(getGoogleApiClient(), null).await();
                return status.getStatus().isSuccess();
            } catch (IOException e) {
                Log.e(TAG, "IOException while appending to the output stream", e);
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (!result) {
                showMessage("Error while editing contents");
                return;
            }
            showMessage("Successfully edited contents");
        }
    }
}

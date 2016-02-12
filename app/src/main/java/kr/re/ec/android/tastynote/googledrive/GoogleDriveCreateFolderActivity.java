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

package kr.re.ec.android.tastynote.googledrive;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveFolder.DriveFolderResult;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.query.Filter;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;

import kr.re.ec.android.tastynote.R;
import kr.re.ec.android.tastynote.common.Constants;

/**
 * An activity to illustrate how to create a new folder.
 */
public class GoogleDriveCreateFolderActivity extends GoogleDriveBaseActivity {
    private static final String TAG = GoogleDriveCreateFolderActivity.class.getName();

    private ResultsAdapter mResultsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mResultsAdapter = new ResultsAdapter(this);

        setContentView(R.layout.activity_create_folder);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }

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
            DriveFolder rootFolder = Drive.DriveApi.getRootFolder(getGoogleApiClient());
            Log.v(TAG, "result Folder Id: " + result.getDriveId().getResourceId());

            //query WORK_FOLDER_NAME on root exists.
            Filter nameFilter = Filters.eq(SearchableField.TITLE, Constants.GoogleDrive.WORK_FOLDER_NAME);
            Filter notTrashedFilter = Filters.eq(SearchableField.TRASHED, Boolean.FALSE);

            Query query = new Query.Builder()
                    .addFilter (notTrashedFilter)
                    .addFilter (nameFilter)
                    .build(); //logical AND operate between two filters
            rootFolder.queryChildren(getGoogleApiClient(), query)
                    .setResultCallback(queryExistFolderOnRootCallback);
        }
    };

    final private ResultCallback<DriveApi.MetadataBufferResult> queryExistFolderOnRootCallback = new
            ResultCallback<DriveApi.MetadataBufferResult>() {
                @Override
                public void onResult(DriveApi.MetadataBufferResult result) {
                    Log.v(TAG, "onResult() invoked");
                    if (!result.getStatus().isSuccess()) {
                        showMessage("Problem while retrieving results");
                        return;
                    }
                    mResultsAdapter.clear();
                    mResultsAdapter.append(result.getMetadataBuffer());

                    int count = mResultsAdapter.getCount();
                    //log
                    Log.v(TAG, "count: " + count);
                    for(int i=0; i<count; ++i) {
                        Log.v(TAG, "i:"+i+", title: " + mResultsAdapter.getItem(i).getTitle()
                                + ", createdDate: " + mResultsAdapter.getItem(i).getCreatedDate());
                    }

                    //if not exist, create folder on root
                    if(count == 0) {
                        MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                                .setTitle(Constants.GoogleDrive.WORK_FOLDER_NAME).build();
                        Drive.DriveApi.getRootFolder(getGoogleApiClient()).createFolder(
                                getGoogleApiClient(), changeSet).setResultCallback(createFolderOnRootCallback);
                    }
                }
            };

    final ResultCallback<DriveFolderResult> createFolderOnRootCallback = new ResultCallback<DriveFolderResult>() {
        @Override
        public void onResult(DriveFolderResult result) {
            if (!result.getStatus().isSuccess()) {
                showMessage("Error while trying to create the folder");
                return;
            }
            showMessage("Created a folder: " + result.getDriveFolder().getDriveId());
        }
    };

}

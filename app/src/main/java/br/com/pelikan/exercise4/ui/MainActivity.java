package br.com.pelikan.exercise4.ui;

import android.app.DownloadManager;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkStatus;

import br.com.pelikan.exercise4.R;
import br.com.pelikan.exercise4.worker.DownloadWorker;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Data inputData = new Data.Builder()
                .putString("key_url",
                        "http://www.electrolux.com.br/LocalFiles/Brazil_Portuguese/Catalogo-Acessorios-Mar-15.pdf")
                .build();
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest otwRequest =
                new OneTimeWorkRequest.Builder(DownloadWorker.class)
                        .setInputData(inputData)
                        .setConstraints(constraints).build();

        final LiveData<WorkStatus> status =
                WorkManager.getInstance().getStatusById(otwRequest.getId());

        status.observe(this, new Observer<WorkStatus>() {
            @Override
            public void onChanged(@Nullable WorkStatus workStatus) {
                if ((workStatus != null) && (workStatus.getState().isFinished())){
                    Uri myDownloads = Uri.parse( "content://downloads/my_downloads" );
                    long downloadId = workStatus.getOutputData().getLong("key_download_id", 0);
                    getContentResolver().registerContentObserver( myDownloads, true, new DownloadObserver(new Handler(), downloadId) );
                }
            }
        });

        WorkManager.getInstance().enqueue(otwRequest);
    }

    private class DownloadObserver extends ContentObserver {
        long downloadId = 0;

        DownloadObserver(Handler handler, long downloadId) {
            super(handler);
            this.downloadId = downloadId;
        }

        @Override
        public void onChange( boolean selfChange, Uri uri ) {
            Log.d( "DownloadObserver", "Download " + uri + " updated" );

            DownloadManager downloadManager = (DownloadManager)
                    getApplicationContext()
                            .getSystemService(Context.DOWNLOAD_SERVICE);

            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(downloadId);

            Cursor c = downloadManager.query(query);
            if (c.moveToFirst()) {
                int sizeIndex = c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
                int downloadedIndex = c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
                long size = c.getInt(sizeIndex);
                long downloaded = c.getInt(downloadedIndex);
                double progress = 0.0;
                if (size != -1) {
                    progress = downloaded * 100.0 / size;
                    ((TextView)findViewById(R.id.statusTextView)).setText("Download em Andamento " + Math.round(progress));
                }
            }
        }
    }
}

package br.com.pelikan.exercise4.worker;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.webkit.URLUtil;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;

public class DownloadWorker extends Worker {

    @NonNull
    @Override
    public Result doWork() {
        String url = getInputData().getString("key_url");

        if (URLUtil.isValidUrl(url)) {
            long downloadId = downloadFile(url);
            Data outputData = new Data.Builder()
                    .putLong("key_download_id", downloadId)
                    .putString("key_download_url", String.valueOf(Uri.parse(url))).build();
            setOutputData(outputData);
            return Result.SUCCESS;
        } else {
            return Result.FAILURE;
        }

    }

    public long downloadFile(String url) {
        Long result = -1L;

        DownloadManager downloadManager = (DownloadManager)
                getApplicationContext()
                        .getSystemService(Context.DOWNLOAD_SERVICE);

        if (downloadManager != null) {
            DownloadManager.Request request =
                    new DownloadManager.Request(Uri.parse(url));
            result = downloadManager.enqueue(request);
        }

        return result;
    }
}
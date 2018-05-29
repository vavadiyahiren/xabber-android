package com.xabber.android.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.ResultReceiver;
import android.support.annotation.Nullable;
import android.util.Log;

import com.xabber.android.data.connection.CertificateManager;
import com.xabber.android.data.database.MessageDatabaseManager;
import com.xabber.android.data.database.messagerealm.Attachment;
import com.xabber.android.data.entity.AccountJid;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

import de.duenndns.ssl.MemorizingTrustManager;
import io.realm.Realm;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadService extends IntentService {

    private static final String LOG_TAG = "DownloadService";
    private final static String SERVICE_NAME = "Download Service";
    public static final int UPDATE_PROGRESS_CODE = 3132;
    public static final int ERROR_CODE = 3133;
    public static final int COMPLETE_CODE = 3134;
    private static final String XABBER_DIR = "Xabber";

    public final static String KEY_ATTACHMENT_ID = "attachment_id";
    public final static String KEY_RECEIVER = "receiver";
    public final static String KEY_PROGRESS = "progress";
    public final static String KEY_ACCOUNT_JID = "account_jid";
    public final static String KEY_FILE_NAME = "file_name";
    public final static String KEY_FILE_SIZE = "file_size";
    public final static String KEY_URL = "url";
    public final static String KEY_ERROR = "error";

    private ResultReceiver receiver;
    private String attachmentId;

    public DownloadService() {
        super(SERVICE_NAME);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent == null) return;
        this.receiver = intent.getParcelableExtra(KEY_RECEIVER);
        this.attachmentId = intent.getStringExtra(KEY_ATTACHMENT_ID);
        String fileName = intent.getStringExtra(KEY_FILE_NAME);
        long fileSize = intent.getLongExtra(KEY_FILE_SIZE, 0);
        String url = intent.getStringExtra(KEY_URL);
        AccountJid accountJid = intent.getParcelableExtra(KEY_ACCOUNT_JID);

        // build http client
        OkHttpClient client = createHttpClient(accountJid);

        // start download
        if (client != null) requestFileDownload(fileName, fileSize, url, client);
        else publishError("Downloading not started");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        publishError("Downloading aborted");
    }

    private static OkHttpClient createHttpClient(AccountJid accountJid) {
        // create ssl verification
        SSLSocketFactory sslSocketFactory = null;
        MemorizingTrustManager mtm = CertificateManager.getInstance().getNewFileUploadManager(accountJid);

        final SSLContext sslContext;
        try {
            sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, new X509TrustManager[]{mtm}, new java.security.SecureRandom());
            sslSocketFactory = sslContext.getSocketFactory();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            return null;
        }

        // build http client
        final OkHttpClient client = new OkHttpClient().newBuilder()
                .sslSocketFactory(sslSocketFactory)
                .hostnameVerifier(mtm.wrapHostnameVerifier(new org.apache.http.conn.ssl.StrictHostnameVerifier()))
                .writeTimeout(5, TimeUnit.MINUTES)
                .connectTimeout(5, TimeUnit.MINUTES)
                .readTimeout(5, TimeUnit.MINUTES)
                .build();

        return client;
    }

    private void requestFileDownload(final String fileName, final long fileSize, String url, OkHttpClient client) {
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            public void onFailure(Call call, IOException e) {
                Log.d(LOG_TAG, "download onFailure " + e.getMessage());
                publishError(e.getMessage());
            }

            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.d(LOG_TAG, "download onFailure " + response.toString());
                    publishError(response.toString());
                    return;
                }

                // create dir
                File directory = new File(getDownloadDirPath());
                directory.mkdirs();

                // create file
                String filePath = directory.getPath() + File.separator + fileName;
                File file = new File(filePath);

                if (file.exists()) {
                    publishError("File with same name already exist");
                    return;
                }

                if (file.createNewFile()) {

                    // download
                    FileOutputStream fos = new FileOutputStream(file);
                    byte [] buffer = new byte [8192];
                    int r;

                    int downloadedBytes = 0;
                    while ((r = response.body().byteStream().read(buffer)) > 0) {
                        try {
                            TimeUnit.MILLISECONDS.sleep(200);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        fos.write(buffer, 0, r);
                        downloadedBytes += r;
                        int progress = (int) Math.round((double) downloadedBytes / (double) fileSize * 100.d);
                        publishProgress(progress);
                    }
                    fos.close();

                    // save path to realm
                    saveAttachmentPathToRealm(file.getPath());

                } else publishError("File not created");
            }
        });
    }

    private void saveAttachmentPathToRealm(final String path) {
        MessageDatabaseManager.getInstance().getNewBackgroundRealm().executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                Attachment attachment = realm.where(Attachment.class)
                        .equalTo(Attachment.Fields.UNIQUE_ID, attachmentId).findFirst();
                attachment.setFilePath(path);
                publishCompleted();
            }
        });
    }

    private void publishProgress(int progress) {
        Bundle resultData = new Bundle();
        resultData.putInt(KEY_PROGRESS, progress);
        receiver.send(UPDATE_PROGRESS_CODE, resultData);
    }

    private void publishCompleted() {
        Bundle resultData = new Bundle();
        receiver.send(COMPLETE_CODE, resultData);
    }

    private void publishError(String error) {
        Bundle resultData = new Bundle();
        resultData.putString(KEY_ERROR, error);
        receiver.send(ERROR_CODE, resultData);
    }

    private static String getDownloadDirPath() {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath()
                + File.separator + XABBER_DIR;
    }
}
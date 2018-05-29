package com.xabber.android.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.NavUtils;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.xabber.android.R;
import com.xabber.android.data.database.MessageDatabaseManager;
import com.xabber.android.data.database.messagerealm.Attachment;
import com.xabber.android.data.database.messagerealm.MessageItem;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.filedownload.DownloadManager;
import com.xabber.android.ui.fragment.ImageViewerFragment;

import io.realm.Realm;
import io.realm.RealmList;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;

public class ImageViewerActivity extends AppCompatActivity implements Toolbar.OnMenuItemClickListener {

    private static final String MESSAGE_ID = "MESSAGE_ID";
    private static final String ATTACHMENT_POSITION = "ATTACHMENT_POSITION";

    private AccountJid accountJid;
    private RealmList<Attachment> imageAttachments = new RealmList<>();
    private Toolbar toolbar;
    private ViewPager viewPager;
    private ProgressBar progressBar;
    private ImageView ivCancelDownload;

    private CompositeSubscription subscriptions = new CompositeSubscription();

    @NonNull
    public static Intent createIntent(Context context, String id, int position) {
        Intent intent = new Intent(context, ImageViewerActivity.class);
        Bundle args = new Bundle();
        args.putString(MESSAGE_ID, id);
        args.putInt(ATTACHMENT_POSITION, position);
        intent.putExtras(args);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);

        // get params
        Intent intent = getIntent();
        Bundle args = intent.getExtras();
        String messageId = args.getString(MESSAGE_ID);
        int imagePosition = args.getInt(ATTACHMENT_POSITION);

        // setup toolbar
        toolbar = (Toolbar) findViewById(R.id.toolbar_default);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp);
        toolbar.inflateMenu(R.menu.menu_image_viewer);
        toolbar.setOnMenuItemClickListener(this);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavUtils.navigateUpFromSameTask(ImageViewerActivity.this);
            }
        });

        // get imageAttachments
        Realm realm = MessageDatabaseManager.getInstance().getRealmUiThread();
        MessageItem messageItem = realm.where(MessageItem.class)
                .equalTo(MessageItem.Fields.UNIQUE_ID, messageId)
                .findFirst();
        RealmList<Attachment> attachments = messageItem.getAttachments();

        for (Attachment attachment : attachments) {
            if (attachment.isImage()) imageAttachments.add(attachment);
        }

        // get account jid
        this.accountJid = messageItem.getAccount();

        // find views
        progressBar = findViewById(R.id.progressBar);
        ivCancelDownload = findViewById(R.id.ivCancelDownload);
        ivCancelDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onCancelDownloadClick();
            }
        });

        viewPager = findViewById(R.id.viewPager);
        PagerAdapter pagerAdapter = new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                Attachment attachment = imageAttachments.get(position);
                return ImageViewerFragment.newInstance(attachment.getFilePath(), attachment.getFileUrl());
            }

            @Override
            public int getCount() {
                return imageAttachments.size();
            }
        };
        viewPager.setAdapter(pagerAdapter);
        viewPager.setCurrentItem(imagePosition);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }

            @Override
            public void onPageSelected(int position) {
                updateToolbar();
            }

            @Override
            public void onPageScrollStateChanged(int state) { }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateToolbar();
        subscribeForDownloadProgress();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unsubscribeAll();
        showProgress(false);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_download_image:
                onImageDownloadClick();
                break;
        }
        return true;
    }

    private void updateToolbar() {
        int current = 0, total = 0;
        if (viewPager != null) current = viewPager.getCurrentItem() + 1;
        if (imageAttachments != null) total = imageAttachments.size();
        toolbar.setTitle(current + " of " + total);
        setUpMenuOptions(toolbar.getMenu());
    }

    private void setUpMenuOptions(Menu menu) {
        int position = viewPager.getCurrentItem();
        Attachment attachment = imageAttachments.get(position);
        String filePath = attachment.getFilePath();
        menu.findItem(R.id.action_download_image).setVisible(filePath == null);
    }

    private void onImageDownloadClick() {
        int position = viewPager.getCurrentItem();
        Attachment attachment = imageAttachments.get(position);
        DownloadManager.getInstance().downloadFile(attachment, accountJid, this);
    }

    private void onCancelDownloadClick() {
        DownloadManager.getInstance().cancelDownload(this);
    }

    private void unsubscribeAll() {
        subscriptions.clear();
    }

    private void subscribeForDownloadProgress() {
        subscriptions.add(DownloadManager.getInstance().subscribeForProgress()
            .doOnNext(new Action1<DownloadManager.ProgressData>() {
                @Override
                public void call(DownloadManager.ProgressData progressData) {
                    if (progressData.isCompleted()) {
                        showProgress(false);
                        updateToolbar();
                    } else if (progressData.getError() != null) {
                        showProgress(false);
                        showToast(progressData.getError());
                    } else {
                        progressBar.setProgress(progressData.getProgress());
                        showProgress(true);
                    }
                }
            }).subscribe());
    }

    private void showToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    private void showProgress(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            ivCancelDownload.setVisibility(View.VISIBLE);
        } else {
            progressBar.setVisibility(View.GONE);
            ivCancelDownload.setVisibility(View.GONE);
        }
    }
}
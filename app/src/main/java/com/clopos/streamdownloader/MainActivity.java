package com.clopos.streamdownloader;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.theo.downloader.DownloaderFactory;
import com.theo.downloader.IDownloader;
import com.theo.downloader.Task;
import com.theo.downloader.info.SnifferInfo;
import com.theo.downloader.util.FileUtil;

import java.io.File;
import java.io.FileOutputStream;

public class MainActivity extends AppCompatActivity {

    private final String mUrl = "https://html5demos.com/assets/dizzy.mp4";
    private String mTempPath;

    private IDownloader mDownloader;
    private ProgressBar mProgressBar;
    private TextView mTvDownSpeed;
    private EditText mEtUrl;
    private TextView mTvResult;

    /**
     * downloader listener
     */
    private IDownloader.DownloadListener mDownloadListener = new IDownloader.DownloadListener() {
        /**
         * when download task created.after sniffer the net
         * @param task
         * @param snifferInfo
         */
        @Override
        public void onCreated(Task task, SnifferInfo snifferInfo) {
            System.out.println("onCreated realUrl:" + snifferInfo.realUrl);
            System.out.println("onCreated contentLength:" + snifferInfo.contentLength);
        }

        /**
         * task start to download
         * @param task
         */
        @Override
        public void onStart(Task task) {
            System.out.println("onStart");
        }

        /**
         * task pause to download
         * @param task
         */
        @Override
        public void onPause(Task task) {
            System.out.println("onPause");
        }

        /**
         * download progress
         * @param task task
         * @param total total bytes ps: HLS type not support this progress.use {@link com.theo.downloader.hls.HLSDownloader#setMediaSegmentListener(IDownloader.DownloadListener)}
         * @param down bytes down
         */
        @Override
        public void onProgress(final Task task, final long total, final long down) {
            RunnableUtil.runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    updateProgress(total, down);
                    updateDownSpeed(task.getDownSpeed());
                }
            });
        }

        /**
         * throw error
         * @param task
         * @param error
         * @param msg
         */
        @Override
        public void onError(Task task, int error, String msg) {
            System.out.println("onError [" + error + "," + msg + "]");
        }

        /**
         * task complete
         * @param task
         * @param total
         */
        @Override
        public void onComplete(final Task task, long total) {
            System.out.println("onComplete [" + total + "]");
            RunnableUtil.runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    File file = new File(task.getDstDir() + "/" + task.getFileName());
                    String result = "";
                    result += "Download Complete!\n";
                    result += "Path: " + file.getAbsolutePath() + "\n";
                    result += "Length: " + file.length() + "\n";
                    mTvResult.setText(result);
                }
            });
        }

        /**
         * save the instance.when you wanna continue download next time.
         * @param task
         * @param data ??????????????????
         */
        @Override
        public void onSaveInstance(Task task, byte[] data) {
            saveInstanceToFile(new File(mTempPath), data);
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTempPath = getExternalCacheDir().getAbsolutePath() + "/bundle.tmp";//you should save the path to Persistence way like DB or file
        initView();
    }

    private void initView() {
        findViewById(R.id.btCreate).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onCreateClick();
            }
        });
        findViewById(R.id.btStart).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onStartClick();
            }
        });
        findViewById(R.id.btPause).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onPauseClick();
            }
        });
        findViewById(R.id.btLoad).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onLoadClick();
            }
        });

        mProgressBar = (ProgressBar) findViewById(R.id.pbProgress);
        mTvDownSpeed = (TextView) findViewById(R.id.tvDownSpeed);
        mEtUrl = (EditText) findViewById(R.id.etUrl);
        mEtUrl.setText(mUrl);

        mTvResult = (TextView) findViewById(R.id.tvResult);
        findViewById(R.id.tvGotoHLS).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gotoHLSDemo();
            }
        });
    }

    private void gotoHLSDemo() {
        startActivity(new Intent(this, HLSActivity.class));
    }

    private void onCreateClick() {
        Task task = new Task(mEtUrl.getText().toString(), mEtUrl.getText().toString(), getExternalCacheDir().getAbsolutePath());
        mDownloader = DownloaderFactory.create(IDownloader.Type.MULTI_THREAD, task);
        if (mDownloader != null) {
            mDownloader.setListener(mDownloadListener);
            mDownloader.create();
        }
    }

    private void onStartClick() {
        if (mDownloader != null) {
            mDownloader.start();
        }
    }

    private void onPauseClick() {
        if (mDownloader != null) {
            mDownloader.pause();
        }
    }

    private void onLoadClick() {
        mDownloader = DownloaderFactory.load(FileUtil.readFile(new File(mTempPath)));
        if (mDownloader != null) {
            mDownloader.setListener(mDownloadListener);
        }
    }

    private void saveInstanceToFile(File file, byte[] data) {
        System.out.println("onSaveInstance data.length:" + data.length);
        try {
            FileOutputStream os = new FileOutputStream(file);
            os.write(data);
            os.flush();
            os.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateProgress(long total, long down) {
        if (mProgressBar != null) {
            int progress = (int) (down * mProgressBar.getMax() / total);
            System.out.println("onProgress [" + total + "," + down + "," + progress + "]");
            mProgressBar.setProgress(progress);
        }
    }

    /**
     * convert yourself
     *
     * @param speed
     */
    private void updateDownSpeed(long speed) {
        String speedText;
        if (speed < 1024) { //you can also convert to double
            speedText = speed + " B/s";
        } else if (speed < 1024 * 1024) {
            speedText = (speed / 1024) + " KB/S";
        } else {
            speedText = (speed / (1024 * 1024)) + " MB/S";
        }

        if (mTvDownSpeed != null) {
            mTvDownSpeed.setText(speedText);
        }
    }
}

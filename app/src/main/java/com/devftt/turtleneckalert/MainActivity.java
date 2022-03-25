package com.devftt.turtleneckalert;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.PowerManager;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;


public class MainActivity extends AppCompatActivity {
    private Intent serviceIntent;
    private WebView m_webView;
    private ValueCallback mFilePathCallback;
    private long backKeyPressedTime = 0;
    public static final String WIFE_STATE = "WIFE";
    public static final String MOBILE_STATE = "MOBILE";
    public static final String NONE_STATE = "NONE";
    private static final long GAME_LENGTH_MILLISECONDS = 3000;
    private static final String AD_UNIT_ID = "ca-app-pub-3414125324355914/2895380903";

    private InterstitialAd interstitialAd;

    public class AndroidBridge {
        private final Handler handler = new Handler();

        @JavascriptInterface
        public void setNoticeOn() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    interstitialAd.show(MainActivity.this);
                }
            });

            PreferenceManager.setBoolean(RealService.mContext, RealService.NOTICE_ON_OFF_KEY, true);
        }

        @JavascriptInterface
        public void setNoticeOff() {
            PreferenceManager.setBoolean(RealService.mContext, RealService.NOTICE_ON_OFF_KEY, false);
        }

        @JavascriptInterface
        public void setNoticeTime_S(String input) {
            RealService.isStopTimer = true;
            PreferenceManager.setString(RealService.mContext, RealService.NOTICE_TIME_S_KEY, input);
        }

        @JavascriptInterface
        public void setNoticeTime_M(String input) {
            RealService.isStopTimer = true;
            PreferenceManager.setString(RealService.mContext, RealService.NOTICE_TIME_M_KEY, input);
        }

        @JavascriptInterface
        public void setNoticeTime_H(String input) {
            RealService.isStopTimer = true;
            PreferenceManager.setString(RealService.mContext, RealService.NOTICE_TIME_H_KEY, input);
        }

        @JavascriptInterface
        public boolean getNotice_ON_OFF() {
            return PreferenceManager.getBoolean(RealService.mContext, RealService.NOTICE_ON_OFF_KEY);
        }

        @JavascriptInterface
        public String getNoticeTime_S() {
            return PreferenceManager.getString(RealService.mContext, RealService.NOTICE_TIME_S_KEY);
        }

        @JavascriptInterface
        public String getNoticeTime_M() {
            return PreferenceManager.getString(RealService.mContext, RealService.NOTICE_TIME_M_KEY);
        }

        @JavascriptInterface
        public String getNoticeTime_H() {
            return PreferenceManager.getString(RealService.mContext, RealService.NOTICE_TIME_H_KEY);
        }
    }


    @SuppressLint("JavascriptInterface")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });
        AdRequest adRequest = new AdRequest.Builder().build();


        loadAd();

        PowerManager pm = (PowerManager) getApplicationContext().getSystemService(POWER_SERVICE);
        boolean isWhiteListing = false;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            isWhiteListing = pm.isIgnoringBatteryOptimizations(getApplicationContext().getPackageName());
        }
        if (!isWhiteListing) {
            Intent intent = new Intent();
            intent.setAction(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + getApplicationContext().getPackageName()));
            startActivity(intent);
        }

        // 초기화
        m_webView = (WebView) findViewById(R.id.webView);

        // WebView 설정
        m_webView.getSettings().setJavaScriptEnabled(true);
        m_webView.setWebChromeClient(new WebChromeClient());
        m_webView.setWebViewClient(new WebViewClient());
        m_webView.getSettings().setDomStorageEnabled(true);
        m_webView.setNetworkAvailable(true);
        m_webView.setWebChromeClient(new WebChromeClient(){
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback filePathCallback, FileChooserParams fileChooserParams) {
                mFilePathCallback = filePathCallback;

                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");

                startActivityForResult(intent, 0);
                return true;
            }
        });


        m_webView.addJavascriptInterface(new AndroidBridge(),"androidFunction");
        m_webView.loadUrl("file:///android_asset/index.html");


        if (RealService.serviceIntent == null) {
            serviceIntent = new Intent(this, RealService.class);
            startService(serviceIntent);
        } else {
            serviceIntent = RealService.serviceIntent;//getInstance().getApplication();
        }


        String getNetwork =  getWhatKindOfNetwork(getApplication());
        if(getNetwork.equals("NONE")) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("인터넷 연결 안 됨");
            builder.setMessage("이 앱의 중요 서비스인 거북목 판별 기능을 이용하기 위해서는 인터넷에 연결되어야 합니다.");
            builder.setPositiveButton("확인", null);
            builder.create().show();
        }
    }


    @Override
    public void onBackPressed() {
        // 기존의 뒤로가기 버튼의 기능 제거
        // super.onBackPressed();

        // 2000 milliseconds = 2 seconds
        if (System.currentTimeMillis() > backKeyPressedTime + 2000) {
            backKeyPressedTime = System.currentTimeMillis();
            Toast.makeText(this, "\'뒤로\' 버튼을 한번 더 누르시면 종료됩니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2초 이내에 뒤로가기 버튼을 한번 더 클릭시 finish()(앱 종료)
        if (System.currentTimeMillis() <= backKeyPressedTime + 2000) {
            moveTaskToBack(true);
        }
    }


    public static String getWhatKindOfNetwork(Context context){
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null) {
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                return WIFE_STATE;
            } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                return MOBILE_STATE;
            }
        }
        return NONE_STATE;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0 && resultCode == Activity.RESULT_OK) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mFilePathCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
            } else {
                mFilePathCallback.onReceiveValue(new Uri[]{data.getData()});
            }
            mFilePathCallback = null;
        } else {
            mFilePathCallback.onReceiveValue(null);
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceIntent!=null) {
            stopService(serviceIntent);
            serviceIntent = null;
        }
    }




    public void loadAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(
                this,
                AD_UNIT_ID,
                adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        // The mInterstitialAd reference will be null until
                        // an ad is loaded.
                        MainActivity.this.interstitialAd = interstitialAd;

                        Toast.makeText(MainActivity.this, "onAdLoaded()", Toast.LENGTH_SHORT).show();
                        interstitialAd.setFullScreenContentCallback(
                                new FullScreenContentCallback() {
                                    @Override
                                    public void onAdDismissedFullScreenContent() {
                                        // Called when fullscreen content is dismissed.
                                        // Make sure to set your reference to null so you don't
                                        // show it a second time.
                                        MainActivity.this.interstitialAd = null;
                                    }

                                    @Override
                                    public void onAdFailedToShowFullScreenContent(AdError adError) {
                                        // Called when fullscreen content failed to show.
                                        // Make sure to set your reference to null so you don't
                                        // show it a second time.
                                        MainActivity.this.interstitialAd = null;
                                    }

                                    @Override
                                    public void onAdShowedFullScreenContent() {
                                        // Called when fullscreen content is shown.
                                    }
                                });
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        // Handle the error
                        interstitialAd = null;

                        String error =
                                String.format(
                                        "domain: %s, code: %d, message: %s",
                                        loadAdError.getDomain(), loadAdError.getCode(), loadAdError.getMessage());
                        Toast.makeText(
                                MainActivity.this, "onAdFailedToLoad() with error: " + error, Toast.LENGTH_SHORT)
                                .show();
                    }
                });
    }



}
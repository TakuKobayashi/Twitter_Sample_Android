//  Created by Taku Kobayashi 小林 拓

package com.test.twitter;

import java.io.File;

import com.test.twitter.TwitterOAuth.OAuthResultListener;
import com.test.twitter.TwitterUploadImage.UploadListener;

import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager.LayoutParams;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

public class TwitterTestActivity extends Activity {

	private TwitterOAuth m_TwitterOAuth = null;
	private TwitterUploadImage m_TwitterUploadImage = null;
	private WebView m_WebView;
	private Button m_OAuthButton;
	private Button m_SendImageButton;

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_twitter_test);
		//起動時にはキーボードを表示させないようにする
		getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_HIDDEN);

		//Twitterとの認証ページを表示させるためのWebView
		m_WebView = (WebView) findViewById(R.id.TwitterWebView);
		m_WebView.getSettings().setJavaScriptEnabled(true);

		WebSettings webSettings = m_WebView.getSettings();
		webSettings.setSavePassword(false);

		m_WebView.setWebChromeClient(m_WebChromeClient);
		m_WebView.setWebViewClient(m_WebViewClient);
		m_WebView.setVisibility(View.INVISIBLE);

		m_TwitterOAuth = new TwitterOAuth(this);
		m_TwitterUploadImage = new TwitterUploadImage();
		m_TwitterUploadImage.setOnUploadListener(m_UploadListener);

		m_OAuthButton = (Button) findViewById(R.id.TwitterOAuthButton);
		m_SendImageButton = (Button) findViewById(R.id.TwitterSendImageButton);
		m_SendImageButton.setOnClickListener(m_UploadImageListener);

		//AccessTokenが保存されていればその値をそのまま利用する
		SharedPreferences setting = PreferenceManager.getDefaultSharedPreferences(this);
		String AccessToken = setting.getString("AccessToken", null);
		String AccessTokenSecret = setting.getString("AccessTokenSecret", null);
		if(AccessToken != null && AccessTokenSecret != null){
			m_TwitterUploadImage.setAccessToken(AccessToken, AccessTokenSecret);
			ChangeView(true);
		}else{
			m_TwitterOAuth.setOnOAuthResultListener(m_OAuthResultListener);
			ChangeView(false);
		}
	}

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	//認証前と認証後とで表示を切り替える
	private void ChangeView(boolean bOAuthfinish){
		if(bOAuthfinish == true){
			m_OAuthButton.setText("Logout");
			m_OAuthButton.setOnClickListener(m_LogoutListener);
			m_SendImageButton.setVisibility(View.VISIBLE);
		}else{
			m_OAuthButton.setText("OAuth");
			m_OAuthButton.setOnClickListener(m_OAuthListener);
			m_SendImageButton.setVisibility(View.INVISIBLE);
		}
	}

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == Activity.RESULT_OK && m_TwitterUploadImage != null){
			//Fileは投稿する画像のファイル、第二引数(String)はツイート文
			m_TwitterUploadImage.SendImageWithTweetToTwitter(new File(UriToPath(data.getData())), "Test");
		}
	}

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	//UriをFileのPathに変換する処理
	private String UriToPath(Uri uri) {
		ContentResolver contentResolver = getContentResolver();
		Cursor cursor = contentResolver.query(uri, new String[]{MediaStore.Images.Media.DATA}, null, null, null);
		cursor.moveToFirst();
		String path = cursor.getString(0);
		cursor.close();
		return path;
	}

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	//WebViewがURLを読み込んでいる最中の処理の設定
	private WebChromeClient m_WebChromeClient = new WebChromeClient(){
		@Override
		public void onProgressChanged(WebView view, int newProgress) {
			super.onProgressChanged(view, newProgress);
			setProgress(newProgress * 100);
		}
	};

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	private WebViewClient m_WebViewClient = new WebViewClient(){

		//※Android2.*系でも常時呼ばれる
		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			if ((url != null) && (url.startsWith(TwitterConfig.CallbackUrl))) {
				m_WebView.stopLoading();
				m_WebView.setVisibility(View.INVISIBLE);
				//認証完了後にCallbackするURLをフックし、AccessTokenを取得する処理を行う
				m_TwitterOAuth.returnOAuth(url);
			}
		};

		/*
		//※Android2.*系ではフックできない可能性がある
		//返り値をtrueにするとURLを読み込む前にフックして処理を行う
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			boolean result = true;
			if ((url != null) && (url.startsWith(TwitterConfig.CallbackUrl))) {
				//認証完了後にCallbackするURLをフックし、AccessTokenを取得する処理を行う
				m_TwitterOAuth.returnOAuth(url);
			} else {
				result = super.shouldOverrideUrlLoading(view, url);
			}
			return result;
		}
		*/
	};

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	//認証処理開始
	private OnClickListener m_OAuthListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			m_OAuthButton.setClickable(false);
			m_TwitterOAuth.startOAuth();
		}
	};

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	//ログアウト処理
	private OnClickListener m_LogoutListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			m_TwitterOAuth.LogoutTwitter();
			ChangeView(false);
		}
	};

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	//twitterにアップロードする画像を取得処理
	private OnClickListener m_UploadImageListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			//ギャラリーアプリを呼び出す
			Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
			intent.setType("image/*");
			startActivityForResult(intent, 1);
		}
	};

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	private OAuthResultListener m_OAuthResultListener = new OAuthResultListener() {

		//認証ページのURLを取得した時に呼ばれる
		@Override
		public void RequestOAuthUrl(String url) {
			if(url != null){
				m_WebView.loadUrl(url);
				m_WebView.setVisibility(View.VISIBLE);
				//WebView上で入力時にキーボードを出現させるためにフォーカスをあてる。
				m_WebView.requestFocus();
			}
		}

		//認証完了後AccessToken取得完了した時に呼ばれる
		@Override
		public void OAuthResult(String token, String tokenSecret) {
			m_OAuthButton.setClickable(true);
			m_WebView.setVisibility(View.INVISIBLE);
			m_TwitterUploadImage.setAccessToken(token, tokenSecret);
			ChangeView(true);
		}

		//認証エラーが発生した時に呼ばれる
		@Override
		public void OAuthError(int StatusCode) {
			m_OAuthButton.setClickable(true);
			m_WebView.setVisibility(View.INVISIBLE);
		}
	};

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	private UploadListener m_UploadListener = new UploadListener(){

		//twitterに画像の投稿が完了した場合に呼ばれる
		@Override
		public void Success(File UploadFile, String Tweet) {

		}

		//twitterに画像の投稿処理の最中にエラーが発生した時に呼ばれる
		@Override
		public void Error(int StatusCode) {
			finish();
		}
	};

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	@Override
	protected void onDestroy() {
		super.onDestroy();
		//WebViewをメモリから解放する
		m_WebView.stopLoading();
		m_WebView.setWebChromeClient(null);
		m_WebView.setWebViewClient(null);
		m_WebView.destroy();
		m_WebView = null;
	}

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------
}

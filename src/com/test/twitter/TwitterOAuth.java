//  Created by Taku Kobayashi 小林 拓

package com.test.twitter;

import java.util.EventListener;
import twitter4j.TwitterException;
import twitter4j.auth.AccessToken;
import twitter4j.auth.OAuthAuthorization;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

import android.net.Uri;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.content.SharedPreferences;

//Twitterへの認証処理
public class TwitterOAuth{

	private static final String OAUTH_VERIFIER = "oauth_verifier";
	private Activity m_Activity;
	private Handler m_Handler;
	private OAuthResultListener m_OAuthResultListener = null;
	private OAuthAuthorization m_OAuthAuthorization;
	private int m_ErrorStatusCode = 0;

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	public TwitterOAuth(Activity act){
		m_Activity = act;
		m_Handler = new Handler();
	}

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	private Configuration settingConsumerKey(){
		ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setDebugEnabled(true);
		cb.setOAuthConsumerKey(TwitterConfig.ConsumerKey);
		cb.setOAuthConsumerSecret(TwitterConfig.ConsumerSecret);
		return cb.build();
	}

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	//認証ページにURLを取ってくる
	public void startOAuth(){
		m_OAuthAuthorization = new OAuthAuthorization(settingConsumerKey());
		// これが無いとgetOAuthRequestToken()で例外が発生するらしい
		m_OAuthAuthorization.setOAuthAccessToken(null);

		//マルチスレッドにしないとAndroid3.0以降の端末では例外処理が発生する
		new Thread(new Runnable() {

			private String url = null;

			@Override
			public void run() {
				//アプリの認証オブジェクト作成
				RequestToken req = null;
				try {
					req = m_OAuthAuthorization.getOAuthRequestToken();
				} catch (TwitterException e) {
					OAuthErrorHandler(e.getStatusCode());
				}
				if(req != null){
					url = req.getAuthorizationURL();
				}else{
					//エラー原因が不明
					OAuthErrorHandler(TwitterConfig.UNKOWN_ERROR_STATUS_CODE);
				}
				//メインスレッドに処理を投げる
				m_Handler.post(new Runnable() {
					@Override
					public void run() {
						if(m_OAuthResultListener != null){
							m_OAuthResultListener.RequestOAuthUrl(url);
						}
					}
				});
			}
		}).start();
	}

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	//認証完了後AccessTokenを取得する処理
	public void returnOAuth(final String url){

		//マルチスレッドにしないとAndroid3.0以降の端末では例外処理が発生する
		new Thread(new Runnable() {

			private AccessToken accessToken = null;

			@Override
			public void run() {
				try {
					//accessTokenを取得するためのパラメータ(oauth_verifier)がCallBackURLの中にあるのでそれの値を取ってきて認証を行う
					Uri uri = Uri.parse(url);
					String oauth_verifier = uri.getQueryParameter(OAUTH_VERIFIER);
					//AccessTokenを取得する
					accessToken = m_OAuthAuthorization.getOAuthAccessToken(oauth_verifier);
					RecordAccessToken(accessToken.getToken(),accessToken.getTokenSecret());
					//メインスレッドに処理を投げる
					m_Handler.post(new Runnable() {
						@Override
						public void run() {
							if(m_OAuthResultListener != null){
								m_OAuthResultListener.OAuthResult(accessToken.getToken(), accessToken.getTokenSecret());
							}
						}
					});
				} catch (TwitterException e) {
					OAuthErrorHandler(e.getStatusCode());
				}
			}
		}).start();
	}

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	//ログアウト(登録されているAccessTokenを削除する)
	public void LogoutTwitter(){
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(m_Activity);
		SharedPreferences.Editor editor = sp.edit();
		editor.remove("AccessToken");
		editor.remove("AccessTokenSecret");
		editor.commit();
	}

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	//取得したAccessTokenをローカルに保存する
	private void RecordAccessToken(String AccessToken,String AccessTokenSecret){
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(m_Activity);
		SharedPreferences.Editor editor = sp.edit();
		editor.putString("AccessToken", AccessToken);
		editor.putString("AccessTokenSecret", AccessTokenSecret);
		editor.commit();
	}

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	//認証処理中にエラーが出たときに行う処理
	private void OAuthErrorHandler(int StatusCode){
		m_ErrorStatusCode = StatusCode;
		//メインスレッドに処理を投げる
		m_Handler.post(new Runnable() {
			@Override
			public void run() {
				if(m_OAuthResultListener != null){
					m_OAuthResultListener.OAuthError(m_ErrorStatusCode);
				}
			}
		});
	}

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	/**
	 * リスナーを追加する
	 */
	public void setOnOAuthResultListener(OAuthResultListener listener){
		m_OAuthResultListener = listener;
	}

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	/**
	 * リスナーを削除する
	 */
	public void removeListener(){
		m_OAuthResultListener = null;
	}

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	//処理が終わったことを通知する独自のリスナーを作成
	public interface OAuthResultListener extends EventListener {

		//Twitterの認証ページのURLを取得した場合に呼ばれる
		public void RequestOAuthUrl(String url);

		//Twitterの認証時にエラーが発生した場合に呼ばれる
		public void OAuthError(int StatusCode);

		//Twitterの認証処理が完了し、AccessTokenが取得できる時に呼ばれる
		public void OAuthResult(String token,String tokenSecret);

	}

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

}

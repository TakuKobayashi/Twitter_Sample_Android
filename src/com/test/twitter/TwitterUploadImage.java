//  Created by Taku Kobayashi 小林 拓

package com.test.twitter;

import java.io.File;
import java.util.EventListener;

import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;
import twitter4j.media.MediaProvider;
import android.os.Handler;

public class TwitterUploadImage{

	private Twitter m_Twitter;
	private File m_UploadFile;
	private String m_Tweet;
	private Handler m_Handler;
	private UploadListener m_UploadListener = null;

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	public TwitterUploadImage(){
		m_Handler = new Handler();
	}

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	//AccessTokenの設定
	public void setAccessToken(String AccessToken,String AccessTokenSecret){
		m_Twitter = new TwitterFactory(settingAccessToken(AccessToken,AccessTokenSecret)).getInstance();
	}

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	private Configuration settingAccessToken(String AccessToken,String AccessTokenSecret){
		ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setDebugEnabled(true);
		cb.setOAuthConsumerKey(TwitterConfig.ConsumerKey);
		cb.setOAuthConsumerSecret(TwitterConfig.ConsumerSecret);
		cb.setOAuthAccessToken(AccessToken);
		cb.setOAuthAccessTokenSecret(AccessTokenSecret);
		cb.setMediaProvider(MediaProvider.TWITTER.toString());
		return cb.build();
	}

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	//Twitpicに投稿する場合の設定処理
	/*
	private Configuration settingAccessToken(String AccessToken,String AccessTokenSecret){
		ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setDebugEnabled(true);
		cb.setOAuthConsumerKey(TwitterConfig.ConsumerKey);
		cb.setOAuthConsumerSecret(TwitterConfig.ConsumerSecret);
		cb.setOAuthAccessToken(AccessToken);
		cb.setOAuthAccessTokenSecret(AccessTokenSecret);
		cb.setMediaProvider(MediaProvider.TWITPIC.toString());
		cb.setMediaProviderAPIKey(TwitterConfig.TwitPicAPIKey);
		return cb.build();
	}
	*/

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	public void SendImageWithTweetToTwitter(File imageFile,String Tweet){
		m_UploadFile = imageFile;
		m_Tweet = Tweet;

		//マルチスレッドにしないとAndroid3.0以降の端末では例外処理が発生する
		new Thread(new Runnable() {

			private int StatusCode = 0;

			@Override
			public void run() {
				try {
					//ツイートする内容の設定
					StatusUpdate status = new StatusUpdate(m_Tweet);
					//投稿する画像ファイルの設定
					status.media(m_UploadFile);
					//twitterと通信し、ツイートと画像を投稿する
					m_Twitter.updateStatus(status);

					//Twitpicを利用する場合
					//String TwipicURL = m_ImageUpload.upload(m_UploadFile);
					//m_Tweet += " "+TwipicURL;
					//m_Twitter.updateStatus(m_Tweet);

					//メインスレッドに処理を投げる
					m_Handler.post(new Runnable() {
						@Override
						public void run() {
							if(m_UploadListener != null){
								m_UploadListener.Success(m_UploadFile, m_Tweet);
							}
						}
					});
				} catch (TwitterException e) {
					StatusCode = e.getStatusCode();
					//メインスレッドに処理を投げる
					m_Handler.post(new Runnable() {
						@Override
						public void run() {
							if(m_UploadListener != null){
								m_UploadListener.Error(StatusCode);
							}
						}
					});
				}
			}
		}).start();
	}

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	/**
	 * リスナーを追加する
	 */
	public void setOnUploadListener(UploadListener listener){
		m_UploadListener = listener;
	}

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	/**
	 * リスナーを削除する
	 */
	public void removeListener(){
		m_UploadListener = null;
	}

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	//処理が終わったことを通知する独自のリスナーを作成
	public interface UploadListener extends EventListener {

		//Twitterへの画像の投稿が完了した場合に呼ばれる
		public void Success(File UploadFile,String Tweet);

		//Twitterへの画像の投稿に失敗した場合に呼ばれる
		public void Error(int StatusCode);

	}

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

}

package admobex;

import android.provider.Settings.Secure;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.util.Log;
import java.util.Date;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.google.android.gms.ads.appopen.AppOpenAd.AppOpenAdLoadCallback;

import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.ConsentDebugSettings;
import com.google.android.ump.ConsentForm;
import com.google.android.ump.FormError;
import com.google.android.ump.UserMessagingPlatform;

import org.haxe.extension.Extension;
import org.haxe.lime.HaxeObject;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//https://developers.google.com/admob/android/quick-start?hl=en

public class AdmobEx extends Extension
{
	public static final String INIT_OK = "INIT_OK";
	//public static final String INIT_FAIL = "INIT_FAIL";
	public static final String CONSENT_FAIL = "CONSENT_FAIL";
	public static final String BANNER_LOADED = "BANNER_LOADED";
	public static final String BANNER_FAILED_TO_LOAD = "BANNER_FAILED_TO_LOAD";
	public static final String BANNER_OPENED = "BANNER_OPENED";
	public static final String BANNER_CLICKED = "BANNER_CLICKED";
	public static final String BANNER_CLOSED = "BANNER_CLOSED";
	public static final String INTERSTITIAL_LOADED = "INTERSTITIAL_LOADED";
	public static final String INTERSTITIAL_FAILED_TO_LOAD = "INTERSTITIAL_FAILED_TO_LOAD";
	public static final String INTERSTITIAL_DISMISSED = "INTERSTITIAL_DISMISSED";
	public static final String INTERSTITIAL_FAILED_TO_SHOW = "INTERSTITIAL_FAILED_TO_SHOW";
	public static final String INTERSTITIAL_SHOWED = "INTERSTITIAL_SHOWED";
	public static final String REWARDED_LOADED = "REWARDED_LOADED";
	public static final String REWARDED_FAILED_TO_LOAD = "REWARDED_FAILED_TO_LOAD";
	public static final String REWARDED_DISMISSED = "REWARDED_DISMISSED";
	public static final String REWARDED_FAILED_TO_SHOW = "REWARDED_FAILED_TO_SHOW";
	public static final String REWARDED_SHOWED = "REWARDED_SHOWED";
	public static final String REWARDED_EARNED = "REWARDED_EARNED";
	public static final String WHAT_IS_GOING_ON = "WHAT_IS_GOING_ON";
	public static final String APPOPEN_FAILED_TO_SHOW = "APPOPEN_FAILED_TO_SHOW";
	public static final String APPOPEN_SHOWED = "APPOPEN_SHOWED";
	public static final String APPOPEN_LOADED = "APPOPEN_LOADED";
	public static final String APPOPEN_FAILED_TO_LOAD = "APPOPEN_FAILED_TO_LOAD";
	public static final String APPOPEN_DISMISSED = "APPOPEN_DISMISSED";
	
	private static final int BANNER_SIZE_ADAPTIVE = 0; //Anchored adaptive, somewhat default now (a replacement for SMART_BANNER); banner width is fullscreen, height calculated acordingly (might not work well with landscape orientation)
	private static final int BANNER_SIZE_BANNER = 1; //320x50
	private static final int BANNER_SIZE_FLUID = 2; //A dynamically sized banner that matches its parent's width and expands/contracts its height to match the ad's content after loading completes.
	private static final int BANNER_SIZE_FULL_BANNER = 3; //468x60
	private static final int BANNER_SIZE_LARGE_BANNER = 4; //320x100
	private static final int BANNER_SIZE_LEADERBOARD = 5; //728x90
	private static final int BANNER_SIZE_MEDIUM_RECTANGLE = 6; //300x250
	private static final int BANNER_SIZE_WIDE_SKYSCRAPER = 7; //160x600
	
	private static int _inited = 0;
	private static AdView _banner = null;
	private static RelativeLayout _rl = null;
	private static AdSize _bannerSize = null;
	private static InterstitialAd _interstitial = null;
	private static RewardedAd _rewarded = null;
	private static AppOpenAd _appOpenAd = null;
	private static String _appOpenId = "";
    private static boolean _isLoadingAppOpenAd = false;
    private static boolean _isShowingAppOpenAd = false;
    private static String LOG_TAG = "Admobex";

    /** Keep track of the time an app open ad is loaded to ensure you don't show an expired ad. */
    private static long loadTime = 0;
	
	private static ConsentInformation consentInformation = null;

	private static HaxeObject _callback = null;

	public static void init(final boolean testingAds, final boolean childDirected, final boolean enableRDP, HaxeObject callback)
	{
		_callback = callback;
		
		mainActivity.runOnUiThread(new Runnable()
		{
			public void run()
			{
				//> I'm copy/pasting from here, and this code has lots of bugs, read more on how to debug it on your device: https://developers.google.com/admob/android/privacy
				//>> use this to debug GDPR
				/*ConsentDebugSettings debugSettings = new ConsentDebugSettings.Builder(mainContext)
					.setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
					.addTestDeviceHashedId("[TEST_DEVICE_ID]")
					.build();

				ConsentRequestParameters params = new ConsentRequestParameters
					.Builder()
					.setConsentDebugSettings(debugSettings)
					.build();*/
				//<<
				
				// Set tag for under age of consent. false means users are not under age
				// of consent.
				//>> don't use this if debugging GDPR
				ConsentRequestParameters params = new ConsentRequestParameters
					.Builder()
					.setTagForUnderAgeOfConsent(childDirected)
					.build();
				//<<

				consentInformation = UserMessagingPlatform.getConsentInformation(mainContext);
				consentInformation.requestConsentInfoUpdate
				(
					mainActivity,
					params,
					(ConsentInformation.OnConsentInfoUpdateSuccessListener) () ->
					{
						UserMessagingPlatform.loadAndShowConsentFormIfRequired
						(
							mainActivity,
							(ConsentForm.OnConsentFormDismissedListener) loadAndShowError ->
							{
								if(loadAndShowError != null) //don't know the reason, but initialize admob anyway
								{
									// Consent gathering failed.
									//Log.w("AdmobEx", String.format("here %s: %s", loadAndShowError.getErrorCode(), loadAndShowError.getMessage()));
									_callback.call("onStatus", new Object[] {CONSENT_FAIL, String.format("%s: %s", loadAndShowError.getErrorCode(), loadAndShowError.getMessage())});
								}

								// Consent has been gathered.
								//Log.w("AdmobEx", String.format("Consent and privacy status: %s, %s", consentInformation.getConsentStatus(), consentInformation.getPrivacyOptionsRequirementStatus()));
								initMobileAds(testingAds, childDirected, enableRDP);
							}
						);
					},
					(ConsentInformation.OnConsentInfoUpdateFailureListener) requestConsentError -> //this can happen when there is no internet, initialize admob anyway
					{
						// Consent gathering failed.
						//Log.w("AdmobEx", String.format("or here %s: %s", requestConsentError.getErrorCode(), requestConsentError.getMessage()));
						_callback.call("onStatus", new Object[] {CONSENT_FAIL, String.format("%s: %s", requestConsentError.getErrorCode(), requestConsentError.getMessage())});
						
						initMobileAds(testingAds, childDirected, enableRDP);
					}
				);
				
				// Check if you can initialize the Google Mobile Ads SDK in parallel
				// while checking for new consent information. Consent obtained in
				// the previous session can be used to request ads.
				if(consentInformation.canRequestAds()) //this part makes no sense, cause if it's true, then OnConsentInfoUpdateSuccessListener->OnConsentFormDismissedListener will be ok too and then init will be called twice
				{
					//Log.w("AdmobEx", String.format("Consent and privacy status 2: %s, %s", consentInformation.getConsentStatus(), consentInformation.getPrivacyOptionsRequirementStatus()));
					initMobileAds(testingAds, childDirected, enableRDP);
				}
				//<
			}
		});
	}
	
	public static void initMobileAds(final boolean testingAds, final boolean childDirected, final boolean enableRDP)
	{
		//Log.d("AdmobEx", "init...");
		if(_inited == 1) //to prevent repeat initialization
			return;
			
		_inited = 1;
		
		RequestConfiguration.Builder configuration = new RequestConfiguration.Builder();

		//> set testing devices
		if(testingAds)
		{
			List<String> testDeviceIds = new ArrayList<String>();
			
			testDeviceIds.add(AdRequest.DEVICE_ID_EMULATOR); //needed???
			
			String androidId = Secure.getString(mainActivity.getContentResolver(), Secure.ANDROID_ID);
			String deviceId = md5(androidId).toUpperCase();
			testDeviceIds.add(deviceId);
			
			configuration.setTestDeviceIds(testDeviceIds);
			//Log.d("AdmobEx", "TEST DEVICE ID: "+deviceId);
		}
		//<
		
		//> set COPPA
		if(childDirected)
		{
			//Log.d("AdmobEx", "Enabling COPPA support.");
			configuration.setTagForChildDirectedTreatment(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE);
		}
		//<
		
		//> set CCPA
		if(enableRDP)
		{
			//Log.d("AdmobEx", "Enabling RDP.");
			SharedPreferences sharedPref = mainActivity.getPreferences(Context.MODE_PRIVATE);
			SharedPreferences.Editor editor = sharedPref.edit();
			editor.putInt("gad_rdp", 1);
			editor.commit();
		}
		//<
		
		MobileAds.setRequestConfiguration(configuration.build());

		MobileAds.initialize(mainContext, new OnInitializationCompleteListener()
		{
			@Override
			public void onInitializationComplete(InitializationStatus initializationStatus)
			{
				Log.d("AdmobEx", MobileAds.getVersion().toString());
				//Log.d("AdmobEx", INIT_OK);
				_callback.call("onStatus", new Object[] {INIT_OK, ""});
			}
		});
	}
	
	public static void showBanner(final String id, final int size, final int align)
	{
		if(_banner != null)
		{
			//Log.d("AdmobEx", BANNER_FAILED_TO_LOAD+"Hide previous banner first!");
			_callback.call("onStatus", new Object[] {BANNER_FAILED_TO_LOAD, "Hide previous banner first!"});
			return;
		}
		
		mainActivity.runOnUiThread(new Runnable()
		{
			public void run()
			{
				_rl = new RelativeLayout(mainActivity);
				_rl.setGravity(align);
			
				_banner = new AdView(mainActivity);
				_banner.setAdUnitId(id);
				
				AdSize adSize = AdSize.INVALID;
				switch(size)
				{
					case BANNER_SIZE_ADAPTIVE: adSize = getAdSize(); break; //get right size for adaptive banner
					case BANNER_SIZE_BANNER: adSize = AdSize.BANNER; break;
					case BANNER_SIZE_FLUID: adSize = AdSize.FLUID; break;
					case BANNER_SIZE_FULL_BANNER: adSize = AdSize.FULL_BANNER; break;
					case BANNER_SIZE_LARGE_BANNER: adSize = AdSize.LARGE_BANNER; break;
					case BANNER_SIZE_LEADERBOARD: adSize = AdSize.LEADERBOARD; break;
					case BANNER_SIZE_MEDIUM_RECTANGLE: adSize = AdSize.MEDIUM_RECTANGLE; break;
					case BANNER_SIZE_WIDE_SKYSCRAPER: adSize = AdSize.WIDE_SKYSCRAPER; break;
				}
				
				_banner.setAdSize(adSize);
				_banner.setAdListener(new AdListener() {
					@Override
					public void onAdLoaded() {
						// Code to be executed when an ad finishes loading.
						//Log.d("AdmobEx", BANNER_LOADED);
						_callback.call("onStatus", new Object[] {BANNER_LOADED, ""});
						_banner.setVisibility(View.VISIBLE); //To fix this problem, if it is still valid: https://groups.google.com/forum/#!topic/google-admob-ads-sdk/avwVXvBt_sM
					}

					@Override
					public void onAdFailedToLoad(LoadAdError adError) {
						// Code to be executed when an ad request fails.
						//Log.d("AdmobEx", BANNER_FAILED_TO_LOAD+adError.toString());
						_callback.call("onStatus", new Object[] {BANNER_FAILED_TO_LOAD, adError.toString()});
					}

					@Override
					public void onAdOpened() {
						// Code to be executed when an ad opens an overlay that
						// covers the screen.
						//Log.d("AdmobEx", BANNER_OPENED);
						_callback.call("onStatus", new Object[] {BANNER_OPENED, ""}); //ie shown
					}

					@Override
					public void onAdClicked() {
						// Code to be executed when the user clicks on an ad.
						//Log.d("AdmobEx", BANNER_CLICKED);
						_callback.call("onStatus", new Object[] {BANNER_CLICKED, ""});
					}

					@Override
					public void onAdClosed() {
						// Code to be executed when the user is about to return
						// to the app after tapping on an ad.
						//Log.d("AdmobEx", BANNER_CLOSED);
						_callback.call("onStatus", new Object[] {BANNER_CLOSED, ""});
					}
				});
				
				RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
				mainActivity.addContentView(_rl, params);
				_rl.addView(_banner);
				_rl.bringToFront();
				
				AdRequest adRequest = new AdRequest.Builder().build();
				_banner.loadAd(adRequest);
			}
		});
	}
	
	public static void hideBanner()
	{
		if(_banner != null)
		{
			mainActivity.runOnUiThread(new Runnable()
			{
				public void run()
				{
					_banner.setVisibility(View.INVISIBLE);
					ViewGroup parent = (ViewGroup) _rl.getParent();
					parent.removeView(_rl);
					_rl.removeView(_banner);
					_banner.destroy();
					_banner = null;
					_rl = null;
				}
			});
		}
	}

	public static void loadAppOpen(final String id)
	{
		_appOpenId = id;
		mainActivity.runOnUiThread(new Runnable()
		{
			public void run()
			{
				loadAppOpenAd(id);
			}
			
		});
	}

	public static void showAppOpen()
	{
		if(_appOpenAd != null)
		{
			mainActivity.runOnUiThread(new Runnable()
			{
				public void run()
				{
					showAppOpenAdIfAvailable();
					
				}
			});
		}
		else
		{
			//Log.d("AdmobEx", INTERSTITIAL_FAILED_TO_SHOW+"You need to load interstitial ad first!");
			_callback.call("onStatus", new Object[] {APPOPEN_FAILED_TO_SHOW, "You need to load app open ad first!"});
		}
	}
	
	public static void loadInterstitial(final String id)
	{
		mainActivity.runOnUiThread(new Runnable()
		{
			public void run()
			{
				AdRequest adRequest = new AdRequest.Builder().build();

				InterstitialAd.load(mainActivity, id, adRequest, new InterstitialAdLoadCallback()
				{
					@Override
					public void onAdLoaded(@NonNull InterstitialAd interstitialAd)
					{
						// The _interstitial reference will be null until
						// an ad is loaded.
						_interstitial = interstitialAd;
						_interstitial.setFullScreenContentCallback(new FullScreenContentCallback(){
							@Override
							public void onAdDismissedFullScreenContent() {
								// Called when fullscreen content is dismissed.
								//Log.d("AdmobEx", INTERSTITIAL_DISMISSED);
								_callback.call("onStatus", new Object[] {INTERSTITIAL_DISMISSED, ""});
							}

							@Override
							public void onAdFailedToShowFullScreenContent(AdError adError) {
								// Called when fullscreen content failed to show.
								//Log.d("AdmobEx", INTERSTITIAL_FAILED_TO_SHOW+adError.toString());
								_callback.call("onStatus", new Object[] {INTERSTITIAL_FAILED_TO_SHOW, adError.toString()});
							}

							@Override
							public void onAdShowedFullScreenContent() {
								// Called when fullscreen content is shown.
								// Make sure to set your reference to null so you don't
								// show it a second time.
								//Log.d("AdmobEx", INTERSTITIAL_SHOWED);
								_interstitial = null;
								_callback.call("onStatus", new Object[] {INTERSTITIAL_SHOWED, ""});
							}
						});
						
						//Log.d("AdmobEx", INTERSTITIAL_LOADED);
						_callback.call("onStatus", new Object[] {INTERSTITIAL_LOADED, ""});
					}

					@Override
					public void onAdFailedToLoad(@NonNull LoadAdError loadAdError)
					{
						// Handle the error
						//Log.d("AdmobEx", INTERSTITIAL_FAILED_TO_LOAD+loadAdError.getMessage());
						_interstitial = null;
						_callback.call("onStatus", new Object[] {INTERSTITIAL_FAILED_TO_LOAD, loadAdError.getMessage()});
					}
				});
			}
		});
	}

	public static void showInterstitial()
	{
		if(_interstitial != null)
		{
			mainActivity.runOnUiThread(new Runnable()
			{
				public void run()
				{
					_interstitial.show(mainActivity);
					//_callback.call("onStatus", new Object[] {INTERSTITIAL_SHOWED, ""}); //no need here, because called at onAdShowedFullScreenContent
				}
			});
		}
		else
		{
			//Log.d("AdmobEx", INTERSTITIAL_FAILED_TO_SHOW+"You need to load interstitial ad first!");
			_callback.call("onStatus", new Object[] {INTERSTITIAL_FAILED_TO_SHOW, "You need to load interstitial ad first!"});
		}
	}
	
	public static void loadRewarded(final String id)
	{
		mainActivity.runOnUiThread(new Runnable()
		{
			public void run()
			{
				AdRequest adRequest = new AdRequest.Builder().build();

				RewardedAd.load(mainActivity, id, adRequest, new RewardedAdLoadCallback() {
					@Override
					public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
						_rewarded = rewardedAd;
						_rewarded.setFullScreenContentCallback(new FullScreenContentCallback(){
							@Override
							public void onAdDismissedFullScreenContent() {
								// Called when fullscreen content is dismissed.
								_callback.call("onStatus", new Object[] {REWARDED_DISMISSED, ""});
							}

							@Override
							public void onAdFailedToShowFullScreenContent(AdError adError) {
								// Called when fullscreen content failed to show.
								_callback.call("onStatus", new Object[] {REWARDED_FAILED_TO_SHOW, adError.toString()});
							}

							@Override
							public void onAdShowedFullScreenContent() {
								// Called when fullscreen content is shown.
								// Make sure to set your reference to null so you don't
								// show it a second time.
								_rewarded = null;
								_callback.call("onStatus", new Object[] {REWARDED_SHOWED, ""});
							}
						});
						
						_callback.call("onStatus", new Object[] {REWARDED_LOADED, ""});
					}

					@Override
					public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
						// Handle the error
						_rewarded = null;
						_callback.call("onStatus", new Object[] {REWARDED_FAILED_TO_LOAD, loadAdError.getMessage()});
					}
				});
			}
		});
	}

	public static void showRewarded()
	{
		if(_rewarded != null)
		{
			mainActivity.runOnUiThread(new Runnable()
			{
				public void run()
				{
					_rewarded.show(mainActivity, new OnUserEarnedRewardListener()
					{
						@Override
						public void onUserEarnedReward(@NonNull RewardItem rewardItem)
						{
							// Handle the reward.
							int rewardAmount = rewardItem.getAmount();
							String rewardType = rewardItem.getType();
							_callback.call("onStatus", new Object[] {REWARDED_EARNED, rewardType+":"+String.valueOf(rewardAmount)});
						}
					});
					//_callback.call("onStatus", new Object[] {REWARDED_SHOWED, ""}); //no need here, because called at onAdShowedFullScreenContent
				}
			});
		}
		else
			_callback.call("onStatus", new Object[] {REWARDED_FAILED_TO_SHOW, "You need to load rewarded ad first!"});
	}
	
	public static void setVolume(final float vol)
	{
		mainActivity.runOnUiThread(new Runnable()
		{
			public void run()
			{
				if(vol >= 0)
				{
					MobileAds.setAppMuted(false);
					MobileAds.setAppVolume(vol);
				}
				else //muted
					MobileAds.setAppMuted(true);
			}
		});
	}
	
	//https://support.google.com/admob/answer/9760862?hl=en&ref_topic=9756841
	public static int hasConsentForPuprpose(final int purpose)
	{
		//copy/pasting from here: https://developers.google.com/admob/android/privacy/gdpr
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mainContext);
		// Example value: "1111111111"
		String purposeConsents = sharedPref.getString("IABTCF_PurposeConsents", "");
		// Purposes are zero-indexed. Index 0 contains information about Purpose 1.
		if(purposeConsents.length() > purpose)
		{
			//String purposeOneString = purposeConsents.charAt(0); //this line copied from google docs throws error message on build, wtf?
			int hasorwhat = Character.getNumericValue(purposeConsents.charAt(purpose));
			return hasorwhat;
		}
		
		return -1;
	}
	
	public static String getConsent()
	{
		//copy/pasting from here: https://developers.google.com/admob/android/privacy/gdpr
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mainContext);
		// Example value: "1111111111"
		String purposeConsents = sharedPref.getString("IABTCF_PurposeConsents", "");
		
		return purposeConsents;
	}
	
	public static int isPrivacyOptionsRequired()
	{
		if(consentInformation != null && consentInformation.getPrivacyOptionsRequirementStatus() == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED)
			return 1;
	
		return 0;
	}
	
	public static void showPrivacyOptionsForm()
	{
		mainActivity.runOnUiThread(new Runnable()
		{
			public void run()
			{
				// Present the privacy options form when a user interacts with
				// your privacy settings button.
				//Log.d("AdmobEx", "showPrivacyOptionsForm");
				UserMessagingPlatform.showPrivacyOptionsForm
				(
						mainActivity,
						formError ->
						{
							if(formError != null)
							{
								_callback.call("onStatus", new Object[] {CONSENT_FAIL, String.format("%s: %s", formError.getErrorCode(), formError.getMessage())});
							}
						}
				);
			}
		});
	}
	
	//> copy/paste from https://developers.google.com/admob/android/banner/anchored-adaptive
	private static AdSize getAdSize() {
		// Step 2 - Determine the screen width (less decorations) to use for the ad width.
		Display display = mainActivity.getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);

		float widthPixels = outMetrics.widthPixels;
		float density = outMetrics.density;

		int adWidth = (int) (widthPixels / density);

		// Step 3 - Get adaptive ad size and return for setting on the ad view.
		return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(mainContext, adWidth);
	}
	//<
	
	//> from previous version of extension
	private static String md5(String s) {
		MessageDigest digest;
		try  {
			digest = MessageDigest.getInstance("MD5");
			digest.update(s.getBytes(),0,s.length());
			String hexDigest = new java.math.BigInteger(1, digest.digest()).toString(16);
			if (hexDigest.length() >= 32) return hexDigest;
			else return "00000000000000000000000000000000".substring(hexDigest.length()) + hexDigest;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}
	//<


	public interface OnShowAdCompleteListener {
    void onShowAdComplete();
  }

    

    

    /**
     * Load an ad.
     *
     * @param context the context of the activity that loads the ad
     */
    public static void loadAppOpenAd(final String adId) {
      // Do not load ad if there is an unused ad or one is already loading.
      if (_isLoadingAppOpenAd || isAppOpenAdAvailable()) {
        return;
      }

      _isLoadingAppOpenAd = true;
      AdRequest request = new AdRequest.Builder().build();
      AppOpenAd.load(
          mainActivity,
          adId,
          request,
          new AppOpenAdLoadCallback() {
            /**
             * Called when an app open ad has loaded.
             *
             * @param ad the loaded app open ad.
             */
            @Override
            public void onAdLoaded(AppOpenAd ad) {
              _appOpenAd = ad;
              _isLoadingAppOpenAd = false;
              loadTime = (new Date()).getTime();

              Log.d(LOG_TAG, "onAdLoaded.");
              _callback.call("onStatus", new Object[] {APPOPEN_LOADED, ""});
              // Toast.makeText(mainActivity, "onAdLoaded", Toast.LENGTH_SHORT).show();
            }

            /**
             * Called when an app open ad has failed to load.
             *
             * @param loadAdError the error.
             */
            @Override
            public void onAdFailedToLoad(LoadAdError loadAdError) {
              _isLoadingAppOpenAd = false;
              Log.d(LOG_TAG, "onAdFailedToLoad: " + loadAdError.getMessage());
              // Toast.makeText(mainActivity, "onAdFailedToLoad", Toast.LENGTH_SHORT).show();
              _callback.call("onStatus", new Object[] {APPOPEN_FAILED_TO_LOAD, loadAdError.toString()});
            }
          });
    }

    /** Check if ad was loaded more than n hours ago. */
    private static boolean wasLoadTimeLessThanNHoursAgo(long numHours) {
      long dateDifference = (new Date()).getTime() - loadTime;
      long numMilliSecondsPerHour = 3600000;
      return (dateDifference < (numMilliSecondsPerHour * numHours));
    }

    /** Check if ad exists and can be shown. */
    public static boolean isAppOpenAdAvailable() {
      // Ad references in the app open beta will time out after four hours, but this time limit
      // may change in future beta versions. For details, see:
      // https://support.google.com/admob/answer/9341964?hl=en
      return _appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4);
    }

    /**
     * Show the ad if one isn't already showing.
     *
     * @param activity the activity that shows the app open ad
     */
    private static void showAppOpenAdIfAvailable() {
      showAppOpenAdIfAvailable(
          mainActivity,
          new OnShowAdCompleteListener() {
            @Override
            public void onShowAdComplete() {
              // Empty because the user will go back to the activity that shows the ad.
            	_callback.call("onStatus", new Object[] {APPOPEN_SHOWED, ""});
            }
          });
    }

    /**
     * Show the ad if one isn't already showing.
     *
     * @param activity the activity that shows the app open ad
     * @param onShowAdCompleteListener the listener to be notified when an app open ad is complete
     */
    private static void showAppOpenAdIfAvailable(
        @NonNull final Activity activity,
        @NonNull OnShowAdCompleteListener onShowAdCompleteListener) {
      // If the app open ad is already showing, do not show the ad again.
      if (_isShowingAppOpenAd) {
        Log.d(LOG_TAG, "The app open ad is already showing.");
        return;
      }

      // If the app open ad is not available yet, invoke the callback then load the ad.
      if (!isAppOpenAdAvailable()) {
        Log.d(LOG_TAG, "The app open ad is not ready yet.");
        onShowAdCompleteListener.onShowAdComplete();
        if (consentInformation!= null){
        	if (consentInformation.canRequestAds()) {
	          loadAppOpenAd(_appOpenId);
	        }
        }
        
        return;
      }

      Log.d(LOG_TAG, "Will show ad.");

      _appOpenAd.setFullScreenContentCallback(
          new FullScreenContentCallback() {
            /** Called when full screen content is dismissed. */
            @Override
            public void onAdDismissedFullScreenContent() {
              // Set the reference to null so isAppOpenAdAvailable() returns false.
              _appOpenAd = null;
              _isShowingAppOpenAd = false;

              Log.d(LOG_TAG, "onAdDismissedFullScreenContent.");
              // Toast.makeText(activity, "onAdDismissedFullScreenContent", Toast.LENGTH_SHORT).show();
              _callback.call("onStatus", new Object[] {APPOPEN_DISMISSED, ""});

              onShowAdCompleteListener.onShowAdComplete();
              if (consentInformation!= null){
		        	if (consentInformation.canRequestAds()) {
			          loadAppOpenAd(_appOpenId);
			        }
		        }
            }

            /** Called when fullscreen content failed to show. */
            @Override
            public void onAdFailedToShowFullScreenContent(AdError adError) {
              _appOpenAd = null;
              _isShowingAppOpenAd = false;
              _callback.call("onStatus", new Object[] {APPOPEN_FAILED_TO_SHOW, adError.toString()});

              Log.d(LOG_TAG, "onAdFailedToShowFullScreenContent: " + adError.getMessage());
              // Toast.makeText(activity, "onAdFailedToShowFullScreenContent", Toast.LENGTH_SHORT).show();

              onShowAdCompleteListener.onShowAdComplete();
              if (consentInformation!= null){
		        	if (consentInformation.canRequestAds()) {
			          loadAppOpenAd(_appOpenId);
			        }
		        }
            }

            /** Called when fullscreen content is shown. */
            @Override
            public void onAdShowedFullScreenContent() {
              Log.d(LOG_TAG, "onAdShowedFullScreenContent.");
              // Toast.makeText(activity, "onAdShowedFullScreenContent", Toast.LENGTH_SHORT).show();
              _callback.call("onStatus", new Object[] {APPOPEN_SHOWED, ""});
            }
          });

      _isShowingAppOpenAd = true;
      _appOpenAd.show(activity);
    }
}


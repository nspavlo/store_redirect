package com.danieldallos.storeredirect;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.MethodCall;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

/**
 * StoreRedirectPlugin
 */
public class StoreRedirectPlugin implements MethodCallHandler, FlutterPlugin, ActivityAware {

  private static final String CHANNEL_NAME = "store_redirect";
  private static final String GOOGLE_PLAY_PACKAGE = "com.android.vending";
  private static final String GOOGLE_PLAY_URL_PREFIX = "https://play.google.com/store/apps/details?id=";
  private static final String MARKET_URL_PREFIX = "market://details?id=";

  private Activity activity;
  private MethodChannel methodChannel;

  @Override
  public void onAttachedToEngine(FlutterPluginBinding binding) {
    onAttachedToEngine(binding.getBinaryMessenger());
  }

  @Override
  public void onDetachedFromEngine(FlutterPluginBinding binding) {
    methodChannel.setMethodCallHandler(null);
    methodChannel = null;
  }

  private void onAttachedToEngine(BinaryMessenger messenger) {
    methodChannel = new MethodChannel(messenger, CHANNEL_NAME);
    methodChannel.setMethodCallHandler(this);
  }

  @Override
  public void onMethodCall(MethodCall call, Result result) {
    if (!call.method.equals("redirect")) {
      result.notImplemented();
      return;
    }

    if (activity == null) {
      result.error("no_activity", "Store redirect requires a foreground activity.", null);
      return;
    }

    final String appId = call.argument("android_id");
    final String appPackageName = appId != null ? appId : activity.getPackageName();

    Activity currentActivity = activity;

    Intent playStoreIntent = createPlayStoreIntent(appPackageName);
    if (canHandleIntent(playStoreIntent)) {
      currentActivity.startActivity(playStoreIntent);
      result.success(null);
      return;
    }

    Intent webIntent = createWebIntent(appPackageName);
    if (canHandleIntent(webIntent)) {
      currentActivity.startActivity(webIntent);
      result.success(null);
      return;
    }

    result.error("unavailable", "No activity found to handle store redirect.", null);
  }

  private boolean canHandleIntent(Intent intent) {
    return intent.resolveActivity(activity.getPackageManager()) != null;
  }

  @Override
  public void onAttachedToActivity(ActivityPluginBinding binding) {
    this.activity = binding.getActivity();
  }

  @Override
  public void onDetachedFromActivityForConfigChanges() {
    onDetachedFromActivity();
  }

  @Override
  public void onReattachedToActivityForConfigChanges(ActivityPluginBinding binding) {
    onAttachedToActivity(binding);
  }

  @Override
  public void onDetachedFromActivity() {
    this.activity = null;
  }

  private Intent createPlayStoreIntent(String appPackageName) {
    Intent intent = createWebIntent(appPackageName);
    intent.setPackage(GOOGLE_PLAY_PACKAGE);
    return intent;
  }

  private Intent createWebIntent(String appPackageName) {
    Intent intent = new Intent(Intent.ACTION_VIEW);
    intent.setData(Uri.parse(GOOGLE_PLAY_URL_PREFIX + appPackageName));
    return intent;
  }
}

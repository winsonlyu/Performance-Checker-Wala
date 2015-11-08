package PerfCheckerRL;

import java.util.ArrayList;

import PerfCheckerRL.checkRL.ReportItem;

public class Resource {
  public ArrayList<ResourceAPI> targetAPI = new ArrayList<ResourceAPI>();

  public Resource()
  {
    targetAPI.add(new ResourceAPI("Landroid/accounts/AccountManager", "addOnAccountsUpdatedListener(", "Landroid/accounts/AccountManager", "removeOnAccountsUpdatedListener("));
    targetAPI.add(new ResourceAPI("Landroid/net/http/AndroidHttpClient", "newInstance(", "Landroid/net/http/AndroidHttpClient", "close("));
    targetAPI.add(new ResourceAPI("Landroid/media/AudioManager", "requestAudioFocus(", "Landroid/media/AudioManager", "abandonAudioFocus("));
    targetAPI.add(new ResourceAPI("Landroid/media/AudioManager", "startBluetoothSco(", "Landroid/media/AudioManager", "stopBluetoothSco("));
    targetAPI.add(new ResourceAPI("Landroid/media/AudioManager", "loadSoundEffects(", "Landroid/media/AudioManager", "unloadSoundEffects("));
    targetAPI.add(new ResourceAPI("Landroid/media/AudioRecord", "<init>", "Landroid/media/AudioRecord", "release("));
    //targetAPI.add(new ResourceAPI("Landroid/graphics/Bitmap", "createBitmap(", "Landroid/graphics/Bitmap", "recycle("));
    //targetAPI.add(new ResourceAPI("Landroid/graphics/Bitmap", "copy(", "Landroid/graphics/Bitmap", "recycle("));
    targetAPI.add(new ResourceAPI("Landroid/bluetooth/BluetoothAdapter", "startDiscovery(", "Landroid/bluetooth/BluetoothAdapter(", "cancelDiscovery("));
    targetAPI.add(new ResourceAPI("Landroid/bluetooth/BluetoothAdapter", "getProfileProxy(", "Landroid/bluetooth/BluetoothAdapter(", "closeProfileProxy("));
    targetAPI.add(new ResourceAPI("Landroid/bluetooth/BluetoothHeadset", "startVoiceRecognition(", "Landroid/bluetooth/BluetoothHeadset", "stopVoiceRecognition("));
    targetAPI.add(new ResourceAPI("Landroid/hardware/Camera", "lock", "Landroid/hardware/Camera(", "unlock("));
    targetAPI.add(new ResourceAPI("Landroid/hardware/Camera", "open", "Landroid/hardware/Camera(", "release("));
    targetAPI.add(new ResourceAPI("Landroid/hardware/Camera", "startFaceDetection(", "Landroid/hardware/Camera", "stopFaceDetection("));
    targetAPI.add(new ResourceAPI("Landroid/hardware/Camera", "startPreview(", "Landroid/hardware/Camera", "stopPreview("));
    //targetAPI.add(new ResourceAPI("Landroid/widget/Chronometer", "start(", "Landroid/widget/Chronometer", "stop("));
    targetAPI.add(new ResourceAPI("Landroid/content/ContentResolver", "acquireContentProviderClient(", "Landroid/content/ContentProviderClient", "release("));
    targetAPI.add(new ResourceAPI("Landroid/drm/DrmManagerClient", "<init>", "Landroid/drm/DrmManagerClient", "release("));
    targetAPI.add(new ResourceAPI("Landroid/media/effect/EffectContext", "createWithCurrentGlContext(", "Landroid/media/effect/EffectContext", "release("));
    targetAPI.add(new ResourceAPI("Landroid/nfc/tech/IsoDep", "connect(", "Landroid/nfc/tech/IsoDep", "close("));
    targetAPI.add(new ResourceAPI("Landroid/app/KeyguardManager.KeyguardLock", "disableKeyguard(", "Landroid/app/KeyguardManager.KeyguardLock", "reenableKeyguard("));
    targetAPI.add(new ResourceAPI("Landroid/util/LruCache", "<init>", "Landroid/util/LruCache", "evictAll("));
    targetAPI.add(new ResourceAPI("Landroid/location/LocationManager", "requestLocationUpdate(", "Landroid/location/LocationManager", "removeUpdates("));
    targetAPI.add(new ResourceAPI("Landroid/media/MediaCodec", "createDecoderByType(", "Landroid/media/MediaCodec", "release("));
    //targetAPI.add(new ResourceAPI("Landroid/media/MediaCodec", "start(", "Landroid/media/MediaCodec", "stop("));
    targetAPI.add(new ResourceAPI("Landroid/media/MediaPlayer", "<init>", "Landroid/media/MediaPlayer", "release("));
    targetAPI.add(new ResourceAPI("Landroid/media/MediaPlayer", "create(", "Landroid/media/MediaPlayer", "release("));
    //targetAPI.add(new ResourceAPI("Landroid/media/MediaPlayer", "start(", "Landroid/media/MediaPlayer", "stop("));
    targetAPI.add(new ResourceAPI("Landroid/media/MediaRecorder", "<init>", "Landroid/media/MediaRecorder", "release("));
    //targetAPI.add(new ResourceAPI("Landroid/media/MediaRecorder", "start(", "Landroid/media/MediaRecorder", "stop("));
    targetAPI.add(new ResourceAPI("Landroid/mtp/MtpDevice", "open(", "Landroid/mtp/MtpDevice", "close("));
    targetAPI.add(new ResourceAPI("Landroid/nfc/tech/MifareClassic", "connect(", "Landroid/nfc/tech/MifareClassic", "close("));
    targetAPI.add(new ResourceAPI("Landroid/nfc/tech/MifareUltralight", "connect(", "Landroid/nfc/tech/MifareUltralight", "close("));
    targetAPI.add(new ResourceAPI("Landroid/nfc/tech/Ndef", "connect(", "Landroid/nfc/tech/Ndef", "close("));
    targetAPI.add(new ResourceAPI("Landroid/nfc/tech/NdefFormatable", "connect(", "Landroid/nfc/tech/NdefFormatable", "close("));
    targetAPI.add(new ResourceAPI("Landroid/nfc/NfcA", "connect(", "Landroid/nfc/NfcA", "close("));
    targetAPI.add(new ResourceAPI("Landroid/nfc/NfcB", "connect(", "Landroid/nfc/NfcB", "close("));
    targetAPI.add(new ResourceAPI("Landroid/nfc/NfcBarcode", "connect(", "Landroid/nfc/NfcBarcode", "close("));
    targetAPI.add(new ResourceAPI("Landroid/nfc/NfcF", "connect(", "Landroid/nfc/NfcF", "close("));
    targetAPI.add(new ResourceAPI("Landroid/nfc/NfcV", "connect(", "Landroid/nfc/NfcV", "close("));
    targetAPI.add(new ResourceAPI("Landroid/nfc/NfcAdapter", "enableForegroundDispatch(", "Landroid/nfc/NfcAdapter", "disableForegroundDispatch("));
    targetAPI.add(new ResourceAPI("Landroid/nfc/NfcAdapter", "enableForegroundNdefPush(", "Landroid/nfc/NfcAdapter", "disableForegroundNdefPush("));
    targetAPI.add(new ResourceAPI("Landroid/net/nsd/NsdManager", " registerService(", "Landroid/net/nsd/NsdManager", " unregisterService("));
    targetAPI.add(new ResourceAPI("Landroid/os/ParcelFileDescriptor", "open(", "Landroid/os/ParcelFileDescriptor", "close("));
    targetAPI.add(new ResourceAPI("Landroid/os/PowerManager$WakeLock", "acquire(", "Landroid/os/PowerManager$WakeLock", "release("));
    targetAPI.add(new ResourceAPI("Landroid/media/audiofx/PresetReverb", "setPreset(", "Landroid/media/audiofx/PresetReverb", "release("));
    targetAPI.add(new ResourceAPI("Landroid/os/RemoteCallbackList", "beginBroadcast(", "Landroid/os/RemoteCallbackList", "finishBroadcast("));
    targetAPI.add(new ResourceAPI("Landroid/os/RemoteCallbackList", " register(", "Landroid/os/RemoteCallbackList", " unregister("));
    targetAPI.add(new ResourceAPI("Landroid/database/sqlite/SQLiteClosable", "acquireReference(", "Landroid/database/sqlite/SQLiteClosable", "releaseReference("));
    targetAPI.add(new ResourceAPI("Landroid/hardware/SensorManager", " registerListener(", "Landroid/hardware/SensorManager", " unregisterListener("));
    targetAPI.add(new ResourceAPI("Landroid/widget/SlidingDrawer", "lock(", "Landroid/widget/SlidingDrawer", "unlock("));
    targetAPI.add(new ResourceAPI("Landroid/os/storage/StorageManager", "mountObb(", "Landroid/os/storage/StorageManager", "unmountObb("));
    targetAPI.add(new ResourceAPI("Landroid/view/Surface", "<init>", "Landroid/view/Surface", "release("));
    targetAPI.add(new ResourceAPI("Landroid/view/Surface", "lockCanvas(", "Landroid/view/Surface", "unlockCanvasAndPost("));
    targetAPI.add(new ResourceAPI("Landroid/view/SurfaceHolder", "addCallback(", "Landroid/view/SurfaceHolder", "removeCallback("));
    targetAPI.add(new ResourceAPI("Landroid/view/SurfaceHolder", "lockCanvas(", "Landroid/view/SurfaceHolder", "unlockCanvasAndPost("));
    targetAPI.add(new ResourceAPI("Landroid/nfc/tech/TagTechnology", "connect(", "Landroid/nfc/tech/TagTechnology", "close("));
    targetAPI.add(new ResourceAPI("Landroid/os/TokenWatcher", "acquire(", "Landroid/os/TokenWatcher", "release("));
    targetAPI.add(new ResourceAPI("Landroid/hardware/usb/UsbManager", "openDevice(", "Landroid/hardware/usb/UsbDeviceConnection", "close("));
    targetAPI.add(new ResourceAPI("Landroid/view/VelocityTracker", "obtain(", "Landroid/view/VelocityTracker", "recycle("));
    //targetAPI.add(new ResourceAPI("Landroid/os/Vibrator", "vibrate(", "Landroid/os/Vibrator", "cancel("));
    targetAPI.add(new ResourceAPI("Landroid/webkit/WebIconDatabase", "open(", "Landroid/webkit/WebIconDatabase", "close("));
    targetAPI.add(new ResourceAPI("Landroid/net/wifi/WifiManager$MulticastLock(", "acquire", "Landroid/net/wifi/WifiManager$MulticastLock", "release("));
    targetAPI.add(new ResourceAPI("Landroid/net/wifi/WifiManager$WifiLock", "acquire(", "Landroid/net/wifi/WifiManager$WifiLock", "release("));
    targetAPI.add(new ResourceAPI("Landroid/net/wifi/WifiManager", "enableNetwork(", "Landroid/net/wifi/WifiManager", "disableNetwork("));
  }; 

  
  public void checkResource(String sig)
  {
    for(ResourceAPI api: targetAPI)
    {
      api.match(sig);
    }
  }
  
  public void checkResource_w(String sig)
  {
    for(ResourceAPI api: targetAPI)
    {
      api.match_w(sig);
    }
  }
  
  public ReportItem gen_reportitem(String m)
  {
    ReportItem ri = new ReportItem(m);
    
    boolean isRL = false;
    for(ResourceAPI api: targetAPI)
    {
      if(api.count > 0)
      {
        ri.targetAPIs.add(api);
        isRL = true;
      }
    }
    
    if(isRL)
      return ri;
    else
      return null;
  }
  
  public ReportItem gen_reportitem_w(String m)
  {
    ReportItem ri = new ReportItem(m);
    
    boolean isRL = false;
    for(ResourceAPI api: targetAPI)
    {
      if(api.acquire == true && api.relesee == false)
      {
        ri.targetAPIs.add(api);
        isRL = true;
      }
    }
    
    if(isRL)
      return ri;
    else
      return null;
  }
}

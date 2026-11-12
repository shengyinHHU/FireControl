import java.nio.file.Files;
import com.ruoyi.system.fire.dahua.*;
/** Initialization only: no login, device connection, microphone, playback or audio transmission. */
public class DahuaNativeSmokeTest {
 public static void main(String[] args)throws Exception {
  var properties=new DahuaProperties();properties.setExtractDirectory(Files.createTempDirectory("dahua-native-smoke-").toString());
  var sdk=new DahuaSdk(properties);
  try{var method=DahuaSdk.class.getDeclaredMethod("runtime");method.setAccessible(true);method.invoke(sdk);System.out.println("PASS: bundled native SDK loaded and initialized; no camera connection or audio");}
  finally{sdk.close();}
 }
}

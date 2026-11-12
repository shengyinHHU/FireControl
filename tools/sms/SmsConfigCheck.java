import java.util.*;
import org.springframework.boot.env.PropertiesPropertySourceLoader;
import org.springframework.boot.context.properties.bind.*;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.FileSystemResource;
import com.ruoyi.system.fire.sms.*;

/** Uses the same properties loader and binder as the application. No HTTP requests. */
class SmsConfigCheck {
    public static void main(String[] args) throws Exception {
        var environment = new StandardEnvironment();
        for (var source : new PropertiesPropertySourceLoader().load("sms-check", new FileSystemResource(args[0]))) {
            environment.getPropertySources().addFirst(source);
        }
        var config = Binder.get(environment).bind("fire.sms", Bindable.of(FireSmsProperties.class)).get();
        new FireSmsClient(config, new tools.jackson.databind.ObjectMapper()).ready();
        if (!config.getSignature().equals("【大厂街道办事处】")) throw new AssertionError("Signature decoded incorrectly");
        System.out.println("SMS_CONFIG_OK signatureDecoded=true providerConfigValid=true liveSmsSent=false");
    }
}

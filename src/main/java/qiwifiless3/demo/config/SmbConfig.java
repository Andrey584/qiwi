package qiwifiless3.demo.config;

import jakarta.annotation.PostConstruct;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Setter
@Configuration
@ConfigurationProperties(prefix = "smb.protocol")
public class SmbConfig {

    private final static String KEY_FOR_MIN_VERSION = "jcifs.smb.client.minVersion";
    private final static String KEY_FOR_MAX_VERSION = "jcifs.smb.client.maxVersion";

    private String minVersion;
    private String maxVersion;

    private final static Map<String, String> mapProtocol = new HashMap<>() {
        {
            put("1", "SMB1");
            put("2", "SMB2");
            put("3", "SMB3");
        }
    };

    @PostConstruct
    public void setSmbProtocolVersions() {
        System.setProperty(KEY_FOR_MIN_VERSION, mapProtocol.get(minVersion));
        System.setProperty(KEY_FOR_MAX_VERSION, mapProtocol.get(maxVersion));
    }
}


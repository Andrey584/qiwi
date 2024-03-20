package spectrum.qf.config;

import jakarta.annotation.PostConstruct;
import jcifs.smb.NtlmPasswordAuthentication;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Setter
@Configuration
@ConfigurationProperties(prefix = "smb.protocol")
public class SmbConfig {

    @Value(value = "${smb.domain}")
    private String smbDomain;
    @Value(value = "${smb.username}")
    private String smbUsername;
    @Value(value = "${smb.password}")
    private String smbPassword;

    private final static String KEY_FOR_MIN_VERSION = "jcifs.smb.client.minVersion";
    private final static String KEY_FOR_MAX_VERSION = "jcifs.smb.client.maxVersion";

    private String version;

    private final static Map<String, String> mapProtocol = new HashMap<>() {
        {
            put("1", "SMB1");
            put("2", "SMB2");
            put("3", "SMB3");
        }
    };

    @Bean
    public NtlmPasswordAuthentication auth() {
        return new NtlmPasswordAuthentication(smbDomain, smbUsername, smbPassword);
    }

    @PostConstruct
    public void setSmbProtocolVersions() {
        System.setProperty(KEY_FOR_MIN_VERSION, mapProtocol.get(version));
        System.setProperty(KEY_FOR_MAX_VERSION, mapProtocol.get(version));
    }
}


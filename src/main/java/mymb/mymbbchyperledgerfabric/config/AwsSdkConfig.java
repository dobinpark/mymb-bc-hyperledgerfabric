package mymb.mymbbchyperledgerfabric.config;

import com.amazonaws.services.managedblockchain.AmazonManagedBlockchain;
import com.amazonaws.services.managedblockchain.AmazonManagedBlockchainClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AwsSdkConfig {

    @Bean
    public AmazonManagedBlockchain amazonManagedBlockchain() {
        return AmazonManagedBlockchainClientBuilder.defaultClient();
    }
}

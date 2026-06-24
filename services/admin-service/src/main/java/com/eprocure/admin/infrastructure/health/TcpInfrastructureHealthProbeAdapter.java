package com.eprocure.admin.infrastructure.health;

import com.eprocure.admin.application.port.out.InfrastructureHealthProbePort;
import com.eprocure.admin.domain.model.InfrastructureComponentHealth;
import com.eprocure.admin.domain.model.InfrastructureHealth;
import com.eprocure.admin.domain.model.ServiceStatus;
import com.eprocure.admin.infrastructure.config.AdminProperties;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

@Component
public class TcpInfrastructureHealthProbeAdapter implements InfrastructureHealthProbePort {
    private static final Logger log = LogManager.getLogger(TcpInfrastructureHealthProbeAdapter.class);

    private final AdminProperties properties;
    private final int timeoutMillis;

    public TcpInfrastructureHealthProbeAdapter(AdminProperties properties) {
        this.properties = properties;
        this.timeoutMillis = Math.toIntExact(Math.min(properties.getHealthTimeout().toMillis(), Integer.MAX_VALUE));
    }

    @Override
    public InfrastructureHealth check() {
        return new InfrastructureHealth(
                check(properties.getInfrastructure().getPostgresql()),
                check(properties.getInfrastructure().getRedis()),
                check(properties.getInfrastructure().getKafka()));
    }

    private InfrastructureComponentHealth check(AdminProperties.Component component) {
        if (component.getHost() == null || component.getHost().isBlank() || component.getPort() <= 0) {
            return new InfrastructureComponentHealth(component.getName(), ServiceStatus.UNKNOWN, Map.of());
        }
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(component.getHost(), component.getPort()), timeoutMillis);
            return new InfrastructureComponentHealth(component.getName(), ServiceStatus.UP, Map.of());
        } catch (Exception exception) {
            log.debug("[ACTION] Infrastructure health probe failed | component={} | reason={}",
                    component.getName(),
                    exception.getClass().getSimpleName());
            return new InfrastructureComponentHealth(component.getName(), ServiceStatus.DOWN, Map.of());
        }
    }
}

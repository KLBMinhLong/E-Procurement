package com.eprocure.iam.application.usecase;

import com.eprocure.iam.application.port.out.PublicKeyInfo;
import com.eprocure.iam.application.port.out.PublicKeyProviderPort;
import org.springframework.stereotype.Service;

@Service
public class GetPublicKeyUseCase {
    private final PublicKeyProviderPort publicKeyProviderPort;

    public GetPublicKeyUseCase(PublicKeyProviderPort publicKeyProviderPort) {
        this.publicKeyProviderPort = publicKeyProviderPort;
    }

    public PublicKeyInfo execute() {
        return publicKeyProviderPort.current();
    }
}

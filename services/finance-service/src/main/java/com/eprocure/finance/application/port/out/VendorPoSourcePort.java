package com.eprocure.finance.application.port.out;

import com.eprocure.finance.application.service.VendorPoSource;
import java.util.UUID;

public interface VendorPoSourcePort {
    VendorPoSource fetch(UUID vendorId);
}

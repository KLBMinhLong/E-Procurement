package com.eprocure.vendor.application.port.in;

import java.util.UUID;

public record CloseRfqCommand(UUID actorId, UUID rfqId) {
}

package com.eprocure.vendor.application.port.in;

import java.util.List;
import java.util.UUID;

public record CreateVendorCommand(
        UUID actorId,
        String name,
        String taxCode,
        String email,
        String phone,
        String addressStreet,
        String addressDistrict,
        String addressCity,
        String addressCountry,
        List<String> categories,
        List<VendorContactCommand> contacts,
        String notes) {

    public CreateVendorCommand {
        categories = List.copyOf(categories == null ? List.of() : categories);
        contacts = List.copyOf(contacts == null ? List.of() : contacts);
    }
}

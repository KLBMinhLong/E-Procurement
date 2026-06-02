package com.eprocure.vendor.presentation.mapper;

import com.eprocure.vendor.application.port.in.ApproveVendorCommand;
import com.eprocure.vendor.application.port.in.CreateVendorCommand;
import com.eprocure.vendor.application.port.in.ListVendorsQuery;
import com.eprocure.vendor.application.port.in.VendorContactCommand;
import com.eprocure.vendor.application.service.VendorAddressView;
import com.eprocure.vendor.application.service.VendorContactView;
import com.eprocure.vendor.application.service.VendorDetailView;
import com.eprocure.vendor.application.service.VendorScorecardView;
import com.eprocure.vendor.application.service.VendorSummaryView;
import com.eprocure.vendor.common.security.UserPrincipal;
import com.eprocure.vendor.domain.model.VendorStatus;
import com.eprocure.vendor.presentation.request.ApproveVendorRequest;
import com.eprocure.vendor.presentation.request.CreateVendorRequest;
import com.eprocure.vendor.presentation.request.VendorAddressRequest;
import com.eprocure.vendor.presentation.request.VendorContactRequest;
import com.eprocure.vendor.presentation.response.VendorAddressResponse;
import com.eprocure.vendor.presentation.response.VendorContactResponse;
import com.eprocure.vendor.presentation.response.VendorDetailResponse;
import com.eprocure.vendor.presentation.response.VendorScorecardResponse;
import com.eprocure.vendor.presentation.response.VendorSummaryResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class VendorPresentationMapper {

    public ListVendorsQuery toListQuery(
            UserPrincipal principal,
            VendorStatus status,
            String category,
            Boolean onAvlOnly,
            String query,
            int page,
            int size,
            String sort) {
        return new ListVendorsQuery(
                principal.getId(),
                status,
                category,
                onAvlOnly,
                query,
                page,
                size,
                sort);
    }

    public CreateVendorCommand toCreateCommand(UserPrincipal principal, CreateVendorRequest request) {
        VendorAddressRequest address = request.address();
        return new CreateVendorCommand(
                principal.getId(),
                request.name(),
                request.taxCode(),
                request.email(),
                request.phone(),
                address == null ? null : address.street(),
                address == null ? null : address.district(),
                address == null ? null : address.city(),
                address == null ? null : address.country(),
                request.categories(),
                toContactCommands(request.contacts()),
                request.notes());
    }

    public ApproveVendorCommand toApproveCommand(
            UserPrincipal principal,
            UUID vendorId,
            ApproveVendorRequest request) {
        return new ApproveVendorCommand(
                principal.getId(),
                vendorId,
                request == null ? null : request.notes());
    }

    public VendorSummaryResponse toResponse(VendorSummaryView view) {
        return new VendorSummaryResponse(
                view.id(),
                view.vendorCode(),
                view.name(),
                view.taxCode(),
                view.email(),
                view.phone(),
                view.status(),
                view.onApprovedVendorList(),
                view.categories(),
                view.overallScore());
    }

    public VendorDetailResponse toResponse(VendorDetailView view) {
        return new VendorDetailResponse(
                view.id(),
                view.vendorCode(),
                view.name(),
                view.taxCode(),
                view.email(),
                view.phone(),
                view.status(),
                view.onApprovedVendorList(),
                view.categories(),
                view.overallScore(),
                toResponse(view.address()),
                view.contacts().stream().map(this::toResponse).toList(),
                view.scorecard() == null ? null : toResponse(view.scorecard()),
                List.of(),
                view.notes(),
                view.createdAt());
    }

    private List<VendorContactCommand> toContactCommands(List<VendorContactRequest> contacts) {
        return contacts == null ? List.of() : contacts.stream()
                .map(contact -> new VendorContactCommand(
                        contact.name(),
                        contact.role(),
                        contact.email(),
                        contact.phone(),
                        contact.isPrimary()))
                .toList();
    }

    private VendorAddressResponse toResponse(VendorAddressView view) {
        return new VendorAddressResponse(view.street(), view.district(), view.city(), view.country());
    }

    private VendorContactResponse toResponse(VendorContactView view) {
        return new VendorContactResponse(
                view.id(),
                view.name(),
                view.role(),
                view.email(),
                view.phone(),
                view.primary());
    }

    private VendorScorecardResponse toResponse(VendorScorecardView view) {
        return new VendorScorecardResponse(
                view.qualityScore(),
                view.deliveryScore(),
                view.priceScore(),
                view.responsivenessScore(),
                view.overallScore(),
                view.lastEvaluatedAt(),
                view.totalOrders(),
                view.onTimeDeliveryRate());
    }
}

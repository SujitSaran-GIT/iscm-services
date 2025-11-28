package com.iscm.mapper;

import com.iscm.dtos.response.VendorResponse;
import com.iscm.entity.Vendor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface VendorMapper {

    @Mapping(target = "organizationId", source = "organization.id")
    @Mapping(target = "organizationName", source = "organization.name")
    @Mapping(target = "status", expression = "java(vendor.getStatus().name())")
    @Mapping(target = "kycStatus", expression = "java(vendor.getKycStatus().name())")
    @Mapping(target = "slaTier", expression = "java(vendor.getSlaTier().name())")
    VendorResponse toResponse(Vendor vendor);
}

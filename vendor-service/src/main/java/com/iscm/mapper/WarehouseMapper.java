package com.iscm.mapper;

import com.iscm.dtos.response.WarehouseResponse;
import com.iscm.entity.Warehouse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WarehouseMapper {

    @Mapping(target = "organizationId", source = "organization.id")
    @Mapping(target = "organizationName", source = "organization.name")
    @Mapping(target = "vendorId", source = "vendor.id")
    @Mapping(target = "vendorName", source = "vendor.displayName")
    @Mapping(target = "type", expression = "java(warehouse.getType().name())")
    @Mapping(target = "status", expression = "java(warehouse.getStatus().name())")
    WarehouseResponse toResponse(Warehouse warehouse);
}

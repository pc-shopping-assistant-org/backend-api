package com.ecm.server.mapper;

import com.ecm.server.config.GlobalMapperConfig;
import com.ecm.server.dto.request.RegisterRequest;
import com.ecm.server.dto.request.UpdateProfileRequest;
import com.ecm.server.dto.response.CustomerDetailResponse;
import com.ecm.server.dto.response.CustomerOrderSummaryResponse;
import com.ecm.server.dto.response.EmployeeDetailResponse;
import com.ecm.server.dto.response.UserProfileResponse;
import com.ecm.server.dto.response.UserSummaryResponse;
import com.ecm.server.model.Account;
import com.ecm.server.model.Customer;
import com.ecm.server.model.Employee;
import com.ecm.server.model.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = GlobalMapperConfig.class)
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "account", ignore = true)
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "createdAt", ignore = true)
    Customer toCustomer(RegisterRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "account", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateCustomerFromRequest(UpdateProfileRequest request, @MappingTarget Customer customer);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "account", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEmployeeFromRequest(UpdateProfileRequest request, @MappingTarget Employee employee);

    @Mapping(target = "id", source = "customer.id")
    @Mapping(target = "accountId", source = "account.id")
    @Mapping(target = "username", source = "account.username")
    @Mapping(target = "role", source = "account.role.name")
    @Mapping(target = "fullName", source = "customer.fullName")
    @Mapping(target = "email", source = "customer.email")
    @Mapping(target = "phone", source = "customer.phone")
    UserSummaryResponse toSummary(Account account, Customer customer);

    @Mapping(target = "id", source = "employee.id")
    @Mapping(target = "accountId", source = "account.id")
    @Mapping(target = "username", source = "account.username")
    @Mapping(target = "role", source = "account.role.name")
    @Mapping(target = "fullName", source = "employee.fullName")
    @Mapping(target = "email", source = "employee.email")
    @Mapping(target = "phone", source = "employee.phone")
    UserSummaryResponse toSummary(Account account, Employee employee);

    @Mapping(target = "id", source = "customer.id")
    @Mapping(target = "accountId", source = "account.id")
    @Mapping(target = "username", source = "account.username")
    @Mapping(target = "role", source = "account.role.name")
    @Mapping(target = "fullName", source = "customer.fullName")
    @Mapping(target = "email", source = "customer.email")
    @Mapping(target = "phone", source = "customer.phone")
    @Mapping(target = "gender", source = "customer.gender")
    @Mapping(target = "birthday", source = "customer.birthday")
    @Mapping(target = "address", source = "customer.address")
    @Mapping(target = "status", source = "customer.status")
    @Mapping(target = "createdAt", source = "customer.createdAt")
    UserProfileResponse toProfile(Account account, Customer customer);

    @Mapping(target = "id", source = "employee.id")
    @Mapping(target = "accountId", source = "account.id")
    @Mapping(target = "username", source = "account.username")
    @Mapping(target = "role", source = "account.role.name")
    @Mapping(target = "fullName", source = "employee.fullName")
    @Mapping(target = "email", source = "employee.email")
    @Mapping(target = "phone", source = "employee.phone")
    @Mapping(target = "gender", source = "employee.gender")
    @Mapping(target = "birthday", source = "employee.birthday")
    @Mapping(target = "address", source = "employee.address")
    @Mapping(target = "status", source = "employee.status")
    @Mapping(target = "createdAt", source = "employee.createdAt")
    UserProfileResponse toProfile(Account account, Employee employee);

    @Mapping(target = "id", source = "employee.id")
    @Mapping(target = "accountId", source = "employee.account.id")
    @Mapping(target = "username", source = "employee.account.username")
    @Mapping(target = "roleId", source = "employee.account.role.id")
    @Mapping(target = "roleName", source = "employee.account.role.name")
    @Mapping(target = "fullName", source = "employee.fullName")
    @Mapping(target = "email", source = "employee.email")
    @Mapping(target = "phone", source = "employee.phone")
    @Mapping(target = "gender", source = "employee.gender")
    @Mapping(target = "birthday", source = "employee.birthday")
    @Mapping(target = "address", source = "employee.address")
    @Mapping(target = "status", source = "employee.status")
    @Mapping(target = "createdAt", source = "employee.createdAt")
    EmployeeDetailResponse toEmployeeDetail(Employee employee);

    @Mapping(target = "id", source = "customer.id")
    @Mapping(target = "accountId", source = "customer.account.id")
    @Mapping(target = "username", source = "customer.account.username")
    @Mapping(target = "fullName", source = "customer.fullName")
    @Mapping(target = "email", source = "customer.email")
    @Mapping(target = "phone", source = "customer.phone")
    @Mapping(target = "gender", source = "customer.gender")
    @Mapping(target = "birthday", source = "customer.birthday")
    @Mapping(target = "address", source = "customer.address")
    @Mapping(target = "status", source = "customer.status")
    @Mapping(target = "createdAt", source = "customer.createdAt")
    @Mapping(target = "totalOrders", source = "totalOrders")
    @Mapping(target = "totalSpent", source = "totalSpent")
    CustomerDetailResponse toCustomerDetail(Customer customer, long totalOrders, long totalSpent);

    @Mapping(target = "orderId", source = "order.id")
    @Mapping(target = "orderTime", source = "order.orderTime")
    @Mapping(target = "totalAmount", source = "order.totalAmount")
    @Mapping(target = "discountAmount", source = "order.discountAmount")
    @Mapping(target = "shipAmount", source = "order.shipAmount")
    @Mapping(target = "status", source = "order.status")
    @Mapping(target = "deliveryAddress", source = "order.deliveryAddress")
    @Mapping(target = "recipientName", source = "order.recipientName")
    @Mapping(target = "recipientPhone", source = "order.recipientPhone")
    CustomerOrderSummaryResponse toOrderSummary(Order order);
}

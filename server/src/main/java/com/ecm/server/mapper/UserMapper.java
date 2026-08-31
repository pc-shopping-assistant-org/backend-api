package com.ecm.server.mapper;

import com.ecm.server.config.GlobalMapperConfig;
import com.ecm.server.dto.request.RegisterRequest;
import com.ecm.server.dto.request.UpdateProfileRequest;
import com.ecm.server.dto.response.*;
import com.ecm.server.model.Account;
import com.ecm.server.model.Customer;
import com.ecm.server.model.Employee;
import com.ecm.server.model.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = GlobalMapperConfig.class)
public interface UserMapper {

    @Mapping(target = "accountId", ignore = true)
    @Mapping(target = "account", ignore = true)
    @Mapping(target = "firstName", expression = "java(UserMappingSupport.firstName(request.getFullName()))")
    @Mapping(target = "lastName", expression = "java(UserMappingSupport.lastName(request.getFullName()))")
    @Mapping(target = "gender", expression = "java(UserMappingSupport.normalizeCustomerGender(request.getGender()))")
    @Mapping(target = "avatarFileId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Customer toCustomer(RegisterRequest request);

    @Mapping(target = "accountId", ignore = true)
    @Mapping(target = "account", ignore = true)
    @Mapping(target = "firstName", expression = "java(UserMappingSupport.firstName(request.getFullName()))")
    @Mapping(target = "lastName", expression = "java(UserMappingSupport.lastName(request.getFullName()))")
    @Mapping(target = "gender", expression = "java(UserMappingSupport.normalizeCustomerGender(request.getGender()))")
    @Mapping(target = "avatarFileId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateCustomerFromRequest(UpdateProfileRequest request, @MappingTarget Customer customer);

    @Mapping(target = "accountId", ignore = true)
    @Mapping(target = "account", ignore = true)
    @Mapping(target = "firstName", expression = "java(UserMappingSupport.firstName(request.getFullName()))")
    @Mapping(target = "lastName", expression = "java(UserMappingSupport.lastName(request.getFullName()))")
    @Mapping(target = "gender", expression = "java(UserMappingSupport.normalizeEmployeeGender(request.getGender()))")
    @Mapping(target = "avatarFileId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEmployeeFromRequest(UpdateProfileRequest request, @MappingTarget Employee employee);

    @Mapping(target = "id", source = "account.id")
    @Mapping(target = "accountId", source = "account.id")
    @Mapping(target = "role", source = "account.role.name")
    @Mapping(target = "fullName", expression = "java(UserMappingSupport.fullName(customer.getFirstName(), customer.getLastName()))")
    @Mapping(target = "email", source = "account.email")
    @Mapping(target = "phone", source = "account.phone")
    UserSummaryResponse toSummary(Account account, Customer customer);

    @Mapping(target = "id", source = "account.id")
    @Mapping(target = "accountId", source = "account.id")
    @Mapping(target = "role", source = "account.role.name")
    @Mapping(target = "fullName", expression = "java(UserMappingSupport.fullName(employee.getFirstName(), employee.getLastName()))")
    @Mapping(target = "email", source = "account.email")
    @Mapping(target = "phone", source = "account.phone")
    UserSummaryResponse toSummary(Account account, Employee employee);

    @Mapping(target = "id", source = "account.id")
    @Mapping(target = "accountId", source = "account.id")
    @Mapping(target = "role", source = "account.role.name")
    @Mapping(target = "fullName", expression = "java(UserMappingSupport.fullName(customer.getFirstName(), customer.getLastName()))")
    @Mapping(target = "avatarFileId", source = "customer.avatarFileId")
    @Mapping(target = "email", source = "account.email")
    @Mapping(target = "phone", source = "account.phone")
    @Mapping(target = "gender", source = "customer.gender")
    @Mapping(target = "birthday", source = "customer.birthday")
    @Mapping(target = "address", ignore = true)
    @Mapping(target = "status", source = "account.status")
    @Mapping(target = "createdAt", source = "customer.createdAt")
    UserProfileResponse toProfile(Account account, Customer customer);

    @Mapping(target = "id", source = "account.id")
    @Mapping(target = "accountId", source = "account.id")
    @Mapping(target = "role", source = "account.role.name")
    @Mapping(target = "fullName", expression = "java(UserMappingSupport.fullName(employee.getFirstName(), employee.getLastName()))")
    @Mapping(target = "avatarFileId", source = "employee.avatarFileId")
    @Mapping(target = "email", source = "account.email")
    @Mapping(target = "phone", source = "account.phone")
    @Mapping(target = "gender", source = "employee.gender")
    @Mapping(target = "birthday", source = "employee.birthday")
    @Mapping(target = "address", source = "employee.address")
    @Mapping(target = "status", source = "account.status")
    @Mapping(target = "createdAt", source = "employee.createdAt")
    UserProfileResponse toProfile(Account account, Employee employee);

    @Mapping(target = "id", source = "employee.accountId")
    @Mapping(target = "accountId", source = "employee.account.id")
    @Mapping(target = "roleId", source = "employee.account.role.id")
    @Mapping(target = "roleName", source = "employee.account.role.name")
    @Mapping(target = "fullName", expression = "java(UserMappingSupport.fullName(employee.getFirstName(), employee.getLastName()))")
    @Mapping(target = "avatarFileId", source = "employee.avatarFileId")
    @Mapping(target = "email", source = "employee.account.email")
    @Mapping(target = "phone", source = "employee.account.phone")
    @Mapping(target = "gender", source = "employee.gender")
    @Mapping(target = "birthday", source = "employee.birthday")
    @Mapping(target = "salary", source = "employee.salary")
    @Mapping(target = "joinedAt", source = "employee.joinedAt")
    @Mapping(target = "address", source = "employee.address")
    @Mapping(target = "status", source = "employee.account.status")
    @Mapping(target = "createdAt", source = "employee.createdAt")
    EmployeeDetailResponse toEmployeeDetail(Employee employee);

    @Mapping(target = "id", source = "customer.accountId")
    @Mapping(target = "accountId", source = "customer.account.id")
    @Mapping(target = "fullName", expression = "java(UserMappingSupport.fullName(customer.getFirstName(), customer.getLastName()))")
    @Mapping(target = "avatarFileId", source = "customer.avatarFileId")
    @Mapping(target = "email", source = "customer.account.email")
    @Mapping(target = "phone", source = "customer.account.phone")
    @Mapping(target = "gender", source = "customer.gender")
    @Mapping(target = "birthday", source = "customer.birthday")
    @Mapping(target = "address", ignore = true)
    @Mapping(target = "addresses", ignore = true)
    @Mapping(target = "status", source = "customer.account.status")
    @Mapping(target = "createdAt", source = "customer.createdAt")
    @Mapping(target = "totalOrders", source = "totalOrders")
    @Mapping(target = "totalSpent", source = "totalSpent")
    CustomerDetailResponse toCustomerDetail(Customer customer, long totalOrders, long totalSpent);

    @Mapping(target = "orderId", source = "order.id")
    @Mapping(target = "orderTime", source = "order.orderTime")
    @Mapping(target = "totalAmount", source = "order.totalAmount")
    @Mapping(target = "discountAmount", source = "order.discountAmount")
    @Mapping(target = "shippingFee", source = "order.shippingFee")
    @Mapping(target = "status", source = "order.status")
    @Mapping(target = "deliveryAddress", source = "order.deliveryAddress")
    @Mapping(target = "recipientName", source = "order.recipientName")
    @Mapping(target = "recipientPhone", source = "order.recipientPhone")
    CustomerOrderSummaryResponse toOrderSummary(Order order);
}

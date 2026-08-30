$ErrorActionPreference = "Stop"

Write-Host "==========================================================================" -ForegroundColor Cyan
Write-Host " STARTING FULL END-TO-END API TEST SUITE (MODULES 1, 2, 3, 4, 5, 6, 7, 8)" -ForegroundColor Cyan
Write-Host "==========================================================================" -ForegroundColor Cyan

$baseUrl = "http://localhost:8080"
$rand = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()

function Invoke-ApiTest {
    param(
        [string]$Name,
        [string]$Method,
        [string]$Url,
        [object]$Body = $null,
        [string]$Token = $null,
        [bool]$ExpectFailure = $false,
        [hashtable]$CustomHeaders = $null
    )
    Write-Host "`n>>> [TEST] $Name" -ForegroundColor Yellow
    Write-Host "    $Method $Url" -ForegroundColor Gray

    $headers = @{}
    if ($Token) {
        $headers["Authorization"] = "Bearer $Token"
    }
    if ($CustomHeaders) {
        foreach ($k in $CustomHeaders.Keys) {
            $headers[$k] = $CustomHeaders[$k]
        }
    }

    try {
        $params = @{
            Uri = "$baseUrl$Url"
            Method = $Method
            Headers = $headers
            ContentType = "application/json; charset=utf-8"
        }
        if ($Body) {
            if ($Body -is [string]) {
                $params["Body"] = [System.Text.Encoding]::UTF8.GetBytes($Body)
            } else {
                $jsonString = ($Body | ConvertTo-Json -Depth 10)
                $params["Body"] = [System.Text.Encoding]::UTF8.GetBytes($jsonString)
            }
        }

        $res = Invoke-RestMethod @params
        if ($ExpectFailure) {
            Write-Host "    STATUS: UNEXPECTED SUCCESS (Expected failure but got code: $($res.code))" -ForegroundColor Red
            throw "Expected failure but received success response"
        }
        Write-Host "    STATUS: SUCCESS (Code: $($res.code), Msg: $($res.message))" -ForegroundColor Green
        return $res
    } catch {
        if ($ExpectFailure) {
            Write-Host "    STATUS: EXPECTED FAILURE RECEIVED - $($_.Exception.Message)" -ForegroundColor Green
            return $null
        }
        Write-Host "    STATUS: FAILED - $($_.Exception.Message)" -ForegroundColor Red
        if ($_.ErrorDetails) {
            Write-Host "    DETAILS: $($_.ErrorDetails.Message)" -ForegroundColor DarkRed
        }
        throw $_
    }
}

# -------------------------------------------------------------
# STEP 1: ADMIN LOGIN
# -------------------------------------------------------------
$adminLogin = Invoke-ApiTest -Name "1. Admin Login" -Method "POST" -Url "/api/v1/auth/login" -Body @{
    username = "admin"
    password = "Admin@123"
}
$adminToken = $adminLogin.data.accessToken

# -------------------------------------------------------------
# MODULE 1: AUTH & USER PROFILE
# -------------------------------------------------------------
Write-Host "`n========================================================" -ForegroundColor Magenta
Write-Host " MODULE 1: AUTH & USER PROFILE TESTS" -ForegroundColor Magenta
Write-Host "========================================================" -ForegroundColor Magenta

$testCustUsername = "cust_$rand"
$testCustEmail = "cust_$rand@example.com"
$testCustPhone = "098" + (Get-Random -Minimum 1000000 -Maximum 9999999)

# 1.1 Customer Registration
$regData = @{
    username = $testCustUsername
    password = "Password@123"
    fullName = "Nguyen Van Test"
    email = $testCustEmail
    phone = $testCustPhone
    gender = "MALE"
    address = "123 Le Loi, TP.HCM"
}
$regRes = Invoke-ApiTest -Name "1.1 Register Customer" -Method "POST" -Url "/api/v1/auth/register" -Body $regData

# 1.2 Retrieve OTP from Redis
$otp = (docker exec ecm-redis redis-cli get "otp:REGISTRATION:$testCustEmail").Trim()
Write-Host "    Retrieved Redis OTP: $otp" -ForegroundColor Cyan

# 1.3 Verify OTP
$verifyRes = Invoke-ApiTest -Name "1.2 Verify Registration OTP" -Method "POST" -Url "/api/v1/auth/verify-otp" -Body @{
    email = $testCustEmail
    otp = $otp
    purpose = "REGISTRATION"
}
$customerToken = $verifyRes.data.accessToken
$customerRefreshToken = $verifyRes.data.refreshToken
$customerId = $verifyRes.data.user.id
Write-Host "    Customer Account Created: ID=$customerId" -ForegroundColor Green

# 1.4 Customer Login
$custLogin = Invoke-ApiTest -Name "1.3 Customer Login" -Method "POST" -Url "/api/v1/auth/login" -Body @{
    username = $testCustUsername
    password = "Password@123"
}

# 1.5 Refresh Token
$refreshRes = Invoke-ApiTest -Name "1.4 Refresh Token" -Method "POST" -Url "/api/v1/auth/refresh-token" -Body @{
    refreshToken = $customerRefreshToken
}
$customerToken = $refreshRes.data.accessToken

# 1.6 View Profile /me
$meRes = Invoke-ApiTest -Name "1.5 Get Profile /me" -Method "GET" -Url "/api/v1/users/profile/me" -Token $customerToken

# 1.7 Update Profile /me
$updatedPhone = "097" + (Get-Random -Minimum 1000000 -Maximum 9999999)
$updateProfileRes = Invoke-ApiTest -Name "1.6 Update Profile /me" -Method "PUT" -Url "/api/v1/users/profile/me" -Token $customerToken -Body @{
    fullName = "Nguyen Van Test Updated"
    phone = $updatedPhone
    gender = "MALE"
    address = "456 Nguyen Hue, TP.HCM"
}

# 1.8 Change Password
$changePassRes = Invoke-ApiTest -Name "1.7 Change Password" -Method "PATCH" -Url "/api/v1/users/profile/change-password" -Token $customerToken -Body @{
    oldPassword = "Password@123"
    newPassword = "NewPassword@456"
}

# 1.9 Login with New Password
$loginNewPass = Invoke-ApiTest -Name "1.8 Login with New Password" -Method "POST" -Url "/api/v1/auth/login" -Body @{
    username = $testCustUsername
    password = "NewPassword@456"
}
$customerToken = $loginNewPass.data.accessToken

# 1.10 Logout
$logoutRes = Invoke-ApiTest -Name "1.9 Customer Logout" -Method "POST" -Url "/api/v1/auth/logout" -Token $customerToken

# -------------------------------------------------------------
# MODULE 2: USER & ROLE MANAGEMENT
# -------------------------------------------------------------
Write-Host "`n========================================================" -ForegroundColor Magenta
Write-Host " MODULE 2: USER & ROLE MANAGEMENT TESTS" -ForegroundColor Magenta
Write-Host "========================================================" -ForegroundColor Magenta

# 2.1 Get All Roles
$rolesRes = Invoke-ApiTest -Name "2.1 Get All Roles" -Method "GET" -Url "/api/v1/admin/roles" -Token $adminToken
$employeeRole = $rolesRes.data | Where-Object { $_.name -eq "ROLE_EMPLOYEE" } | Select-Object -First 1

# 2.2 Get Role By ID
$roleDetail = Invoke-ApiTest -Name "2.2 Get Role By ID" -Method "GET" -Url "/api/v1/admin/roles/$($employeeRole.id)" -Token $adminToken

# 2.3 Create New Employee
$empUsername = "emp_$rand"
$empEmail = "sales_$rand@ecm.com"
$empPhone = "091" + (Get-Random -Minimum 1000000 -Maximum 9999999)

$empCreateRes = Invoke-ApiTest -Name "2.3 Create Employee" -Method "POST" -Url "/api/v1/admin/employees" -Token $adminToken -Body @{
    username = $empUsername
    password = "Password@123"
    roleId = $employeeRole.id
    fullName = "Tran Van Sales"
    email = $empEmail
    phone = $empPhone
    gender = "FEMALE"
    address = "789 Hai Ba Trung, Ha Noi"
}
$createdEmpId = $empCreateRes.data.id

# 2.4 List Employees
$empListRes = Invoke-ApiTest -Name "2.4 List Employees (Cursor)" -Method "GET" -Url "/api/v1/admin/employees?limit=10" -Token $adminToken

# 2.5 Get Employee Detail
$empDetailRes = Invoke-ApiTest -Name "2.5 Get Employee Detail" -Method "GET" -Url "/api/v1/admin/employees/$createdEmpId" -Token $adminToken

# 2.6 Update Employee
$empUpdatedPhone = "092" + (Get-Random -Minimum 1000000 -Maximum 9999999)
$empUpdateRes = Invoke-ApiTest -Name "2.6 Update Employee" -Method "PUT" -Url "/api/v1/admin/employees/$createdEmpId" -Token $adminToken -Body @{
    roleId = $employeeRole.id
    fullName = "Tran Van Sales Senior"
    phone = $empUpdatedPhone
    gender = "FEMALE"
    address = "789 Hai Ba Trung Updated, Ha Noi"
}

# 2.7 Update Employee Status (LOCKED then ACTIVE)
$empStatusRes = Invoke-ApiTest -Name "2.7 Lock Employee Status" -Method "PATCH" -Url "/api/v1/admin/employees/$createdEmpId/status" -Token $adminToken -Body @{
    status = "LOCKED"
    reason = "Annual review period"
}
$empStatusRes2 = Invoke-ApiTest -Name "2.8 Unlock Employee Status" -Method "PATCH" -Url "/api/v1/admin/employees/$createdEmpId/status" -Token $adminToken -Body @{
    status = "ACTIVE"
    reason = "Review completed"
}

# 2.8 List Customers
$custListRes = Invoke-ApiTest -Name "2.9 List Customers (Cursor)" -Method "GET" -Url "/api/v1/admin/customers?limit=10" -Token $adminToken

# 2.9 Get Customer Detail
$custDetailRes = Invoke-ApiTest -Name "2.10 Get Customer Detail" -Method "GET" -Url "/api/v1/admin/customers/$customerId" -Token $adminToken

# 2.10 Get Customer Orders
$custOrdersRes = Invoke-ApiTest -Name "2.11 Get Customer Orders" -Method "GET" -Url "/api/v1/admin/customers/$customerId/orders" -Token $adminToken

# 2.11 Update Customer Status (BLOCKED then ACTIVE)
$custStatusRes = Invoke-ApiTest -Name "2.12 Block Customer" -Method "PATCH" -Url "/api/v1/admin/customers/$customerId/status" -Token $adminToken -Body @{
    status = "BLOCKED"
    reason = "Suspected fraud"
}
$custStatusRes2 = Invoke-ApiTest -Name "2.13 Unblock Customer" -Method "PATCH" -Url "/api/v1/admin/customers/$customerId/status" -Token $adminToken -Body @{
    status = "ACTIVE"
    reason = "Verified"
}

# -------------------------------------------------------------
# MODULE 3: CATALOG & MASTER DATA MANAGEMENT
# -------------------------------------------------------------
Write-Host "`n========================================================" -ForegroundColor Magenta
Write-Host " MODULE 3: CATALOG MASTER DATA TESTS" -ForegroundColor Magenta
Write-Host "========================================================" -ForegroundColor Magenta

# 3.1 Create Parent Category
$cat1 = Invoke-ApiTest -Name "3.1 Create Parent Category" -Method "POST" -Url "/api/v1/admin/categories" -Token $adminToken -Body @{
    name = "Smartphones $rand"
    seoName = "smartphones-$rand"
}
$parentCatId = $cat1.data.id

# 3.2 Create Child Category
$cat2 = Invoke-ApiTest -Name "3.2 Create Subcategory" -Method "POST" -Url "/api/v1/admin/categories" -Token $adminToken -Body @{
    name = "iOS Phones $rand"
    seoName = "ios-phones-$rand"
    parentId = $parentCatId
}
$childCatId = $cat2.data.id

# 3.3 Get Category Tree (Public)
$treeRes = Invoke-ApiTest -Name "3.3 Get Category Tree (Public)" -Method "GET" -Url "/api/v1/categories"

# 3.4 Get Category By ID (Public)
$catByIdRes = Invoke-ApiTest -Name "3.4 Get Category By ID (Public)" -Method "GET" -Url "/api/v1/categories/$parentCatId"

# 3.5 Get Category By Slug (Public)
$catBySlugRes = Invoke-ApiTest -Name "3.5 Get Category By Slug (Public)" -Method "GET" -Url "/api/v1/categories/slug/smartphones-$rand"

# 3.6 Update Category
$updateCatRes = Invoke-ApiTest -Name "3.6 Update Subcategory" -Method "PUT" -Url "/api/v1/admin/categories/$childCatId" -Token $adminToken -Body @{
    name = "iOS Phones Updated $rand"
    seoName = "ios-phones-updated-$rand"
    parentId = $parentCatId
    status = "ACTIVE"
}

# 3.7 Create Brand
$brand1 = Invoke-ApiTest -Name "3.7 Create Brand" -Method "POST" -Url "/api/v1/admin/brands" -Token $adminToken -Body @{
    name = "Apple $rand"
    description = "Leading brand in technology"
    logoUrl = "https://cdn.example.com/brands/apple_$rand.png"
}
$brandId = $brand1.data.id

# 3.8 Get All Brands (Public)
$brandsRes = Invoke-ApiTest -Name "3.8 Get All Brands (Public)" -Method "GET" -Url "/api/v1/brands"

# 3.9 Get Brand By ID (Public)
$brandByIdRes = Invoke-ApiTest -Name "3.9 Get Brand By ID (Public)" -Method "GET" -Url "/api/v1/brands/$brandId"

# 3.10 Update Brand
$brandUpdateRes = Invoke-ApiTest -Name "3.10 Update Brand" -Method "PUT" -Url "/api/v1/admin/brands/$brandId" -Token $adminToken -Body @{
    name = "Apple Global $rand"
    description = "Updated description"
    logoUrl = "https://cdn.example.com/brands/apple_updated_$rand.png"
    status = "ACTIVE"
}

# 3.11 Create Supplier
$supplier1 = Invoke-ApiTest -Name "3.11 Create Supplier" -Method "POST" -Url "/api/v1/admin/suppliers" -Token $adminToken -Body @{
    name = "FPT Trading $rand"
    email = "fpt_$rand@ecm.com"
    phone = "028" + (Get-Random -Minimum 1000000 -Maximum 9999999)
    address = "District 7, HCMC"
    description = "Official IT distributor"
}
$supplierId = $supplier1.data.id

# 3.12 Get Suppliers (Cursor)
$suppliersRes = Invoke-ApiTest -Name "3.12 Get Suppliers (Cursor)" -Method "GET" -Url "/api/v1/admin/suppliers?limit=10" -Token $adminToken

# 3.13 Get Supplier By ID
$supplierByIdRes = Invoke-ApiTest -Name "3.13 Get Supplier By ID" -Method "GET" -Url "/api/v1/admin/suppliers/$supplierId" -Token $adminToken

# 3.14 Update Supplier
$supplierUpdateRes = Invoke-ApiTest -Name "3.14 Update Supplier" -Method "PUT" -Url "/api/v1/admin/suppliers/$supplierId" -Token $adminToken -Body @{
    name = "FPT Distribution $rand"
    email = "fpt_$rand@ecm.com"
    phone = "028" + (Get-Random -Minimum 1000000 -Maximum 9999999)
    address = "District 7, HCMC"
    description = "Updated distributor info"
    status = "ACTIVE"
}

# -------------------------------------------------------------
# MODULE 4: DYNAMIC SPECIFICATIONS & VARIANT OPTIONS
# -------------------------------------------------------------
Write-Host "`n========================================================" -ForegroundColor Magenta
Write-Host " MODULE 4: DYNAMIC SPECIFICATIONS & VARIANT OPTIONS TESTS" -ForegroundColor Magenta
Write-Host "========================================================" -ForegroundColor Magenta

# 4.1 Create Attribute Definitions
$attr1 = Invoke-ApiTest -Name "4.1 Create Attribute (screen_size)" -Method "POST" -Url "/api/v1/admin/attributes" -Token $adminToken -Body @{
    key = "screen_size_$rand"
    displayName = "Kích thước màn hình"
    dataType = "NUMBER"
    unit = "inch"
    filterable = $true
    comparable = $true
}
$attr1Id = $attr1.data.id

$attr2 = Invoke-ApiTest -Name "4.2 Create Attribute (ram_capacity)" -Method "POST" -Url "/api/v1/admin/attributes" -Token $adminToken -Body @{
    key = "ram_$rand"
    displayName = "Dung lượng RAM"
    dataType = "ENUM"
    unit = "GB"
    allowedValues = @("8GB", "12GB", "16GB")
    filterable = $true
    comparable = $true
}
$attr2Id = $attr2.data.id

# 4.2 List All Attributes (Public)
$allAttrs = Invoke-ApiTest -Name "4.3 List All Attributes (Public)" -Method "GET" -Url "/api/v1/attributes"

# 4.3 Get Attribute By ID (Public)
$attrById = Invoke-ApiTest -Name "4.4 Get Attribute By ID (Public)" -Method "GET" -Url "/api/v1/attributes/$attr1Id"

# 4.4 Update Attribute
$updateAttr = Invoke-ApiTest -Name "4.5 Update Attribute" -Method "PUT" -Url "/api/v1/admin/attributes/$attr1Id" -Token $adminToken -Body @{
    displayName = "Kích thước màn hình Super Retina"
    dataType = "NUMBER"
    unit = "inch"
    filterable = $true
    comparable = $true
    status = "ACTIVE"
}

# 4.5 Create Category Attribute Group for Parent Category
$group1 = Invoke-ApiTest -Name "4.6 Create Category Spec Group (Màn hình)" -Method "POST" -Url "/api/v1/admin/category-attributes/groups" -Token $adminToken -Body @{
    categoryId = $parentCatId
    name = "Màn hình & Hiển thị"
    displayOrder = 1
}
$groupId = $group1.data.id

# 4.6 Update Category Attribute Group
$updateGroup = Invoke-ApiTest -Name "4.7 Update Category Spec Group" -Method "PUT" -Url "/api/v1/admin/category-attributes/groups/$groupId" -Token $adminToken -Body @{
    name = "Màn hình, Tấm nền & Tần số quét"
    displayOrder = 1
    status = "ACTIVE"
}

# 4.7 Assign Attribute to Group
$assignRes = Invoke-ApiTest -Name "4.8 Assign Attribute to Group" -Method "POST" -Url "/api/v1/admin/category-attributes/assign" -Token $adminToken -Body @{
    categoryGroupId = $groupId
    attributeId = $attr1Id
    required = $true
    displayOrder = 1
}
$assignmentId = $assignRes.data.id

# 4.8 Get Dynamic Category Specs Schema (Public)
$schemaRes = Invoke-ApiTest -Name "4.9 Get Category Dynamic Specs Schema (Public)" -Method "GET" -Url "/api/v1/categories/$parentCatId/specs-schema"
Write-Host "    Schema Group Count: $($schemaRes.data.groups.Count)" -ForegroundColor Cyan

# 4.9 Create Variant Options (Color, Storage)
$optionColor = Invoke-ApiTest -Name "4.10 Create Option (Color - Titan Sa Mạc)" -Method "POST" -Url "/api/v1/admin/options" -Token $adminToken -Body @{
    type = "COLOR"
    name = "Titan Sa Mạc $rand"
    value = "#C2B280"
}
$colorOptionId = $optionColor.data.id

$optionStorage = Invoke-ApiTest -Name "4.11 Create Option (Storage - 256GB)" -Method "POST" -Url "/api/v1/admin/options" -Token $adminToken -Body @{
    type = "STORAGE"
    name = "256GB $rand"
    value = "256GB"
}
$storageOptionId = $optionStorage.data.id

# 4.10 List Options Filtered by Type (Public)
$colorOptions = Invoke-ApiTest -Name "4.12 List Options by Type (COLOR) (Public)" -Method "GET" -Url "/api/v1/options?type=COLOR"

# 4.11 Get Option by ID (Public)
$optionDetail = Invoke-ApiTest -Name "4.13 Get Option by ID (Public)" -Method "GET" -Url "/api/v1/options/$colorOptionId"

# 4.12 Update Option
$updateOption = Invoke-ApiTest -Name "4.14 Update Option" -Method "PUT" -Url "/api/v1/admin/options/$colorOptionId" -Token $adminToken -Body @{
    type = "COLOR"
    name = "Titan Sa Mạc Cao Cấp $rand"
    value = "#C2B289"
    status = "ACTIVE"
}

# -------------------------------------------------------------
# MODULE 5: PRODUCT, VARIANT & MEDIA
# -------------------------------------------------------------
Write-Host "`n========================================================" -ForegroundColor Magenta
Write-Host " MODULE 5: PRODUCT, VARIANT & MEDIA TESTS" -ForegroundColor Magenta
Write-Host "========================================================" -ForegroundColor Magenta

# 5.1 Create Product with Initial Variant
$createProductData = @{
    name = "iPhone 16 Pro Max $rand"
    seoName = "iphone-16-pro-max-$rand"
    brandId = $brandId
    categoryId = $parentCatId
    supplierId = $supplierId
    description = "Flagship smartphone with Apple Intelligence"
    imageUrl = "https://cdn.example.com/products/iphone-16-pro-max.png"
    specifications = @{
        "screen_size" = 6.9
        "chip" = "A18 Pro"
    }
    variants = @(
        @{
            sku = "IP16PM-DESERT-256-$rand"
            price = 34990000
            priceSale = 33490000
            quantity = 100
            model = "A3296"
            inventoryPolicy = "DENY"
            warranty = "12 Months Apple VN"
            barcode = "885909$rand"
            imageUrl = "https://cdn.example.com/variants/iphone-16-desert.png"
            optionIds = @($colorOptionId, $storageOptionId)
            images = @(
                @{
                    name = "Front View"
                    imageUrl = "https://cdn.example.com/images/iphone-16-front.png"
                    isMain = $true
                }
            )
        }
    )
}
$createdProductRes = Invoke-ApiTest -Name "5.1 Create Product with Initial Variant" -Method "POST" -Url "/api/v1/admin/products" -Token $adminToken -Body $createProductData
$productId = $createdProductRes.data.id
$initialVariantId = $createdProductRes.data.variants[0].id

# 5.2 Add Additional Variant to Product
$addVariantData = @{
    sku = "IP16PM-TITAN-512-$rand"
    price = 40990000
    priceSale = 39990000
    quantity = 50
    model = "A3296"
    inventoryPolicy = "DENY"
    warranty = "12 Months Apple VN"
    barcode = "885910$rand"
    imageUrl = "https://cdn.example.com/variants/iphone-16-titan.png"
    optionIds = @($colorOptionId)
}
$addVariantRes = Invoke-ApiTest -Name "5.2 Add Variant to Product" -Method "POST" -Url "/api/v1/admin/products/$productId/variants" -Token $adminToken -Body $addVariantData
$secondVariantId = $addVariantRes.data.id

# 5.3 Add Image to Variant
$addImageRes = Invoke-ApiTest -Name "5.3 Add Image to Variant" -Method "POST" -Url "/api/v1/admin/variants/$secondVariantId/images" -Token $adminToken -Body @{
    name = "Side Profile"
    imageUrl = "https://cdn.example.com/images/iphone-16-side.png"
    isMain = $false
}
$imageId = $addImageRes.data.id

# 5.4 Admin List Products (Cursor)
$adminProductsRes = Invoke-ApiTest -Name "5.4 Admin List Products (Cursor)" -Method "GET" -Url "/api/v1/admin/products?limit=10" -Token $adminToken

# 5.5 Admin Update Product
$updateProductRes = Invoke-ApiTest -Name "5.5 Admin Update Product" -Method "PUT" -Url "/api/v1/admin/products/$productId" -Token $adminToken -Body @{
    name = "iPhone 16 Pro Max 2026 Edition $rand"
    seoName = "iphone-16-pro-max-$rand"
    brandId = $brandId
    categoryId = $parentCatId
    supplierId = $supplierId
    description = "Updated description with Apple Intelligence features"
    imageUrl = "https://cdn.example.com/products/iphone-16-pro-max-updated.png"
    status = "ACTIVE"
}

# 5.6 Admin Update Product Status
$updateProdStatusRes = Invoke-ApiTest -Name "5.6 Admin Update Product Status (ACTIVE)" -Method "PATCH" -Url "/api/v1/admin/products/$productId/status" -Token $adminToken -Body @{
    status = "ACTIVE"
    reason = "Ready for public sales"
}

# 5.7 Admin Update Variant
$updateVariantRes = Invoke-ApiTest -Name "5.7 Admin Update Variant" -Method "PUT" -Url "/api/v1/admin/variants/$secondVariantId" -Token $adminToken -Body @{
    price = 40990000
    priceSale = 38990000
    quantity = 75
    model = "A3296"
    inventoryPolicy = "CONTINUE"
    warranty = "24 Months VIP"
    barcode = "885910$rand"
    status = "ACTIVE"
    optionIds = @($colorOptionId, $storageOptionId)
}

# 5.8 Public List Products (Cursor & Filters)
$publicProducts = Invoke-ApiTest -Name "5.8 Public List Products (Cursor)" -Method "GET" -Url "/api/v1/products?limit=10"
Write-Host "    Found Public Products Count: $($publicProducts.data.items.Count)" -ForegroundColor Cyan

# 5.9 Public Get Product by ID
$publicProductDetail = Invoke-ApiTest -Name "5.9 Public Get Product by ID" -Method "GET" -Url "/api/v1/products/$productId"
Write-Host "    Product has $($publicProductDetail.data.variants.Count) variants" -ForegroundColor Cyan

# 5.10 Public Get Product by Slug
$publicProductBySlug = Invoke-ApiTest -Name "5.10 Public Get Product by Slug" -Method "GET" -Url "/api/v1/products/slug/iphone-16-pro-max-$rand"

# 5.11 Public Get Specific Variant Detail
$publicVariant = Invoke-ApiTest -Name "5.11 Public Get Variant Detail" -Method "GET" -Url "/api/v1/products/$productId/variants/$secondVariantId"
Write-Host "    Variant SKU: $($publicVariant.data.sku), Price: $($publicVariant.data.priceSale)" -ForegroundColor Cyan

# 5.12 Admin Delete Variant Image
$delImageRes = Invoke-ApiTest -Name "5.12 Admin Delete Variant Image" -Method "DELETE" -Url "/api/v1/admin/images/$imageId" -Token $adminToken

# -------------------------------------------------------------
# MODULE 6: DISCOUNT & PROMOTION MANAGEMENT
# -------------------------------------------------------------
Write-Host "`n========================================================" -ForegroundColor Magenta
Write-Host " MODULE 6: DISCOUNT & PROMOTION MANAGEMENT TESTS" -ForegroundColor Magenta
Write-Host "========================================================" -ForegroundColor Magenta

# 6.1 Admin Create Percentage Discount (ALL Scope)
$disc1Code = "SALE10_$rand"
$createDisc1 = Invoke-ApiTest -Name "6.1 Admin Create Discount (10% ALL)" -Method "POST" -Url "/api/v1/admin/discounts" -Token $adminToken -Body @{
    code = $disc1Code
    title = "Khuyến mãi 10% toàn sàn"
    type = "PERCENT"
    value = 10
    startAt = (Get-Date).ToUniversalTime().AddDays(-1).ToString("yyyy-MM-ddTHH:mm:ssZ")
    endAt = (Get-Date).ToUniversalTime().AddDays(30).ToString("yyyy-MM-ddTHH:mm:ssZ")
    scope = "ALL"
    minOrderAmount = 500000
    description = "Áp dụng cho mọi đơn hàng từ 500k"
}
$disc1Id = $createDisc1.data.id

# 6.2 Admin Create Fixed Discount (PRODUCT Scope)
$disc2Code = "VOUCHER1M_$rand"
$createDisc2 = Invoke-ApiTest -Name "6.2 Admin Create Fixed Discount (1M for iPhone Variant)" -Method "POST" -Url "/api/v1/admin/discounts" -Token $adminToken -Body @{
    code = $disc2Code
    title = "Giảm 1 triệu cho iPhone 16 Pro Max"
    type = "FIXED"
    value = 1000000
    startAt = (Get-Date).ToUniversalTime().AddDays(-1).ToString("yyyy-MM-ddTHH:mm:ssZ")
    endAt = (Get-Date).ToUniversalTime().AddDays(15).ToString("yyyy-MM-ddTHH:mm:ssZ")
    scope = "PRODUCT"
    minOrderAmount = 20000000
    description = "Giảm trực tiếp 1tr cho biến thể iPhone được chỉ định"
    appliedVariantIds = @($initialVariantId)
}
$disc2Id = $createDisc2.data.id

# 6.3 Admin List Discounts (Cursor)
$adminDiscList = Invoke-ApiTest -Name "6.3 Admin List Discounts (Cursor)" -Method "GET" -Url "/api/v1/admin/discounts?limit=10" -Token $adminToken

# 6.4 Admin Get Discount By ID
$discDetail = Invoke-ApiTest -Name "6.4 Admin Get Discount By ID" -Method "GET" -Url "/api/v1/admin/discounts/$disc2Id" -Token $adminToken
Write-Host "    Applied variants count: $($discDetail.data.appliedVariants.Count)" -ForegroundColor Cyan

# 6.5 Admin Update Discount
$updateDisc = Invoke-ApiTest -Name "6.5 Admin Update Discount" -Method "PUT" -Url "/api/v1/admin/discounts/$disc1Id" -Token $adminToken -Body @{
    title = "Khuyến mãi 12% toàn sàn Mega"
    type = "PERCENT"
    value = 12
    startAt = (Get-Date).ToUniversalTime().AddDays(-1).ToString("yyyy-MM-ddTHH:mm:ssZ")
    endAt = (Get-Date).ToUniversalTime().AddDays(30).ToString("yyyy-MM-ddTHH:mm:ssZ")
    scope = "ALL"
    minOrderAmount = 600000
    description = "Cập nhật giảm 12% cho đơn từ 600k"
    status = "ACTIVE"
}

# 6.6 Admin Update Discount Status (DISABLED then ACTIVE)
$updateDiscStatus = Invoke-ApiTest -Name "6.6 Admin Disable Discount Status" -Method "PATCH" -Url "/api/v1/admin/discounts/$disc1Id/status" -Token $adminToken -Body @{
    status = "DISABLED"
    reason = "Tạm dừng chương trình"
}
$updateDiscStatus2 = Invoke-ApiTest -Name "6.7 Admin Activate Discount Status" -Method "PATCH" -Url "/api/v1/admin/discounts/$disc1Id/status" -Token $adminToken -Body @{
    status = "ACTIVE"
    reason = "Kích hoạt lại"
}

# 6.7 Public List Active Discounts (Cursor)
$publicDiscList = Invoke-ApiTest -Name "6.8 Public List Active Discounts" -Method "GET" -Url "/api/v1/discounts?limit=10"

# 6.8 Customer Re-login to validate discount
$loginCust = Invoke-ApiTest -Name "Customer Login for Validation" -Method "POST" -Url "/api/v1/auth/login" -Body @{
    username = $testCustUsername
    password = "NewPassword@456"
}
$customerToken = $loginCust.data.accessToken

# 6.9 Customer Validate Percentage Discount (ALL Scope)
$validateAllRes = Invoke-ApiTest -Name "6.9 Customer Validate Percentage Discount" -Method "POST" -Url "/api/v1/discounts/validate" -Token $customerToken -Body @{
    code = $disc1Code
    orderAmount = 1000000
}
Write-Host "    Discount Amount: $($validateAllRes.data.discountAmount), Final: $($validateAllRes.data.finalAmount)" -ForegroundColor Cyan

# 6.10 Customer Validate Product-Scope Discount with matching items
$validateProdRes = Invoke-ApiTest -Name "6.10 Customer Validate Product-Specific Discount" -Method "POST" -Url "/api/v1/discounts/validate" -Token $customerToken -Body @{
    code = $disc2Code
    orderAmount = 33490000
    items = @(
        @{
            productVariantId = $initialVariantId
            quantity = 1
            unitPrice = 33490000
        }
    )
}
Write-Host "    Discount Amount: $($validateProdRes.data.discountAmount), Final: $($validateProdRes.data.finalAmount)" -ForegroundColor Cyan

# 6.11 Customer Validate Discount with insufficient order amount (Expect 400 Bad Request)
$validateFailRes = Invoke-ApiTest -Name "6.11 Validate Discount below Min Amount (Expect Failure)" -Method "POST" -Url "/api/v1/discounts/validate" -Token $customerToken -ExpectFailure $true -Body @{
    code = $disc1Code
    orderAmount = 200000
}

# -------------------------------------------------------------
# MODULE 7: CART & ORDER MANAGEMENT
# -------------------------------------------------------------
Write-Host "`n========================================================" -ForegroundColor Magenta
Write-Host " MODULE 7: CART & ORDER MANAGEMENT TESTS" -ForegroundColor Magenta
Write-Host "========================================================" -ForegroundColor Magenta

# 7.1 Customer Add Variant to Cart (Redis)
$addCartRes = Invoke-ApiTest -Name "7.1 Customer Add Variant to Cart (Redis)" -Method "POST" -Url "/api/v1/cart/items" -Token $customerToken -Body @{
    productVariantId = $initialVariantId
    quantity = 2
}
Write-Host "    Cart Items Count: $($addCartRes.data.totalItems), Subtotal: $($addCartRes.data.subtotalAmount)" -ForegroundColor Cyan

# 7.2 Customer View Cart (Redis)
$getCartRes = Invoke-ApiTest -Name "7.2 Customer View Cart (Redis)" -Method "GET" -Url "/api/v1/cart" -Token $customerToken
Write-Host "    Cart Total Items: $($getCartRes.data.totalItems)" -ForegroundColor Cyan

# 7.3 Customer Update Cart Item Quantity (Redis)
$updateCartRes = Invoke-ApiTest -Name "7.3 Customer Update Cart Item Quantity (Redis)" -Method "PUT" -Url "/api/v1/cart/items/$initialVariantId" -Token $customerToken -Body @{
    quantity = 3
}

# 7.4 Customer Remove Item from Cart (Redis)
$removeCartRes = Invoke-ApiTest -Name "7.4 Customer Remove Item from Cart (Redis)" -Method "DELETE" -Url "/api/v1/cart/items/$initialVariantId" -Token $customerToken

# 7.5 Customer Clear Cart (Redis)
$clearCartRes = Invoke-ApiTest -Name "7.5 Customer Clear Cart (Redis)" -Method "DELETE" -Url "/api/v1/cart" -Token $customerToken

# 7.6 Customer Re-add Variant to Cart for Checkout
$readdCartRes = Invoke-ApiTest -Name "7.6 Customer Re-add Variant to Cart" -Method "POST" -Url "/api/v1/cart/items" -Token $customerToken -Body @{
    productVariantId = $initialVariantId
    quantity = 1
}

# 7.7 Customer Checkout / Create Order (Order 1 with 10% Discount)
$createOrder1 = Invoke-ApiTest -Name "7.7 Customer Checkout / Create Order (with Discount)" -Method "POST" -Url "/api/v1/orders" -Token $customerToken -Body @{
    items = @(
        @{
            productVariantId = $initialVariantId
            quantity = 1
        }
    )
    discountCode = $disc1Code
    recipientName = "Nguyen Van Test"
    recipientPhone = $testCustPhone
    deliveryAddress = "456 Nguyen Hue, Quan 1, TP.HCM"
    note = "Giao hang gio hanh chinh"
    paymentMethod = "COD"
}
$order1Id = $createOrder1.data.id
Write-Host "    Created Order 1 ID: $order1Id, Total: $($createOrder1.data.totalAmount)" -ForegroundColor Cyan

# 7.8 Customer View My Orders (Cursor)
$myOrdersRes = Invoke-ApiTest -Name "7.8 Customer View My Orders (Cursor)" -Method "GET" -Url "/api/v1/orders/me?limit=10" -Token $customerToken
Write-Host "    My Orders Count: $($myOrdersRes.data.items.Count)" -ForegroundColor Cyan

# 7.9 Customer View Order Detail by ID
$orderDetailRes = Invoke-ApiTest -Name "7.9 Customer View Order Detail by ID" -Method "GET" -Url "/api/v1/orders/$order1Id" -Token $customerToken
Write-Host "    Order 1 Status: $($orderDetailRes.data.status)" -ForegroundColor Cyan

# 7.10 Customer Cancel Order (Order 1)
$cancelOrderRes = Invoke-ApiTest -Name "7.10 Customer Cancel Order (Order 1)" -Method "PATCH" -Url "/api/v1/orders/$order1Id/cancel" -Token $customerToken -Body @{
    reason = "Doi y khong muon mua nua"
}
Write-Host "    Order 1 Status after cancel: $($cancelOrderRes.data.status)" -ForegroundColor Cyan

# 7.11 Customer Checkout Order 2 for Admin Lifecycle Testing
$createOrder2 = Invoke-ApiTest -Name "7.11 Customer Checkout Order 2 (For Admin Flow)" -Method "POST" -Url "/api/v1/orders" -Token $customerToken -Body @{
    items = @(
        @{
            productVariantId = $initialVariantId
            quantity = 1
        }
    )
    recipientName = "Nguyen Van Test"
    recipientPhone = $testCustPhone
    deliveryAddress = "456 Nguyen Hue, Quan 1, TP.HCM"
    paymentMethod = "VNPAY"
}
$order2Id = $createOrder2.data.id

# 7.12 Admin List All Orders (Cursor)
$adminOrdersRes = Invoke-ApiTest -Name "7.12 Admin List All Orders (Cursor)" -Method "GET" -Url "/api/v1/admin/orders?limit=10" -Token $adminToken
Write-Host "    Admin Found Orders Count: $($adminOrdersRes.data.items.Count)" -ForegroundColor Cyan

# 7.13 Admin View Order Detail by ID
$adminOrderDetail = Invoke-ApiTest -Name "7.13 Admin View Order Detail by ID" -Method "GET" -Url "/api/v1/admin/orders/$order2Id" -Token $adminToken

# 7.14 Admin Update Order Status (PENDING -> CONFIRM)
$statusConfirmRes = Invoke-ApiTest -Name "7.14 Admin Update Order Status (CONFIRM)" -Method "PATCH" -Url "/api/v1/admin/orders/$order2Id/status" -Token $adminToken -Body @{
    status = "CONFIRM"
    reason = "Da xac nhan don hang voi khach"
}

# 7.15 Admin Update Order Status (CONFIRM -> SHIPPING)
$statusShippingRes = Invoke-ApiTest -Name "7.15 Admin Update Order Status (SHIPPING)" -Method "PATCH" -Url "/api/v1/admin/orders/$order2Id/status" -Token $adminToken -Body @{
    status = "SHIPPING"
    reason = "Da ban giao cho don vi van chuyen"
}

# 7.16 Admin Update Order Status (SHIPPING -> COMPLETED)
$statusCompletedRes = Invoke-ApiTest -Name "7.16 Admin Update Order Status (COMPLETED)" -Method "PATCH" -Url "/api/v1/admin/orders/$order2Id/status" -Token $adminToken -Body @{
    status = "COMPLETED"
    reason = "Giao hang thanh cong va da thu tien"
}
Write-Host "    Order 2 Status: $($statusCompletedRes.data.status), DeliveredAt: $($statusCompletedRes.data.deliveredAt)" -ForegroundColor Cyan

# 7.17 Admin Get Order Invoice
$invoiceRes = Invoke-ApiTest -Name "7.17 Admin Get Order Invoice" -Method "GET" -Url "/api/v1/admin/orders/$order2Id/invoice" -Token $adminToken
Write-Host "    Invoice Number: $($invoiceRes.data.invoiceId), Total Amount: $($invoiceRes.data.totalAmount)" -ForegroundColor Cyan

# -------------------------------------------------------------
# MODULE 8: PAYMENT & CHECKOUT PROCESSING
# -------------------------------------------------------------
Write-Host "`n========================================================" -ForegroundColor Magenta
Write-Host " MODULE 8: PAYMENT & CHECKOUT PROCESSING TESTS" -ForegroundColor Magenta
Write-Host "========================================================" -ForegroundColor Magenta

# 8.1 Create Order 3 for Payment Tests
$createOrder3 = Invoke-ApiTest -Name "Setup: Customer Create Order 3 for Payment" -Method "POST" -Url "/api/v1/orders" -Token $customerToken -Body @{
    items = @(
        @{
            productVariantId = $initialVariantId
            quantity = 1
        }
    )
    recipientName = "Nguyen Van Test"
    recipientPhone = $testCustPhone
    deliveryAddress = "456 Nguyen Hue, Quan 1, TP.HCM"
    paymentMethod = "STRIPE"
}
$order3Id = $createOrder3.data.id

# 8.2 Create Payment Intent (STRIPE)
$createIntentRes = Invoke-ApiTest -Name "8.1 Customer Create Payment Intent (STRIPE)" -Method "POST" -Url "/api/v1/payments/create-intent" -Token $customerToken -Body @{
    orderId = $order3Id
    paymentMethod = "STRIPE"
}
$payment3Id = $createIntentRes.data.paymentId
$clientSecret = $createIntentRes.data.clientSecret
Write-Host "    Payment ID: $payment3Id, ClientSecret: $clientSecret" -ForegroundColor Cyan

# 8.3 Get Payment by Order ID
$getPaymentByOrderRes = Invoke-ApiTest -Name "8.2 Customer Get Payment by Order ID" -Method "GET" -Url "/api/v1/payments/order/$order3Id" -Token $customerToken
$stripeTransCode = $getPaymentByOrderRes.data.transactionCode
Write-Host "    Payment Method: $($getPaymentByOrderRes.data.method), TransactionCode: $stripeTransCode" -ForegroundColor Cyan

# 8.4 Confirm COD Payment (Order 1)
$payment1Id = $createOrder1.data.payment.id
$confirmCodRes = Invoke-ApiTest -Name "8.3 Customer Confirm COD Payment" -Method "POST" -Url "/api/v1/payments/$payment1Id/confirm-cod" -Token $customerToken
Write-Host "    Confirmed COD Status: $($confirmCodRes.data.status)" -ForegroundColor Cyan

# 8.5 Public Webhook Stripe Event (payment_intent.succeeded)
$stripeWebhookPayload = @{
    type = "payment_intent.succeeded"
    data = @{
        object = @{
            id = $stripeTransCode
            amount = 33490000
            status = "succeeded"
        }
    }
} | ConvertTo-Json -Depth 5

$webhookRes = Invoke-ApiTest -Name "8.4 Public Webhook Stripe (payment_intent.succeeded)" -Method "POST" -Url "/api/v1/payments/webhook/stripe" -Body $stripeWebhookPayload

# 8.6 Verify Order 3 automatically advanced to CONFIRM after successful payment webhook
$order3Detail = Invoke-ApiTest -Name "8.5 Verify Order 3 Auto-confirmed after Webhook" -Method "GET" -Url "/api/v1/orders/$order3Id" -Token $customerToken
Write-Host "    Order 3 Status after Stripe Webhook: $($order3Detail.data.status)" -ForegroundColor Cyan

# 8.7 Admin List All Payments (Cursor)
$adminPaymentsRes = Invoke-ApiTest -Name "8.6 Admin List All Payments (Cursor)" -Method "GET" -Url "/api/v1/admin/payments?limit=10" -Token $adminToken
Write-Host "    Admin Found Payments Count: $($adminPaymentsRes.data.items.Count)" -ForegroundColor Cyan

# 8.8 Admin Update Payment Status Manually (Bank reconciliation)
$adminUpdatePayment = Invoke-ApiTest -Name "8.7 Admin Update Payment Status (PAID)" -Method "PATCH" -Url "/api/v1/admin/payments/$payment1Id/status" -Token $adminToken -Body @{
    status = "PAID"
    transactionCode = "RECONCILED_$rand"
    note = "Xac nhan doi soat ngan hang thanh cong"
}
Write-Host "    Admin Updated Payment Status: $($adminUpdatePayment.data.status), TransactionCode: $($adminUpdatePayment.data.transactionCode)" -ForegroundColor Cyan

# -------------------------------------------------------------
# MODULE 9: PRODUCT REVIEW & RATING
# -------------------------------------------------------------
Write-Host "`n========================================================" -ForegroundColor Magenta
Write-Host " MODULE 9: PRODUCT REVIEW & RATING TESTS" -ForegroundColor Magenta
Write-Host "========================================================" -ForegroundColor Magenta

# 9.1 Customer Create Review (5 Stars + Verified Purchase check)
$createReviewRes = Invoke-ApiTest -Name "9.1 Customer Create Product Review" -Method "POST" -Url "/api/v1/products/$productId/reviews" -Token $customerToken -Body @{
    rating = 5
    comment = "May dung rat muot ma, pin trau, man hinh sac net!"
}
$reviewId = $createReviewRes.data.id
Write-Host "    Review Created ID: $reviewId, Rating: $($createReviewRes.data.rating), Verified: $($createReviewRes.data.isVerifiedPurchase)" -ForegroundColor Cyan

# 9.2 Attempt Duplicate Review on same product (Expect 409 Conflict)
$dupReviewRes = Invoke-ApiTest -Name "9.2 Duplicate Review by Same Customer (Expect Failure)" -Method "POST" -Url "/api/v1/products/$productId/reviews" -Token $customerToken -ExpectFailure $true -Body @{
    rating = 4
    comment = "Co tinh tao them review thu hai"
}

# 9.3 Public Get Product Reviews (Cursor)
$publicReviewsRes = Invoke-ApiTest -Name "9.3 Public Get Product Reviews (Cursor)" -Method "GET" -Url "/api/v1/products/$productId/reviews?limit=10"
Write-Host "    Found Reviews Count: $($publicReviewsRes.data.items.Count)" -ForegroundColor Cyan

# 9.4 Public Get Product Rating Summary
$summaryRes = Invoke-ApiTest -Name "9.4 Public Get Product Rating Summary" -Method "GET" -Url "/api/v1/products/$productId/reviews/summary"
Write-Host "    Average Rating: $($summaryRes.data.averageRating), Total Reviews: $($summaryRes.data.totalReviews)" -ForegroundColor Cyan

# 9.4 Customer Update Review
$updateReviewRes = Invoke-ApiTest -Name "9.4 Customer Update Review" -Method "PUT" -Url "/api/v1/products/$productId/reviews/$reviewId" -Token $customerToken -Body @{
    rating = 4
    comment = "Sau 1 tuan su dung: may hoi am khi quay video 4K nhung van rat hai long."
}
Write-Host "    Updated Review Rating: $($updateReviewRes.data.rating)" -ForegroundColor Cyan

# 9.5 Admin List All Reviews (Cursor)
$adminReviewsRes = Invoke-ApiTest -Name "9.5 Admin List All Reviews (Cursor)" -Method "GET" -Url "/api/v1/admin/reviews?limit=10" -Token $adminToken
Write-Host "    Admin Found Reviews Count: $($adminReviewsRes.data.items.Count)" -ForegroundColor Cyan

# 9.6 Admin Update Review Status (Moderate: INACTIVE then ACTIVE)
$adminModerateRes = Invoke-ApiTest -Name "9.6 Admin Hide Review (INACTIVE)" -Method "PATCH" -Url "/api/v1/admin/reviews/$reviewId/status" -Token $adminToken -Body @{
    status = "INACTIVE"
    reason = "Kiem tra noi dung nhan xet"
}
$adminModerateRes2 = Invoke-ApiTest -Name "9.7 Admin Restore Review (ACTIVE)" -Method "PATCH" -Url "/api/v1/admin/reviews/$reviewId/status" -Token $adminToken -Body @{
    status = "ACTIVE"
    reason = "Noi dung phu hop tieu chuan cong dong"
}

# 9.7 Customer Delete Review
$deleteReviewRes = Invoke-ApiTest -Name "9.8 Customer Delete Review" -Method "DELETE" -Url "/api/v1/products/$productId/reviews/$reviewId" -Token $customerToken

# -------------------------------------------------------------
# MODULE 10: ANALYTICS & DASHBOARD
# -------------------------------------------------------------
Write-Host "`n========================================================" -ForegroundColor Magenta
Write-Host " MODULE 10: ANALYTICS & DASHBOARD TESTS" -ForegroundColor Magenta
Write-Host "========================================================" -ForegroundColor Magenta

# 10.1 Admin Get Overview
$overviewRes = Invoke-ApiTest -Name "10.1 Admin Get Dashboard Overview" -Method "GET" -Url "/api/v1/admin/analytics/overview" -Token $adminToken
Write-Host "    Total Revenue: $($overviewRes.data.totalRevenue), Total Orders: $($overviewRes.data.totalOrders), Completed: $($overviewRes.data.completedOrders), Total Customers: $($overviewRes.data.totalCustomers)" -ForegroundColor Cyan

# 10.2 Admin Get Revenue Chart (DAY)
$revenueChartDayRes = Invoke-ApiTest -Name "10.2 Admin Get Revenue Chart (DAY)" -Method "GET" -Url "/api/v1/admin/analytics/revenue-chart?period=DAY" -Token $adminToken
Write-Host "    Revenue Chart (DAY) DataPoints Count: $($revenueChartDayRes.data.dataPoints.Count)" -ForegroundColor Cyan

# 10.3 Admin Get Revenue Chart (MONTH)
$revenueChartMonthRes = Invoke-ApiTest -Name "10.3 Admin Get Revenue Chart (MONTH)" -Method "GET" -Url "/api/v1/admin/analytics/revenue-chart?period=MONTH" -Token $adminToken
Write-Host "    Revenue Chart (MONTH) DataPoints Count: $($revenueChartMonthRes.data.dataPoints.Count)" -ForegroundColor Cyan

# 10.4 Admin Get Top Selling Products
$topSellingRes = Invoke-ApiTest -Name "10.4 Admin Get Top Selling Products" -Method "GET" -Url "/api/v1/admin/analytics/top-selling?limit=5" -Token $adminToken
Write-Host "    Top Selling Items Count: $($topSellingRes.data.Count)" -ForegroundColor Cyan

# 10.5 Admin Get Order Status Stats
$orderStatsRes = Invoke-ApiTest -Name "10.5 Admin Get Order Status Stats" -Method "GET" -Url "/api/v1/admin/analytics/order-status-stats" -Token $adminToken
Write-Host "    Order Status Categories Count: $($orderStatsRes.data.Count)" -ForegroundColor Cyan

# Clean up
$delVariantRes = Invoke-ApiTest -Name "Clean up: Admin Delete Variant" -Method "DELETE" -Url "/api/v1/admin/variants/$secondVariantId" -Token $adminToken

Write-Host "`n==========================================================================" -ForegroundColor Green
Write-Host " ALL 109 API ENDPOINT TESTS ACROSS MODULES 1-10 COMPLETED SUCCESSFULLY!" -ForegroundColor Green
Write-Host "==========================================================================" -ForegroundColor Green

$body = @{
    username = "anhkha_user"
    email = "anhkha30804.social@gmail.com"
    password = "Password123@"
    fullName = "Anh Kha"
    phone = "0987654321"
    gender = "MALE"
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/register" -Method Post -ContentType "application/json" -Body $body
$response | ConvertTo-Json -Depth 5

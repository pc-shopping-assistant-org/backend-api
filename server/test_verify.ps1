$body = @{
    email = "anhkha30804.social@gmail.com"
    otp = "137861"
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/verify-otp" -Method Post -ContentType "application/json" -Body $body
$response | ConvertTo-Json -Depth 5

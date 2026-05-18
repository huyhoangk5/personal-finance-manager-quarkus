# Script tạo RSA key pair cho JWT
# Chạy: .\generate-keys.ps1

$resourceDir = "src\main\resources"

Write-Host "Tạo RSA key pair cho JWT..."

# Tạo private key
openssl genrsa -out "$resourceDir\privateKey.pem" 2048

# Tạo public key từ private key
openssl rsa -in "$resourceDir\privateKey.pem" -pubout -out "$resourceDir\publicKey.pem"

Write-Host "Done! privateKey.pem và publicKey.pem đã được tạo trong $resourceDir"
Write-Host "QUAN TRỌNG: Không commit privateKey.pem lên git!"

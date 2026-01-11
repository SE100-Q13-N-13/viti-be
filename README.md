# Viti Backend Service

Đây là hệ thống Backend (RESTful API) cho dự án **Viti**, được xây dựng trên nền tảng **Spring Boot**. Hệ thống cung cấp các chức năng quản lý toàn diện bao gồm: quản lý sản phẩm, kho hàng, đơn nhập/xuất, khách hàng, tích điểm thành viên và báo cáo thống kê.

## Công nghệ sử dụng (Tech Stack)

* **Core Framework**: Java 17+, Spring Boot 3.x
* **Database**: PostgreSQL
* **ORM**: Spring Data JPA (Hibernate)
* **Security**: Spring Security, JWT (JSON Web Token)
* **Storage**: Cloudinary (Quản lý hình ảnh)
* **API Documentation**: OpenAPI / Swagger UI
* **Build Tool**: Maven

## Yêu cầu tiên quyết (Prerequisites)

Trước khi cài đặt, hãy đảm bảo máy tính của bạn đã có:

1.  **Java Development Kit (JDK)**: Phiên bản 17 trở lên.
2.  **PostgreSQL**: Đã cài đặt và đang hoạt động (Local hoặc Docker).
3.  **Git**: Để clone mã nguồn.

## Cấu hình (Configuration)

Dự án sử dụng các biến môi trường để bảo mật thông tin. Bạn cần cấu hình file `src/main/resources/application.properties` hoặc tạo file `.env` (nếu dự án có hỗ trợ nạp env) với các thông số sau:

### 1. Database PostgreSQL
Tạo một database trống tên là `viti_db` (hoặc tên tùy chọn) trong PostgreSQL.

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/viti_db
spring.datasource.username=postgres
spring.datasource.password=your_db_password
spring.jpa.hibernate.ddl-auto=update
```
### 2. Cloudinary (Lưu trữ ảnh)
```properties
cloudinary.cloud_name=your_cloud_name
cloudinary.api_key=your_api_key
cloudinary.api_secret=your_api_secret
```
### 3. JWT Security
```properties
viti.app.jwtSecret=your_very_long_secret_key_here
viti.app.jwtExpirationMs=86400000  # Ví dụ: 1 ngày
```
## Cài đặt và Chạy ứng dụng (Build & Run)
Dự án đã tích hợp sẵn Maven Wrapper, bạn không cần cài đặt Maven thủ công.

### Bước 1: Clone dự án
```bash
git clone [https://github.com/username/viti-be.git](https://github.com/username/viti-be.git)
cd viti-be
```
### Bước 2: Cài đặt dependencies
#### Windows (CMD/PowerShell):
```bash
mvnw.cmd clean install
```
#### Linux/macOS:
```bash
./mvnw clean install
```
### Bước 3: Chạy ứng dụng
#### Windows:
```bash
mvnw.cmd spring-boot:run
```
#### Linux/macOS:
```bash
./mvnw spring-boot:run
```
Sau khi khởi động thành công, server sẽ chạy tại: http://localhost:8080

## Tài liệu API (API Documentation)
Hệ thống tích hợp sẵn Swagger để xem và test API trực quan. Sau khi chạy server, truy cập:

Swagger UI: http://localhost:8080/swagger-ui/index.html

OpenAPI JSON: http://localhost:8080/v3/api-docs

## Dữ liệu khởi tạo (Data Seeding)
Khi chạy lần đầu, hệ thống sẽ tự động nạp dữ liệu mẫu (như danh sách Tỉnh/Thành phố, Xã/Phường) từ thư mục src/main/resources/data/ thông qua DataLoader.java và DataInitializer.java.

## Đóng gói (Deployment)
Để build ứng dụng ra file .jar để deploy lên server:

```bash
./mvnw clean package -DskipTests
```
File kết quả sẽ nằm tại thư mục target/viti-be-0.0.1-SNAPSHOT.jar. Chạy file này bằng lệnh:

```bash
java -jar target/viti-be-0.0.1-SNAPSHOT.jar
```
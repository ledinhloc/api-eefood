# 🌟 API EEFOOD

## 📝 Giới thiệu
API EEFOOD là một dự án backend được xây dựng theo kiến trúc microservice sử dụng Java Spring Boot. Dự án cung cấp các dịch vụ liên quan đến quản lý công thức nấu ăn, người dùng, thông báo, và các tính năng khác phục vụ cho ứng dụng EEFOOD.

## ✨ Tính năng chính
- 🍽️ **Quản lý công thức nấu ăn**: Cho phép thêm, sửa, xóa và tìm kiếm công thức nấu ăn.
- 👤 **Quản lý người dùng**: Đăng ký, đăng nhập, phân quyền và quản lý thông tin người dùng.
- 🔔 **Thông báo**: Gửi thông báo đến người dùng.
- 💬 **Phản hồi**: Quản lý các phản hồi và đánh giá từ người dùng.
- 🌐 **Quản lý bài viết**: Quản lý các bài viết người dùng.
- 🔍 **Tìm kiếm món ăn**: Tìm kiếm và lọc theo nhiều tiêu chí.

## 🛠️ Công nghệ sử dụng
- **Ngôn ngữ lập trình**: Java
- **Framework**: Spring Boot
- **Quản lý cấu hình**: Spring Cloud Config
- **Service Discovery**: Netflix Eureka
- **API Gateway**: Spring Cloud Gateway
- **Cơ sở dữ liệu**: PostgreSQL
- **Giao tiếp**: Kafka, Open Feign, Schema Registry
- **Xác thực**: Keycloak
- **Realtime**: Websocket
- **Containerization**: Docker

## 🚀 Hướng dẫn chạy dự án

### ✅ Yêu cầu hệ thống
- **Java**: Phiên bản 17 trở lên
- **Maven**: Phiên bản 3.8 trở lên
- **Docker**: Đã được cài đặt và cấu hình

### 🔧 Các bước thực hiện
1. **Clone repository**:
   ```bash
   git clone https://github.com/ledinhloc/api-eefood.git
   cd api-eefood
   ```

2. **Cấu hình cơ sở dữ liệu**:
   - Chỉnh sửa file `application.yml` trong từng service để phù hợp với cấu hình cơ sở dữ liệu của bạn.

3. **Chạy các service**:
   - Sử dụng Maven để build và chạy từng service:
     ```bash
     cd services/<service-name>
     ./mvnw spring-boot:run
     ```
     Thay `<service-name>` bằng tên của từng service như `config-service`, `discovery`, `gateway`, `iam-service`, v.v.

4. **Chạy toàn bộ hệ thống bằng Docker Compose**:
   - Sử dụng file `docker-compose.infra.yml` để chạy các service:
     ```bash
     docker-compose -f resource/docker-compose.infra.yml up --build
     ```

5. **Kiểm tra hoạt động**:
   - Truy cập các endpoint của từng service qua API Gateway hoặc trực tiếp qua cổng của từng service.

## 📬 Liên hệ
Nếu bạn có bất kỳ câu hỏi hoặc góp ý nào, vui lòng liên hệ qua email: `anhkhoaxn11@gmail.com`.
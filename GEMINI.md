
# Tea & Coffee Project

## Tổng quan dự án
Dự án này là một hệ thống e-commerce hoàn chỉnh cho **Tea & Coffee** (Cửa hàng Bánh ngọt và Trà sữa), được xây dựng bằng **Spring Boot** và **Thymeleaf**. Giao diện của hệ thống được thiết kế mô phỏng theo phong cách của **Phúc Long** (Xanh lá cây #006a31 chủ đạo).

### Các công nghệ chính
- **Ngôn ngữ:** Java 17
- **Framework:** Spring Boot 3.x
- **Cơ sở dữ liệu:** MySQL (Spring Data JPA)
- **Giao diện:** Thymeleaf, Bootstrap 5, CSS tùy chỉnh (Phuc Long Style)
- **Công cụ khác:** Lombok, Spring Validation

## Các phân hệ chính
1. **Phân hệ Khách hàng (User):**
   - Đăng ký/Đăng nhập, Quản lý tài khoản.
   - Trang chủ (Banner, Sản phẩm mới/nổi bật).
   - Menu đa cấp: Thức uống (Trà sữa, Cà phê, Trà trái cây, Đá xay) và Bánh (Bánh ngọt, Bánh nóng).
   - Tìm kiếm & Lọc sản phẩm theo danh mục.
   - Chi tiết sản phẩm & Hệ thống Đánh giá (Review).
   - Giỏ hàng & Thanh toán (Checkout).
   - Lịch sử đơn hàng & Trạng thái đơn hàng.

2. **Phân hệ Quản trị viên (Admin):**
   - Dashboard thống kê tổng quan.
   - Quản lý Danh mục & Sản phẩm (Full CRUD: Thêm/Sửa/Xóa).
   - Quản lý Đơn hàng (Duyệt/Đổi trạng thái, Tự động trừ kho nguyên liệu khi hoàn thành).
   - Quản lý Tin tức (Full CRUD).
   - Quản lý Kho nguyên liệu (Inventory): Theo dõi tồn kho và giá trị nguyên vật liệu.

3. **Hệ thống Kho & Cost (Inventory System):**
   - Quản lý `Ingredient` (Nguyên liệu) và `Recipe` (Công thức).
   - Logic: Khi đơn hàng chuyển sang trạng thái `COMPLETED`, hệ thống tự động trừ kho nguyên liệu theo định mức đã thiết lập.

## Cấu trúc dự án
- `com.example.demo.model`: Các thực thể JPA (User, Product, Category, Order, Ingredient, Recipe, Review...).
- `com.example.demo.service`: Logic nghiệp vụ (OrderService tích hợp InventoryService...).

## Biên dịch và Khởi chạy
Yêu cầu: MySQL đang chạy với database `fandb_db`.

### Các câu lệnh chính
- **Cổng mặc định:** 8081 (Truy cập: http://localhost:8081)
- **Xây dựng dự án:**
  ```bash
  cd demo
  ./mvnw clean install
  ```
- **Chạy ứng dụng:**
  ```bash
  ./mvnw spring-boot:run
  ```

### Tài khoản mặc định (Data Seeder)
- **Admin:** `admin` / `admin123`
- **User:** `user` / `user123`
ge
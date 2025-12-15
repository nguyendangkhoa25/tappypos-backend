# Barber Management System - Backend

Spring Boot REST API for managing barber shop orders, employees, and invoices.

## Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- MySQL 8.0 or higher

## Database Setup

Before running the application, create the MySQL database:

```sql
CREATE DATABASE barber_management_db;
USE barber_management_db;
```

## Configuration

Edit `src/main/resources/application.properties` to configure:

- **Database Connection:**
  ```properties
  spring.datasource.url=jdbc:mysql://localhost:3306/barber_management_db
  spring.datasource.username=root
  spring.datasource.password=your_password
  ```

- **JWT Secret (Change in Production):**
  ```properties
  jwt.secret=your-super-secret-key-change-this
  jwt.expiration=86400000
  ```

## Build and Run

### Build the project:
```bash
mvn clean install
```

### Run the application:
```bash
mvn spring-boot:run
```

Or:
```bash
java -jar target/barber-management-system-1.0.0.jar
```

The API will be available at: `http://localhost:8080/api`

## API Documentation

Swagger UI is available at: `http://localhost:8080/api/swagger-ui.html`

## API Endpoints

### Employees
- `POST /api/employees` - Create employee
- `GET /api/employees` - List all employees (paginated)
- `GET /api/employees/{id}` - Get employee by ID
- `PUT /api/employees/{id}` - Update employee
- `DELETE /api/employees/{id}` - Delete employee (soft delete)
- `GET /api/employees/{id}/earnings` - Get employee earnings
- `GET /api/employees/search?keyword=...` - Search employees

### Customers
- `POST /api/customers` - Create customer
- `GET /api/customers` - List all customers (paginated)
- `GET /api/customers/{id}` - Get customer by ID
- `PUT /api/customers/{id}` - Update customer
- `DELETE /api/customers/{id}` - Delete customer (soft delete)
- `GET /api/customers/search?keyword=...` - Search customers

### Orders
- `POST /api/orders` - Create order
- `GET /api/orders` - List all orders (paginated)
- `GET /api/orders/{id}` - Get order by ID
- `PUT /api/orders/{id}` - Update order
- `PUT /api/orders/{id}/assign` - Assign order to employee
- `PUT /api/orders/{id}/complete` - Complete order
- `DELETE /api/orders/{id}` - Delete order (soft delete)
- `GET /api/orders/{id}/bill` - Download bill as PDF
- `GET /api/orders/search?keyword=...` - Search orders
- `GET /api/orders/status/{status}` - Get orders by status

### Invoices
- `POST /api/invoices` - Create invoice
- `GET /api/invoices` - List all invoices (paginated)
- `GET /api/invoices/{id}` - Get invoice by ID
- `GET /api/invoices/order/{orderId}` - Get invoice by order ID
- `PUT /api/invoices/{id}` - Update invoice
- `PUT /api/invoices/{id}/issue` - Issue invoice
- `POST /api/invoices/{id}/sync-external` - Sync invoice with external system
- `DELETE /api/invoices/{id}` - Delete invoice (soft delete)
- `GET /api/invoices/{id}/download` - Download invoice as PDF
- `GET /api/invoices/status/{status}` - Get invoices by status

## Project Structure

```
backend/
├── src/main/java/com/barbershop/
│   ├── BarberManagementApplication.java    # Main application class
│   ├── config/                              # Configuration classes
│   ├── controller/                          # REST controllers
│   ├── service/                             # Business logic
│   ├── repository/                          # Data access layer
│   ├── model/
│   │   ├── entity/                          # JPA entities
│   │   └── dto/                             # Data transfer objects
│   ├── exception/                           # Custom exceptions
│   └── util/                                # Utility classes
├── src/main/resources/
│   └── application.properties               # Application configuration
└── pom.xml                                  # Maven dependencies
```

## Key Features

✅ Order Management (Create, Assign, Complete, Print Bills)
✅ Employee Management (CRUD + Soft Delete + Earnings Tracking)
✅ Customer Management (Create and Track Customers)
✅ Invoice Management (CRUD + External System Integration)
✅ Pagination and Search
✅ Soft Delete Support
✅ PDF Bill Generation
✅ CORS Enabled
✅ Global Exception Handling
✅ Swagger/OpenAPI Documentation

## Technologies

- Spring Boot 3.2.0
- Spring Data JPA with Hibernate
- MySQL Database
- iTextPDF for PDF generation
- Lombok for code generation
- Swagger/OpenAPI for API documentation

## Notes

- All delete operations use soft delete (sets `deleted_at` timestamp)
- Pagination defaults: page=0, size=20
- Employee earnings are calculated from completed orders
- CORS is configured for `http://localhost:3000` and `http://localhost:3001`

## Future Enhancements

- JWT Authentication and Authorization
- Role-based access control (Admin, Manager, Barber)
- Email notifications for orders
- Advanced reporting and analytics
- Mobile app support
- Payment gateway integration


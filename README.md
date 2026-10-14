# Employee Management System

A Spring Boot REST API for managing employees and departments. The backend uses Spring Data JPA, Hibernate, H2 for local development, request/response DTOs, validation, auditing, pagination, projections, batch inserts, an archive datasource, and JWT authentication with roles.

## Run locally

Requirements: Java 17 or later and Maven.

Create a local secrets file once. From PowerShell:

```powershell
cd E:\Deepikaa\Project\EmployeeManagementSystem\employeemanagementsystem
Copy-Item .env.example .env
notepad .env
```

In `.env`, replace the example admin password with a private password of at least 12 characters. Replace the JWT secret with a randomly generated value of at least 32 characters. Save the file. `.env` is ignored by Git; do not commit it. Spring Boot loads it automatically for local runs.

To generate a random JWT secret once in PowerShell, run this and paste the displayed value into `.env` as `AUTH_TOKEN_SECRET`:

```powershell
[Convert]::ToBase64String([System.Security.Cryptography.RandomNumberGenerator]::GetBytes(48))
```

Then, each time you want to run the app:

```powershell
cd E:\Deepikaa\Project\EmployeeManagementSystem\employeemanagementsystem
mvn spring-boot:run
```

The API starts at `http://localhost:8080`. The H2 console is at `http://localhost:8080/h2-console` for local development. Keep the terminal open while using the API.


The default H2 databases are in-memory, so their data is cleared when the application stops. The configured local JWT secret is also for development. Set a strong `AUTH_TOKEN_SECRET` from a secure environment variable in deployed environments.

## Authentication in Postman

### Get an admin token

Send `POST http://localhost:8080/api/auth/login` with **Body → raw → JSON**:

```json
{
  "email": "admin@example.com",
  "password": "the admin password entered in PowerShell"
}
```

Copy `accessToken` from the response. For protected requests, select **Authorization → Bearer Token** and paste the token. Admin tokens can read and modify records.

### Register a regular user

Send `POST http://localhost:8080/api/auth/register`:

```json
{
  "email": "user@example.com",
  "password": "a-password-with-12-or-more-characters"
}
```

Registration returns a USER token. Users can read protected API data but cannot create, update, or delete records. Missing or invalid tokens return `401`; a USER token on an admin-only operation returns `403`.

## Main endpoints

| Method | Endpoint | Access |
| --- | --- | --- |
| POST | `/api/auth/register` | Public; creates USER account |
| POST | `/api/auth/login` | Public; returns JWT |
| GET | `/api/employees` | Authenticated |
| GET | `/api/employees/{id}` | Authenticated |
| GET | `/api/employees/search?name=test&page=0&size=10` | Authenticated |
| POST/PUT/DELETE | `/api/employees/**` | ADMIN |
| GET | `/api/departments` | Authenticated |
| POST/PUT/DELETE | `/api/departments/**` | ADMIN |
| GET | `/api/archive/employees` | ADMIN |
| POST | `/api/archive/employees/{id}` | ADMIN |
| POST | `/api/archive/employees/{id}/restore` | ADMIN |

Create an employee with an admin bearer token using:

```json
{
  "name": "Test Employee",
  "email": "test.employee@example.com",
  "departmentId": 1
}
```

Create a department first if the database is empty. DTO validation errors return `400`, duplicate records return `409`, and missing records return `404`.

## Run the separate frontend

Start the Spring Boot API on port `8080`, then open a second terminal and run:

```powershell
cd E:\Deepikaa\Project\EmployeeManagementSystem\frontend
npm install
npm start
```

Open `http://localhost:5500` in your browser and sign in with an API account, or create a read-only USER account from the sign-in page. The frontend is served by Node.js and Express and supports employee search, department filtering, paging, sorting, and CSV export of the matching employee list. Admins can import employees from a CSV with `Name`, `Email`, and `Department` columns; department values must match existing department names. Admins can also create, edit, delete, and archive employees, manage departments, and restore archived employees. Archiving moves an employee out of the active directory and saves the record in a separate file-based H2 archive database. Restoring returns the employee to the active directory; the department must still exist. The API allows browser requests from the local frontend addresses on port `5500`.

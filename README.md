# Banking App

Simple Spring Boot backend prototype for basic banking operations.

The application supports deposits, withdrawals and retrieving the current account balance.

If a deposit is made to an unknown account number, a new account is created automatically.

## Tech Stack

- Java
- Spring Boot
- Spring Web
- Spring Data JPA
- H2
- Jakarta Validation
- JUnit 5
- Mockito
- MockMvc
- Gradle

## Features

- Deposit money
- Create a new account automatically on first deposit
- Withdraw money
- Prevent negative account balances
- Retrieve the current balance
- Request validation
- Global exception handling
- Service tests
- Controller tests

## Run the Application

No external database is required because the project uses H2.

Start the application on Windows:

```powershell
.\gradlew.bat bootRun
```

The application runs on:

```text
http://localhost:8080
```

## API

Base path:

```text
/api/accounts
```

## Test the API with PowerShell

Start the application first and keep it running.

Open another PowerShell window and execute the following requests in order.

### 1. Create an account with a deposit

Depositing money into an unknown account automatically creates the account.

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/accounts/1/deposit" `
  -ContentType "application/json" `
  -Body '{"amount":100}'
```

Expected result:

```text
accountNumber balance
------------- -------
1             100
```

### 2. Get the current balance

```powershell
Invoke-RestMethod `
  -Method Get `
  -Uri "http://localhost:8080/api/accounts/1"
```

Expected result:

```text
accountNumber balance
------------- -------
1             100
```

### 3. Withdraw money

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/accounts/1/withdraw" `
  -ContentType "application/json" `
  -Body '{"amount":40}'
```

Expected result:

```text
accountNumber balance
------------- -------
1             60
```

### 4. Check the balance again

```powershell
Invoke-RestMethod `
  -Method Get `
  -Uri "http://localhost:8080/api/accounts/1"
```

Expected result:

```text
accountNumber balance
------------- -------
1             60
```

### 5. Test insufficient funds

The balance is currently 60. Trying to withdraw 100 should fail.

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/accounts/1/withdraw" `
  -ContentType "application/json" `
  -Body '{"amount":100}'
```

Expected response:

```json
{
  "status": 400,
  "message": "Insufficient funds",
  "timestamp": "..."
}
```

### 6. Test an invalid amount

Deposit and withdrawal amounts must be greater than zero.

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/accounts/1/deposit" `
  -ContentType "application/json" `
  -Body '{"amount":0}'
```

Expected response:

```json
{
  "status": 400,
  "message": "Amount must be greater than 0",
  "timestamp": "..."
}
```

### 7. Test an unknown account

```powershell
Invoke-RestMethod `
  -Method Get `
  -Uri "http://localhost:8080/api/accounts/99"
```

Expected response:

```json
{
  "status": 404,
  "message": "Account not found",
  "timestamp": "..."
}
```

## Error Handling

The application uses a global exception handler.

Possible errors include:

- `404 Not Found` if an account does not exist
- `400 Bad Request` if the amount is invalid
- `400 Bad Request` if the account does not have enough funds

## Tests

Service tests use JUnit 5 and Mockito.

Controller tests use MockMvc with a mocked service.

Covered service tests:

- withdraw with sufficient funds
- withdraw with insufficient funds
- withdraw with unknown account
- withdraw with invalid amount
- deposit into an existing account
- deposit into an unknown account
- deposit with invalid amount
- get balance for existing account
- get balance for unknown account

Covered controller tests:

- successful deposit
- successful withdrawal
- successful balance retrieval
- unknown account returns 404
- insufficient funds returns 400
- invalid amount returns 400

Run all automated tests with:

```powershell
.\gradlew.bat test --rerun-tasks
```

## Project Structure

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
H2
```
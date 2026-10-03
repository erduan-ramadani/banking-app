# Banking App

Simple Spring Boot backend prototype for basic banking operations.

The application supports deposits, withdrawals, transfers, retrieving the current account balance and viewing the
transaction history.

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
- Transfer money between accounts
- Transaction history for each account
- Track deposits, withdrawals and transfers
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

### 3. Add another deposit

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/accounts/1/deposit" `
  -ContentType "application/json" `
  -Body '{"amount":50}'
```

Expected result:

```text
accountNumber balance
------------- -------
1             150
```

### 4. Withdraw money

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
1             110
```

### 5. Check the balance again

```powershell
Invoke-RestMethod `
  -Method Get `
  -Uri "http://localhost:8080/api/accounts/1"
```

Expected result:

```text
accountNumber balance
------------- -------
1             110
```

### 6. Test insufficient funds

The balance is currently 110. Trying to withdraw 200 should fail.

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/accounts/1/withdraw" `
  -ContentType "application/json" `
  -Body '{"amount":200}'
```

Expected response:

```json
{
  "status": 422,
  "message": "Insufficient funds",
  "timestamp": "..."
}
```

### 7. Test an invalid amount

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

### 8. Test an unknown account

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

### 9. Create a second account

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/accounts/2/deposit" `
  -ContentType "application/json" `
  -Body '{"amount":50}'
```

Expected result:

```text
accountNumber balance
------------- -------
2             50
```

### 10. Transfer money

Transfer 40 from account 1 to account 2.

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/accounts/transfer" `
  -ContentType "application/json" `
  -Body '{
    "senderAccountNumber":"1",
    "recipientAccountNumber":"2",
    "amount":40
  }'
```

Expected result:

```text
accountNumber balance
------------- -------
1             70
```

The response contains the updated balance of the sender account.

### 11. Check both balances after the transfer

Account 1:

```powershell
Invoke-RestMethod `
  -Method Get `
  -Uri "http://localhost:8080/api/accounts/1"
```

Expected result:

```text
accountNumber balance
------------- -------
1             70
```

Account 2:

```powershell
Invoke-RestMethod `
  -Method Get `
  -Uri "http://localhost:8080/api/accounts/2"
```

Expected result:

```text
accountNumber balance
------------- -------
2             90
```

### 12. Test a transfer with insufficient funds

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/accounts/transfer" `
  -ContentType "application/json" `
  -Body '{
    "senderAccountNumber":"1",
    "recipientAccountNumber":"2",
    "amount":100
  }'
```

Expected response:

```json
{
  "status": 422,
  "message": "Insufficient funds",
  "timestamp": "..."
}
```

The account balances remain unchanged.

### 13. Test a transfer to the same account

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/accounts/transfer" `
  -ContentType "application/json" `
  -Body '{
    "senderAccountNumber":"1",
    "recipientAccountNumber":"1",
    "amount":10
  }'
```

Expected response:

```json
{
  "status": 400,
  "message": "Sender and recipient must be different",
  "timestamp": "..."
}
```

### 14. Get the transaction history

```powershell
Invoke-RestMethod `
  -Method Get `
  -Uri "http://localhost:8080/api/accounts/1/history"
```

The result contains all successful transactions for the account, ordered by timestamp descending.

Example:

```text
type                   : TRANSFER
amount                 : 40
senderAccountNumber    : 1
recipientAccountNumber : 2

type                   : WITHDRAWAL
amount                 : 40
senderAccountNumber    :
recipientAccountNumber :

type                   : DEPOSIT
amount                 : 50
senderAccountNumber    :
recipientAccountNumber :

type                   : DEPOSIT
amount                 : 100
senderAccountNumber    :
recipientAccountNumber :
```

Failed operations are not added to the transaction history.

### 15. Get the transaction history of the recipient

```powershell
Invoke-RestMethod `
  -Method Get `
  -Uri "http://localhost:8080/api/accounts/2/history"
```

The successful transfer is also visible in the recipient account history.

Example:

```text
type                   : TRANSFER
amount                 : 40
senderAccountNumber    : 1
recipientAccountNumber : 2

type                   : DEPOSIT
amount                 : 50
senderAccountNumber    :
recipientAccountNumber :
```

### 16. Test transaction history for an unknown account

```powershell
Invoke-RestMethod `
  -Method Get `
  -Uri "http://localhost:8080/api/accounts/99/history"
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
- `422 Unprocessable Content` if the account does not have enough funds
- `400 Bad Request` if sender and recipient accounts are identical

## Tests

Service tests use JUnit 5 and Mockito.

Controller tests use MockMvc with a mocked service.

Covered scenarios include:

- deposits into existing and unknown accounts
- withdrawals with sufficient and insufficient funds
- invalid amounts
- unknown accounts
- successful transfers
- transfers with insufficient funds
- transfers with identical sender and recipient
- transaction history retrieval
- transaction history for unknown accounts
- transaction history for accounts without transactions
- persistence of deposit history
- persistence of withdrawal history
- persistence of transfer history for sender and recipient
- prevention of history entries for failed operations

Run all automated tests with:

```powershell
.\gradlew.bat test --rerun-tasks
```

After the test run, the HTML test report can be opened with:

```powershell
start build/reports/tests/test/index.html
```

The report shows the total number of executed tests, failures and skipped tests.

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

The project separates API handling, business logic and persistence into dedicated layers.

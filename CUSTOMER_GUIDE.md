# Building and testing the customer creation endpoint

This guide records the customer feature built step by step on October 10, 2026, and the Java, Spring, SQL, and terminal concepts learned along the way.

The result is a working `POST /customers` endpoint. It accepts a name and email as JSON, stores a customer in MySQL, and returns the customer with its generated ID. The successful request returned HTTP 200 and customer ID 1.

## 1. Where the files belong

The application’s Java package is `com.example.dummy`. Its source directory is `src/main/java/com/example/dummy`.

“Same package” meant creating the new Java files in that directory and placing `package com.example.dummy;` at the top of each file. Imports allow a file to refer to types from other packages. An accidentally added `CustomObjectInputStream` import was removed because the customer class did not use it.

The feature consists of four files:

| File | Responsibility |
| --- | --- |
| [Customer.java](src/main/java/com/example/dummy/Customer.java) | Represents a customer and maps it to database records. |
| [CustomerRepository.java](src/main/java/com/example/dummy/CustomerRepository.java) | Provides database operations through Spring Data JPA. |
| [CustomerController.java](src/main/java/com/example/dummy/CustomerController.java) | Receives HTTP requests and saves customers. |
| [V1__create_customer.sql](src/main/resources/db/migration/V1__create_customer.sql) | Creates the database table through Flyway. |

## 2. Writing the Customer entity

The class began with three fields: `Long id`, `String name`, and `String email`.

A field declaration consists of an access modifier, a type, a name, and a semicolon. Making these fields `private` keeps other classes from accessing them directly. Public methods provide controlled access instead.

`Long` can hold `null`, so a new customer can have no ID before it is saved. MySQL assigns the ID during insertion.

The class received these persistence annotations, imported from `jakarta.persistence`:

- `@Entity` above the class tells JPA that the class represents persistent records.
- `@Id` above `id` identifies the primary key.
- `@GeneratedValue(strategy = GenerationType.IDENTITY)` tells JPA to use a database-generated identity value. This matches the MySQL `AUTO_INCREMENT` column.

Using `@GeneratedValue` without a strategy selects the default `AUTO` strategy. We explicitly selected `IDENTITY` to match the table’s ID generation.

No `@Table` annotation was added. With this project’s default naming, the `Customer` entity maps to the table named `customer`.

### Constructors and getters

The public constructor accepts a name and email and assigns them to the fields. A constructor has the same name as its class and no return type. In an assignment such as `this.name = name`, `this.name` refers to the object’s field and `name` refers to the constructor parameter.

A protected constructor with no parameters and an empty body was also added. JPA needs a no-argument constructor to create entity instances when loading records. Once an explicit constructor is declared, Java no longer supplies the implicit no-argument constructor.

Three public getters were added: `getId()`, `getName()`, and `getEmail()`. Each takes no arguments and returns its field. They let other code read private data and expose properties for JSON serialization in the response.

## 3. Writing the repository interface

`CustomerRepository.java` was initially declared as a class, then changed to an interface.

A class contains implementation and can represent objects such as customers. An interface describes operations provided by an implementation. Spring supplies the implementation for this repository.

The interface extends `JpaRepository<Customer, Long>`, imported from `org.springframework.data.jpa.repository`:

- `Customer` is the entity type the repository manages.
- `Long` is the type of that entity’s ID.

The interface body stays empty because it inherits operations such as `save(customer)` and `findById(id)`. There was no need to write their database implementation manually.

## 4. Writing the controller

The controller started as an empty public class in the same package. Two annotations were added from `org.springframework.web.bind.annotation`:

- `@RestController` marks the class as a web controller whose method results become response data, such as JSON.
- `@RequestMapping("/customers")` sets the controller’s base path.

### Constructor injection and final

The controller declares a `private final CustomerRepository repository` field. Its public constructor accepts a repository and assigns it with `this.repository = repository`.

Spring passes a repository instance into that constructor when creating the controller. This is constructor injection: the controller receives the dependency it needs to save customers.

`final` prevents the field from being reassigned after initialization. The compiler also requires the field to be initialized during construction. It does not freeze the repository or the database: calling `repository.save(...)` still changes database data.

Constructor injection does not require `final`. Here it expresses that the controller keeps the repository it receives.

### The request record

A nested public record named `CreateCustomerRequest` was added with two components: `String name` and `String email`.

A record is a Java type for holding data. Java generates its constructor and accessor methods, including `name()` and `email()`. The request contains no ID because the database generates it.

The first method version accessed `request.name` and `request.email`. That is allowed here because the record is nested in the controller, but it was changed to the record accessor methods `request.name()` and `request.email()`.

### The create method

The `create` method is public, returns `Customer`, and accepts a `CreateCustomerRequest` parameter named `request`. Its body constructs a customer from the request’s name and email, then returns `repository.save(customer)`.

`@PostMapping` above this method combines with the controller’s base path to handle `POST /customers`.

`@RequestBody` belongs on the method parameter, immediately before `CreateCustomerRequest request`. It tells Spring to deserialize the incoming JSON body into that parameter. It was initially placed above the record declaration, then moved to the parameter. The record defines the data’s shape; the parameter annotation defines where the incoming value comes from.

The method currently returns HTTP 200 by default. We did not add an annotation or response object to return HTTP 201.

## 5. Creating the table with a migration

The directories `db/migration` were created under `src/main/resources`, and the migration was named `V1__create_customer.sql`.

The filename contains two underscores after `V1`. Flyway interprets it as migration version 1, with the description “create customer.”

The completed SQL is:

```sql
CREATE TABLE customer(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100),
    email VARCHAR(255)
    );
```

The SQL syntax lessons were:

- Put a table name after `CREATE TABLE`.
- Enclose the column definitions in parentheses.
- Separate a column’s name, type, and options with spaces, without colons.
- Separate different column definitions with commas.
- Keep `BIGINT AUTO_INCREMENT PRIMARY KEY` together because all three describe the ID column.
- Finish the statement with a semicolon after the closing parenthesis.

The table name matches the entity’s default mapping. `BIGINT` corresponds to the Java `Long` ID, and the two `VARCHAR` columns hold the strings. The name and email columns currently allow SQL `NULL` values.

## 6. Starting MySQL and the application

From the project directory, the database was started with:

```bash
docker compose up -d --wait mysql
```

This starts the `mysql` service in the background and waits for it to become healthy. Then the Java application was started with:

```bash
./mvnw spring-boot:run
```

The supplied startup output confirmed:

- The MySQL container was healthy.
- Spring found one JPA repository interface.
- The application connected to the `shop` database at `127.0.0.1:3307`.
- Flyway successfully applied migration version 1, creating the customer table.
- Tomcat started on port 8080.
- `DummyApplication` started successfully.

Repository metadata warnings appeared during Maven startup, but they did not prevent this run from compiling or starting.

The application terminal stays running while requests are sent from another terminal.

## 7. Sending a customer request with curl

The successful command was:

```bash
curl -i -X POST http://localhost:8080/customers -H 'Content-TYPE: application/json' -d '{"name": "John Doe", "email": "jdoe@example.com"}'
```

Each piece has a purpose:

| Piece | Meaning |
| --- | --- |
| `curl` | Sends an HTTP request. |
| `-i` | Includes response headers and status in the output. |
| `-X POST` | Selects the method handled by the controller. |
| `http://localhost:8080/customers` | Addresses the running application’s customer endpoint. |
| `-H 'Content-TYPE: application/json'` | Declares that the request body contains JSON. Header names are case-insensitive. |
| `-d '…'` | Sends the enclosed text as the request body. |

### First attempt: separate words instead of JSON

The body was initially written as `-d 'name' 'email'`.

That sent only `name` as the body, which is not a valid JSON object. Spring returned HTTP 400. Curl treated the separate `email` argument as another URL and reported that it could not resolve that host.

The lesson: the body must be one JSON object containing named values. JSON uses braces around an object, double quotes around string keys and values, colons between keys and values, and commas between entries.

### Second attempt: JSON without shell quoting

The next attempt supplied the JSON object without enclosing the whole object in single quotes.

Bash split the text at spaces and processed the double quotes rather than preserving them as JSON characters. Curl received multiple arguments and tried to interpret extra pieces as URLs. The result included HTTP 400 and URL parsing errors.

The lesson: wrap the entire JSON object in single quotes. This keeps it as one shell argument and preserves the double quotes required by JSON.

### Successful response

The correctly quoted command returned:

```http
HTTP/1.1 200
Content-Type: application/json
```

```json
{"name":"John Doe","email":"jdoe@example.com","id":1}
```

This confirmed that the endpoint accepted the request, saved the customer, and returned its generated ID.

## 8. How the pieces worked together

1. Curl sent an HTTP POST request with JSON.
2. Spring routed `/customers` to the controller’s `create` method.
3. `@RequestBody` caused the JSON to become a `CreateCustomerRequest`.
4. The method constructed a `Customer` using the record’s accessors.
5. The repository saved the entity through JPA/Hibernate and the MySQL connection.
6. MySQL inserted the row and generated ID 1.
7. The saved customer was returned and serialized as JSON.

Flyway created the table during application startup. It does not handle individual customer requests.

## 9. Optional direct database verification

The following inspection steps were explained after the successful request. No output confirming that they were performed was supplied in the conversation.

Open the MySQL client:

```bash
docker compose exec mysql mysql -u shop -p shop
```

Enter the configured development password, `shop_password`, when prompted. Then run:

```sql
SELECT * FROM customer;
```

The expected result includes John Doe with ID 1. Enter `exit` to leave the client.

## 10. What is implemented now

The feature creates customers through HTTP and persists them in MySQL. It has an entity, repository, controller, request record, and versioned table migration.

The current code does not add request validation, a unique-email rule, custom error handling, or customer lookup/update/delete endpoints. Repository methods exist for additional database operations, but only the creation method has been exposed through HTTP. No automated customer tests were added or run during this walkthrough; verification consisted of successful application startup and the successful curl request.

The main lessons were Java packages, fields, constructors, getters, interfaces, generic type arguments, records, `final` references, constructor injection, persistence and web annotations, SQL table syntax, Flyway migrations, HTTP request bodies, and the difference between JSON syntax and shell quoting.

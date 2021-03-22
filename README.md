## Antaeus

Antaeus (/ænˈtiːəs/), in Greek mythology, a giant of Libya, the son of the sea god Poseidon and the Earth goddess Gaia. He compelled all strangers who were passing through the country to wrestle with him. Whenever Antaeus touched the Earth (his mother), his strength was renewed, so that even if thrown to the ground, he was invincible. Heracles, in combat with him, discovered the source of his strength and, lifting him up from Earth, crushed him to death.

Welcome to our challenge.

## The challenge

As most "Software as a Service" (SaaS) companies, Pleo needs to charge a subscription fee every month. Our database contains a few invoices for the different markets in which we operate. Your task is to build the logic that will schedule payment of those invoices on the first of the month. While this may seem simple, there is space for some decisions to be taken and you will be expected to justify them.

## Instructions

Fork this repo with your solution. Ideally, we'd like to see your progression through commits, and don't forget to update the README.md to explain your thought process.

Please let us know how long the challenge takes you. We're not looking for how speedy or lengthy you are. It's just really to give us a clearer idea of what you've produced in the time you decided to take. Feel free to go as big or as small as you want.

## Developing

Requirements:
- \>= Java 11 environment

Open the project using your favorite text editor. If you are using IntelliJ, you can open the `build.gradle.kts` file and it is gonna setup the project in the IDE for you.

### Building

```
./gradlew build
```

### Running

There are 2 options for running Anteus. You either need libsqlite3 or docker. Docker is easier but requires some docker knowledge. We do recommend docker though.

*Running Natively*

Native java with sqlite (requires libsqlite3):

If you use homebrew on MacOS `brew install sqlite`.

```
./gradlew run
```

*Running through docker*

Install docker for your platform

```
docker build -t antaeus
docker run antaeus
```

### App Structure
The code given is structured as follows. Feel free however to modify the structure to fit your needs.
```
├── buildSrc
|  | gradle build scripts and project wide dependency declarations
|  └ src/main/kotlin/utils.kt 
|      Dependencies
|
├── pleo-antaeus-app
|       main() & initialization
|
├── pleo-antaeus-core
|       This is probably where you will introduce most of your new code.
|       Pay attention to the PaymentProvider and BillingService class.
|
├── pleo-antaeus-data
|       Module interfacing with the database. Contains the database 
|       models, mappings and access layer.
|
├── pleo-antaeus-models
|       Definition of the Internal and API models used throughout the
|       application.
|
└── pleo-antaeus-rest
        Entry point for HTTP REST API. This is where the routes are defined.
```

### Main Libraries and dependencies
* [Exposed](https://github.com/JetBrains/Exposed) - DSL for type-safe SQL
* [Javalin](https://javalin.io/) - Simple web framework (for REST)
* [kotlin-logging](https://github.com/MicroUtils/kotlin-logging) - Simple logging framework for Kotlin
* [JUnit 5](https://junit.org/junit5/) - Testing framework
* [Mockk](https://mockk.io/) - Mocking library
* [Sqlite3](https://sqlite.org/index.html) - Database storage engine

Happy hacking 😁!

### Seb's reasoning

>As most "Software as a Service" (SaaS) companies, Pleo needs to charge a subscription fee every month. 
>Our database contains a few invoices for the different markets in which we operate. 
>Your task is to build the logic that will schedule payment of those invoices on the first of the month.
>While this may seem simple, there is space for some decisions to be taken and you will be expected to justify them.

Design choices 

I assume that we operate in a single Bounded Context from Domain-Driven-Design perspective and Antaeus's covering currently Invoice/Billing domain. 

We have a Customer entity that's represented by Customer ID and currency.
We have an Invoice entity that's represented by Invoice ID, customer, amount and status.

We have an external Payments Provider service that could result in 
- either successful payment
- rejection of payment
- technical errors (no customer found, currency mismatch, network error)

The task is to build the logic that will schedule payment of those invoices on the first of month.

The first decision is that I made is to separate the "scheduler" part from main application. Two reasons
- first if we intend run the Antaeus in HA mode (multiple instances of application), running a scheduler in each instance does not look like the good idea.
- if we use the container orchestrator (like Kubernetes) we can easily manage [cron jobs on a repeating schedule.](#https://kubernetes.io/docs/concepts/workloads/controllers/cron-jobs/)

It implies that our application should expose the API to pay for pending invoices.

So how we should design the endpoint?

I'm one of the REST skeptics. My criticism is coming from DDD/CQRS position, as my thinking is much more action/intent oriented than REST noun/resource orientation.

Saying that, I'm thinking about function that we need to perform. For me it's a command like ChargeForPendingInvoices()

I think that we can easily translate it to endpoint such 
POST rest/v1/billings/charge-for-pending-invoices

1. I start with adding such REST endpoint. As a novice Kotlin developer, I don't want to spend much time on figuring out the way we could write nice API tests here, so I am taking conscious shortcut here. 
I think that API could initially return the list invoices ids' that were paid in the process. A quick test, as expected give me empty list
   ```
   curl -X POST localhost:7000/rest/v1/billings/charge-for-pending-invoices
   []
   ```
2. I find out which invoices are pending, so I could attempt charging process. 
   I extend the AntaeusDal, so it's possible to fetch invoices by status. 
   I add the test package and test if indeed the fetching works correctly and is filtering by status. 
   I copied the init mechanism for SQLite from AntaeusApp. 
   
3. First attempt to connect charging process with fetching pending invoices and marking successfully charged invoices as paid. Happy path scenario is covered.
I added tests for BillingService that checks if charging and marking as paid processes are connected properly. Also, new function in AntaeusDal got tested.
   
4. There're three immediate not-happy kinds of scenarios that I should think about
   * payment provider return false that account did not allow the charge
      * this is a domain error, product owner/manager should decide what do in such case. 
      I would say for purposes of the task, that we should mark the account as "account-did-not-allow-charge" that allows the CS team to take an action. 
        That error is retriable, so we can continue to try charge for invoice.
        Invoice can remain in pending state.
   * `CustomerNotFoundException` / `CurrencyMismatchException`
      * this is a domain error, product owner/manager should decide what do in such case.
        I would say for purposes of the task, that we should flag the invoice as "customer-not-found" or "currency-mismatch-exception" 
        that allows the CS team to take an action. That error is not-retriable, so we should not continue to try charge for customer invoices before its fixed.
        Invoice should change to failed state.
   * `NetworkException`: when a network error happens.
      * this is a technical error, 
        This is a lovely error, as we can not determine if charge was successful or not.
        Our integration with payment provider should have the option to provide "idempotency" mechanism.
        It's necessary to make sure that when we retry a charge for already charged invoice
        Payment provider should reject the charge as a duplicate (it can be based on some stable id - like e.g. invoice id)
        For the purpose of the task, I assume that payment provider returns me True and does not charge twice
        Invoice can remain in pending state.
   
   That implies that `AccountDidNotAllowToCharge` and `NetworkException` are two events that allow invoices to retry charging safely
   `CustomerNotFoundException` / `CurrencyMismatchException` are two events that blocks recharge process unless it's solved through manual process 
   that I put a bit out of the scope for this task.
   
   That implies that I need to mark invoices as failed if they ended up in such a state.
    
5. I would like to rework the exceptions, as I hope that we can get better in Kotlin!

   So I did a quick search and find Arrow library which reminds me cats or scalaz from Scala!

Checkpoint

   ```
   curl -X POST localhost:7000/rest/v1/billings/charge-for-pending-invoices
   
   [{"id":1,"type":"CustomerAccountDidAllowChargePaymentException"},{"id":11,"type":"CustomerAccountDidAllowChargePaymentException"},(...),{"id":991,"type":"SuccessfullyCharged"}]
   ```

Current mocked payment provider gives randomly successful payment or that customer account did not allow charge
And every 10th invoice in database is pending one (9/10 are already paid)
So that result makes sense for me. 

If I repeat charging process I would expect a bunch of more paid invoices
 ```
   curl -X POST localhost:7000/rest/v1/billings/charge-for-pending-invoices
   
   [{"id":1,"type":"SuccessfullyCharged"},{"id":11,"type":"CustomerAccountDidAllowChargePaymentException"},(...),{"id":971,"type":"SuccessfullyCharged"},{"id":981,"type":"CustomerAccountDidAllowChargePaymentException"}]
   ```
That's good, for invoice #1 (that failed first time) charging process was repeated, Charging process for Invoice #991 (that was paid) was not repeated.

After few repeats, I see that there're no more pendign invoices 

```
curl -X POST localhost:7000/rest/v1/billings/charge-for-pending-invoices
[]
```

Quick look into SQLite on Docker says that it's ok. All invoices are paid

```
sqlite> SELECT count(*) FROM Invoice WHERE status='PAID';
1000
```

Let's commit and think about #6.




   
  
           








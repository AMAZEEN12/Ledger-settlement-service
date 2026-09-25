# Endpoint Log

| Request | Status | Body summary | Time |
|---|---|---|---|
| POST `/payments` — valid payment for MR-4471, amountMinor 128450, currency GBP | 201 Created | Payment recorded; Location header returned | 20 September 2026, 23:32:27 |
| POST `/payments` — negative amount | 400 Bad Request | Validation problem detail: amountMinor must be positive | 20 September 2026, 23:32:27 |
| POST `/payments` — blank merchantId | 400 Bad Request | Validation problem detail: merchantId must not be blank | 20 September 2026, 23:32:27 |
| GET `/payments/settlement?merchantId=MR-4471` | 200 OK | Settlement amount owed: 124469 | 20 September 2026, 23:32:27 |
| GET `/payments/settlement?merchantId=UNKNOWN` | 404 Not Found | Problem detail: unknown merchant | 20 September 2026, 23:32:27 |
# System Design — Large-Scale E-Commerce Platform (Amazon / Flipkart class)

> **Format:** Senior Java Developer / Staff-level system design round (45–60 min).
> **Convention used throughout:** `⟵ SAY THIS` marks the exact sentence to speak out loud when the interviewer drills that box. `⚖️ TRADE-OFF` marks a decision where you must be able to argue *both* sides — interviewers score the justification, not the choice.

---

## 0. How to drive the round (timeboxing)

| Minutes | Phase | What you actually do |
|---|---|---|
| 0–5 | **Scope & requirements** | Ask clarifying questions. Cut scope out loud. Write FR/NFR on the board. |
| 5–8 | **Back-of-envelope** | Traffic, storage, read:write ratio. This picks your datastores for you. |
| 8–12 | **API contract** | 5–6 endpoints. Shows you think in contracts, not boxes. |
| 12–22 | **High-level diagram** | Draw the boxes. Do not deep-dive yet. Say "I'll come back to X." |
| 22–45 | **Deep dives** | Interviewer picks 2–3. Have 8 loaded. Inventory + Saga are always asked. |
| 45–55 | **Failure / scale / bottleneck** | Single points of failure, hot keys, flash sale, cache stampede. |
| 55–60 | **Trade-offs & wrap** | Explicitly list what you'd do differently at 10x. |

**The #1 mistake at senior level:** drawing 20 microservices in minute 3. Start with a monolith-shaped diagram and *split it with a reason*. Every split must be justified by an independent scaling axis, an independent failure domain, or an independent team.

---

## 1. Requirements

### 1.1 Functional (scoped in)

1. **Identity** — register, login (password + OTP + social), session management.
2. **Catalog & Search** — browse categories, full-text search with facets, product detail page (PDP).
3. **Cart** — guest cart, logged-in cart, guest→user merge on login, multi-seller cart.
4. **Pricing & Promotions** — base price, seller price, coupons, cart-level discounts.
5. **Checkout & Orders** — address selection, payment, order placement, order history, cancellation.
6. **Inventory** — multi-warehouse, multi-seller stock; **must not oversell**.
7. **Payments** — cards, UPI/wallets, COD; refunds.
8. **Fulfilment & Delivery** — pick/pack/ship, carrier allocation, tracking, delivery confirmation.
9. **Notifications** — email/SMS/push at each state transition.

### 1.2 Explicitly scoped out (say this out loud — it buys you time)

Seller onboarding portal, recommendations/ML ranking, reviews & ratings, returns/RMA (mentioned but not designed), fraud ML, ads platform, analytics warehouse.

> ⟵ SAY THIS: *"I'm going to scope out recommendations and returns so I can go deep on inventory consistency and the order saga, which I think are the interesting parts. Happy to come back if you'd prefer those."*

### 1.3 Non-functional (this is where senior candidates separate)

| NFR | Target | Consequence on design |
|---|---|---|
| Availability — **browse path** | 99.99% (52 min/yr) | Multi-AZ, multi-region read replicas, aggressive caching, graceful degradation |
| Availability — **checkout path** | 99.95% | Can be slightly lower; correctness > availability here |
| Latency | PDP p99 < 200 ms, search p99 < 300 ms, checkout p99 < 1.5 s | CDN + edge cache, denormalized read models |
| **Consistency — inventory** | Strong (no oversell) | Single-writer per SKU, conditional atomic decrement |
| **Consistency — catalog** | Eventual (seconds) | CDC → search index, cache TTL |
| **Consistency — payments** | Exactly-once *effect* | Idempotency keys + outbox + reconciliation |
| Durability | Zero order loss | WAL + sync replication + outbox before ack |
| Scale spike | 100× during flash sale | Queue-based load levelling, admission control, virtual waiting room |
| Compliance | PCI-DSS, GDPR/DPDP | Never store PAN; tokenize at PSP; PII encryption + right-to-erasure |

### 1.4 Back-of-the-envelope

```
Users                    100 M registered, 10 M DAU
Page views               10 M DAU × 30 views    = 300 M/day
                         300M / 86400           ≈ 3.5 K RPS average
                         peak factor 10×        ≈ 35 K RPS peak (read)

Orders                   1 M orders/day         ≈ 12 orders/s average
                         flash-sale peak        ≈ 1 200 orders/s
Read : Write             ~1000 : 1              → read-optimized everything

Catalog                  100 M SKUs × 5 KB      = 500 GB  (+ images → S3/CDN, ~50 TB)
Search index             100 M docs × 2 KB      = 200 GB  → ~10 ES shards, replicated
Cart (Redis)             5 M active carts × 4 KB= 20 GB   → fits one Redis cluster easily
Orders                   1 M/day × 2 KB × 365×5 = 3.6 TB  → shard by user_id, archive cold to S3
Inventory                100 M SKU-warehouse rows × 200 B = 20 GB → fully in-memory feasible
```

> ⟵ SAY THIS: *"The 1000:1 read-to-write ratio is the single most important number on this board. It tells me the catalog side is a caching and fan-out problem, and the order side is a correctness and concurrency problem. Those are two different systems with two different consistency models, so I'll split them at the very first cut."*

---

## 2. API surface (contract-first)

```http
POST   /v1/auth/login                     → { accessToken (15m JWT), refreshToken (opaque, 30d) }
POST   /v1/auth/refresh                   → rotates refresh token (reuse detection)

GET    /v1/search?q=&f=brand:apple&page=  → faceted results  (cached, CDN-able)
GET    /v1/products/{sku}                 → PDP aggregate    (cached, ETag)

POST   /v1/carts/{cartId}/items           Idempotency-Key: <uuid>
DELETE /v1/carts/{cartId}/items/{sku}
POST   /v1/carts/{cartId}/merge           (guest cart → user cart on login)

POST   /v1/checkout/sessions              → price lock + inventory RESERVATION (TTL 15 min)
POST   /v1/orders                         Idempotency-Key: <uuid>   → 202 Accepted + orderId
GET    /v1/orders/{orderId}               → state machine status (client polls / SSE)

POST   /v1/payments/webhook               ← PSP callback (signed, idempotent, replay-safe)
GET    /v1/shipments/{orderId}/tracking   → carrier events
```

**Three senior-level details to call out:**

1. `POST /v1/orders` returns **202 Accepted**, not 201. Order placement is an asynchronous saga; the client polls `GET /orders/{id}` or receives an SSE/websocket push. This is what lets you survive a 1200 orders/s spike — you decouple *accepting* the order from *completing* it.
2. **`Idempotency-Key` on every mutating call.** Mobile networks retry. Users double-tap. Without this you get duplicate orders and duplicate charges.
3. **Cursor pagination, not offset.** `OFFSET 100000` makes the DB scan and discard 100k rows. Use `WHERE (created_at, id) < (?, ?) ORDER BY created_at DESC, id DESC LIMIT 20`.

---

## 3. High-level architecture

```
                            ┌──────────────┐
   Web / iOS / Android ────►│   CDN edge   │  static assets, product images,
                            │ (CloudFront) │  cached search & PDP JSON (short TTL)
                            └──────┬───────┘
                                   │ cache MISS / all writes
                                   ▼
                          ┌────────────────────┐
                          │  WAF + Global LB   │  DDoS, geo-routing (Route53 latency)
                          └─────────┬──────────┘
                                    ▼
                     ╔══════════════════════════════╗
                     ║        API GATEWAY           ║  AuthN (JWT verify), rate limit,
                     ║ (Spring Cloud GW / Kong/Envoy)║ request coalescing, routing,
                     ╚══════════════════════════════╝  BFF fan-out, circuit breaking
                       │      │       │        │      │
      ┌────────────────┘      │       │        │      └───────────────────┐
      ▼                       ▼       ▼        ▼                          ▼
┌───────────┐  ┌───────────┐ ┌─────┐ ┌──────────┐ ┌──────────┐  ┌──────────────┐
│  Identity │  │  Catalog  │ │Search│ │   Cart   │ │ Pricing/ │  │  Order       │
│  Service  │  │  Service  │ │Svc  │ │  Service │ │ Promo    │  │  Orchestrator│
└─────┬─────┘  └─────┬─────┘ └──┬──┘ └────┬─────┘ └────┬─────┘  └──────┬───────┘
      │              │          │         │            │               │
   Postgres      Postgres   Elastic-    Redis       Postgres        Postgres
   +Redis        (sharded)  search      Cluster    +Redis(hot)     (sharded by
   (sessions)        │      (200GB)     (20GB)                      order_id)
                     │                                                  │
                     └──── Debezium CDC ──┐                             │
                                          ▼                             ▼
                     ╔════════════════════════════════════════════════════════╗
                     ║              KAFKA  (event backbone, 3 AZ)             ║
                     ║  topics: product.changed | order.events | inventory.*  ║
                     ║          payment.events | shipment.events | dlq.*      ║
                     ╚════════════════════════════════════════════════════════╝
                       │        │           │            │            │
      ┌────────────────┘        │           │            │            └────────┐
      ▼                         ▼           ▼            ▼                     ▼
┌───────────┐          ┌──────────────┐ ┌────────┐ ┌──────────────┐  ┌────────────────┐
│ Inventory │          │   Payment    │ │Fulfil- │ │ Notification │  │  Analytics /   │
│  Service  │          │   Service    │ │ ment   │ │   Service    │  │  Data Lake     │
└─────┬─────┘          └──────┬───────┘ └───┬────┘ └──────────────┘  └────────────────┘
      │                       │             │
 Redis (hot counters)    Postgres      Postgres          ┌───────────────┐
 + Postgres (SoR)        (ledger,      (WMS)             │  Shipping /   │
                          double-entry)  │               │  Carrier Svc  │
                          │              └──────────────►└───────┬───────┘
                          ▼                                      ▼
                 ┌─────────────────┐                    ┌──────────────────┐
                 │  PSP (Stripe /  │                    │ Carriers (FedEx, │
                 │  Razorpay/Adyen)│                    │ Delhivery, own)  │
                 └─────────────────┘                    └──────────────────┘

Cross-cutting spine (touches every box above):
  Observability : Micrometer → Prometheus → Grafana | OpenTelemetry traces → Jaeger
                  structured JSON logs → Fluent Bit → Loki/ELK  (traceId propagated everywhere)
  Config/Secrets: Spring Cloud Config / Consul | Vault or AWS Secrets Manager
  Deploy        : Kubernetes (EKS), 3 AZ, HPA on RPS+latency, blue-green for stateless,
                  expand-contract migrations for stateful
```

### 3.1 Why these service boundaries (the justification the interviewer wants)

| Service | Split because… | Datastore | Consistency |
|---|---|---|---|
| **Identity** | Independent failure domain; security blast-radius isolation; different compliance rules | Postgres + Redis (sessions/refresh tokens) | Strong |
| **Catalog** | Write-rare, read-enormous; owned by merchandising team | Postgres (sharded by seller) + Redis | Eventual to readers |
| **Search** | Completely different access pattern (inverted index, faceting); scales on query volume | Elasticsearch / OpenSearch | Eventual (seconds) |
| **Cart** | Extreme write rate, low durability requirement, tolerates loss better than orders | Redis Cluster + async Postgres snapshot | Session-consistent |
| **Pricing/Promo** | Rules change hourly; must be independently deployable without touching checkout | Postgres + Redis, rules cached | Eventual, with price-lock at checkout |
| **Inventory** | **The one place needing strong consistency + highest contention** — deserves its own scaling axis | Redis (atomic counters) + Postgres (system of record) | **Strong** |
| **Order** | Long-lived state machine, saga orchestrator, legal record | Postgres sharded by `order_id`, outbox table | Strong within shard |
| **Payment** | PCI scope isolation — keep card data out of every other service | Postgres double-entry ledger | Strong + idempotent |
| **Fulfilment/Shipping** | Integrates with slow, flaky external systems; needs its own retry/backoff domain | Postgres | Eventual |
| **Notification** | Pure fan-out consumer; can lag without breaking anything | Redis + Postgres (dedupe) | Eventual, at-least-once |

> ⟵ SAY THIS: *"Note that only three services need strong consistency — inventory, order and payment. Everything else is eventually consistent, and that's a deliberate choice: it lets me cache the read path aggressively and keep the checkout path narrow enough to reason about."*

---

## 4. The full flow — login → cart → payment → delivery

This is the narrative you walk the interviewer through. Nine stages. Each stage names the component, the datastore, and the failure mode.

### Stage 1 — Login / Authentication

```
Client                 API GW           Identity Svc      Redis          Postgres
  │                      │                   │              │               │
  │ POST /auth/login ───►│                   │              │               │
  │ {email, password}    │── route ─────────►│              │               │
  │                      │                   │─ SELECT user ───────────────►│
  │                      │                   │◄─ argon2 hash ───────────────│
  │                      │                   │ verify(password, hash)       │
  │                      │                   │ (Argon2id, ~100ms — this is  │
  │                      │                   │  deliberate: brute-force cost)│
  │                      │                   │              │               │
  │                      │                   │─ SETEX rt:{jti} 30d ───────► │
  │◄─ 200 {accessJWT 15m, refreshToken} ─────│              │               │
  │                      │                   │              │               │
  │ POST /carts/merge ──►│──────────────────►│ guest cart → user cart       │
```

**Design decisions:**

- **Access token = short-lived (15 min) signed JWT (RS256).** The gateway verifies it locally with the public key from a JWKS endpoint — **zero network hop per request**. At 35 K RPS, calling Identity for every request would need its own fleet and would make Identity a global SPOF.
- **Refresh token = opaque, random, stored server-side in Redis.** JWTs can't be revoked; opaque tokens can. Logout, password change and "sign out all devices" all need revocation.
- **Refresh-token rotation with reuse detection:** every refresh issues a new token and invalidates the old one. If an old token is presented again, that means it was stolen → revoke the entire token family and force re-login.
- **Guest cart merge:** guest carts are keyed by an anonymous cookie `cartId`. On login you merge into the user cart — union the SKUs, take `max(qty)` on collision (never sum: users get surprise duplicates), and re-validate every price and stock level, because the guest cart may be days old.

> ⚖️ **TRADE-OFF — stateless JWT vs. server-side session.**
> JWT: no lookup per request, scales horizontally, but **cannot be revoked before expiry** and the payload is only base64 (never put PII in it).
> Sessions: instant revocation, small tokens, but a Redis lookup on every request and Redis becomes a hard dependency of every call.
> **Chosen: hybrid.** Short JWT for the access path (performance), opaque server-side refresh token for the control path (revocability). The 15-minute window is the accepted risk — if you can't tolerate 15 minutes of a leaked token, add a Redis "revoked jti" bloom filter checked at the gateway, at the cost of one lookup.

**Failure mode:** Identity down → users with a valid unexpired JWT keep browsing and can even keep adding to cart. Only login and refresh fail. That's the point of separating them.

---

### Stage 2 — Home page / Search / Browse

```
Client → CDN ──(hit ~85%)──► response in <30ms
          │ miss
          ▼
      API Gateway ──► Search Service ──► Elasticsearch (query + facet aggregation)
                             │
                             └──► Catalog Service (hydrate top-20 SKUs) ──► Redis ──► Postgres
```

- Elasticsearch holds a **denormalized search document** per SKU: title, brand, attributes, category path, price band, rating, availability flag, popularity score. Faceting (`brand`, `price range`, `rating`) is an ES aggregation — this is exactly what an inverted index + doc-values are for; doing it in SQL means `GROUP BY` over 100 M rows per query.
- ES returns **IDs plus display fields only**. Anything volatile (real price, real stock) is hydrated from Redis/Catalog so the index can lag without showing wrong prices.
- The index is fed by **Debezium CDC on the Catalog Postgres WAL → Kafka → indexer**. Not dual-writes.

> ⚖️ **TRADE-OFF — CDC vs. dual-write vs. batch reindex.**
> Dual-write (app writes DB *and* ES) is the obvious choice and it is **wrong**: the two writes aren't atomic, so a crash between them permanently desynchronizes the index and you have no way to detect it.
> CDC reads the WAL — the DB commit *is* the event source, so the index can lag but can never diverge. Cost: Debezium/Kafka Connect is real operational surface, and schema changes need care.
> Batch reindex is the safety net (nightly full rebuild) — not the primary path.

**Cache stampede protection (they will ask):** when a hot key expires, 10 000 requests miss simultaneously and stampede the DB. Three defences, use all three: (1) **probabilistic early expiry** — refresh at `ttl × random(0.8,1.0)` so keys don't expire in lockstep; (2) **single-flight / request coalescing** — one thread takes a Redis `SETNX` lock and recomputes, others serve stale; (3) **stale-while-revalidate** — serve the expired value and refresh in the background.

---

### Stage 3 — Product Detail Page (PDP)

The PDP is a **fan-out aggregation**, and it is the highest-QPS composite call in the system.

```
GET /v1/products/{sku}          → BFF / Gateway aggregator
        ├─► Catalog     : title, images, description, specs   [cache 1h]
        ├─► Pricing     : final price after seller + promo    [cache 60s]
        ├─► Inventory   : availability *flag* only            [cache 5s]
        ├─► Reviews     : rating summary                      [cache 10m]
        └─► Delivery    : "delivery by Thu" for user's pincode[cache 30m per pincode]
                                    ↓
                          assemble → ETag → CDN
```

- All five calls in **parallel** (`CompletableFuture.allOf` / Reactor `Mono.zip`), so latency = slowest call, not the sum.
- **Different TTLs per fragment** — that's the whole trick. Description can be stale for an hour; stock cannot be stale for more than a few seconds.
- **Never show an exact stock count** on the PDP, only a boolean `inStock` (or the marketing string "Only 3 left!" computed from a slightly stale value). Exact counts force strong reads on the hottest path in the system for zero business value.
- Each dependency is wrapped in a **circuit breaker with a fallback**: if Reviews is down, render the PDP without ratings. If Delivery-estimate is down, show "delivery dates at checkout". **The page must never fail because a non-essential fragment failed.** This is graceful degradation, and it's the difference between 99.9% and 99.99%.

---

### Stage 4 — Add to Cart

```
POST /v1/carts/{cartId}/items   { sku, qty, sellerId }   Idempotency-Key: <uuid>
        │
        ▼
   Cart Service
        ├─ 1. validate SKU exists + is sellable      (Catalog, cached)
        ├─ 2. soft stock check                       (Inventory, cached, NOT a reservation)
        ├─ 3. HSET cart:{cartId} {sku} {qty,price,ts}  ── Redis, O(1)
        ├─ 4. EXPIRE cart:{cartId} 30d
        └─ 5. emit cart.item.added → Kafka  (analytics, abandoned-cart mailer)
```

**Critical point — adding to cart does NOT reserve inventory.** ⟵ SAY THIS explicitly; it's a favourite trap.

> ⚖️ **TRADE-OFF — reserve at add-to-cart vs. at checkout.**
> Reserve-on-add gives the nicest UX (what's in your cart is yours) but is catastrophic at scale: 100 carts hold stock for every 1 that converts, so real inventory is starved by abandoned carts, and every add-to-cart becomes a write to the most contended row in the database. Reserve-at-checkout means a user can be told "out of stock" at the last step — worse UX, but the inventory stays truthful and add-to-cart stays a cheap Redis write.
> **Chosen: reserve at checkout**, with a *soft* (cached, non-authoritative) availability check at add-time so the common case still shows the right thing early.
> *Exception:* for flash sales and ticketing, invert this — reserve at add-to-cart with a short TTL, because there the scarcity itself is the product.

**Why Redis for cart:**

| | Redis Hash | Postgres row | Client-side (localStorage) |
|---|---|---|---|
| Write latency | < 1 ms | 5–10 ms | 0 |
| Cross-device | ✅ | ✅ | ❌ |
| Durability | AOF `everysec` — can lose ~1 s | Full | None |
| Cost at 5 M carts | ~20 GB RAM | fine but hot | free |

Cart data is **low-value, high-churn** — losing one second of cart writes on a Redis failover is an acceptable business loss; losing an order is not. So: Redis is the primary store, with an **async snapshot to Postgres** every N mutations so a full cluster loss degrades to "your cart is a few minutes old" instead of "your cart is gone".

**Store the price at add-time**, then re-price at checkout and show the delta. Never silently charge the new price; never honour a 6-month-old price. This is a business rule you should state, not assume.

---

### Stage 5 — Checkout session (price lock + inventory reservation)

This is where the system flips from *eventually consistent and cheap* to *strongly consistent and expensive*. Say that transition out loud.

```
POST /v1/checkout/sessions   { cartId, addressId }
        │
        ▼
  Checkout Orchestrator
        │
        ├─ 1. Load cart from Redis, re-validate each SKU (still sellable? seller active?)
        ├─ 2. Re-price the whole cart  ──► Pricing Svc (apply coupons, taxes, shipping)
        │       └─ PRICE LOCK: persist the computed price, valid 15 min
        ├─ 3. RESERVE inventory  ──────► Inventory Svc  (atomic, per SKU, TTL 15 min)
        │       └─ if any SKU fails → release the ones already taken, return 409 with
        │          the exact failing SKUs so the UI can show "2 of 5 items unavailable"
        ├─ 4. Fraud / risk pre-check (velocity rules, device fingerprint)
        └─ 5. Create PaymentIntent at PSP ──► return client_secret to the browser

  Returns: { checkoutSessionId, lockedTotal, reservationExpiresAt, paymentClientSecret }
```

**The reservation is the heart of the design.** It is a *soft-state, TTL-bounded claim* on stock. It is what makes "never oversell" compatible with "the user takes 4 minutes to type a card number".

**Inventory reservation, executed atomically in a Redis Lua script** (single-threaded → the whole script is one atomic operation, no distributed lock needed):

```lua
-- KEYS[1] = inv:{sku}:{warehouse}   ARGV[1] = qty  ARGV[2] = reservationId  ARGV[3] = ttl
local available = tonumber(redis.call('HGET', KEYS[1], 'available'))
if available == nil or available < tonumber(ARGV[1]) then
  return -1                                    -- insufficient stock
end
redis.call('HINCRBY', KEYS[1], 'available', -tonumber(ARGV[1]))
redis.call('HINCRBY', KEYS[1], 'reserved',   tonumber(ARGV[1]))
redis.call('SETEX', 'resv:' .. ARGV[2], tonumber(ARGV[3]), ARGV[1])
return 1
```

> ⚖️ **TRADE-OFF — Redis Lua vs. `SELECT … FOR UPDATE` vs. optimistic locking.**
> **`SELECT … FOR UPDATE`** (pessimistic): correct and simple, but on a flash-sale SKU every one of 10 000 concurrent buyers queues on one row lock. Throughput collapses to `1/lock_hold_time`, connection pools exhaust, and you get lock-wait timeouts cascading into the whole DB.
> **Optimistic locking** (`UPDATE … WHERE version = ?` + retry): great when contention is *low* — no locks held, no deadlocks. Under high contention it degenerates: most transactions fail and retry, wasting work, and effective throughput drops as contention rises.
> **Redis Lua**: ~100 K ops/s per key, atomic, no lock held across a network round-trip. Cost: Redis is now on the critical correctness path, and it's an in-memory store — so Postgres remains the **system of record** and Redis is a *derived, rebuildable* counter, reconciled continuously.
> **Chosen: Redis Lua for the hot path, Postgres for the ledger.** Postgres gets an append-only `inventory_ledger` (every reserve/commit/release as a row); a reconciler replays it to rebuild Redis after a failover, and a continuous job diffs the two and alarms on drift.

**Handling the reservation TTL expiry** — three mechanisms, because one is never enough:
1. **Redis keyspace notification** on `resv:{id}` expiry → release handler. Fast, but Redis notifications are **fire-and-forget and can be lost**.
2. **A durable delay queue** (Kafka delayed topic / RabbitMQ TTL+DLX / a `due_at` column polled by a scheduler) — the reliable backstop.
3. **A sweeper job** every minute over `reservations WHERE state='HELD' AND expires_at < now()` — the janitor that catches whatever both of the above dropped.

> ⟵ SAY THIS: *"I never rely on a single expiry mechanism for something that leaks money. Redis notifications are best-effort, so they're an optimization; the durable sweeper is the correctness guarantee."*

---

### Stage 6 — Place order (the Saga)

Now the interesting part. Placing an order touches **Order, Inventory, Payment, Fulfilment, Notification** — five services, five databases. There is no distributed transaction. (Two-phase commit would work, and you should say why you rejected it: 2PC holds locks across the network, and a coordinator failure leaves participants blocked indefinitely — an availability catastrophe on your revenue path.)

**Chosen: orchestrated Saga with compensating transactions + Transactional Outbox.**

```
                         ┌──────────────────────────────┐
   POST /v1/orders  ────► │   ORDER ORCHESTRATOR         │  (state machine, persisted)
   Idempotency-Key        │   one row per saga, one row  │
                         │   per step, all in Postgres  │
                         └──────────────┬───────────────┘
                                        │
   ┌─────────────┬──────────────┬───────┴───────┬───────────────┬──────────────┐
   ▼             ▼              ▼               ▼               ▼              ▼
 CREATED   ─► PAYMENT_    ─► INVENTORY_  ─► FULFILMENT_  ─► SHIPPED   ─►  DELIVERED
              AUTHORIZED     COMMITTED      SCHEDULED
   │             │              │               │
   │ compensate  │ compensate   │ compensate    │ compensate
   ▼             ▼              ▼               ▼
 CANCELLED  ◄─ VOID/REFUND ◄─ RELEASE_STOCK ◄─ CANCEL_PICKLIST
```

**Step by step, with the compensation for each:**

| # | Step | Forward action | Compensation if a later step fails |
|---|---|---|---|
| 1 | Validate | Re-check price lock + reservation still alive | — (nothing done yet) |
| 2 | Persist order | `INSERT order (state=CREATED)` + outbox row, **one local transaction** | Mark `CANCELLED` |
| 3 | Authorize payment | PSP auth (hold funds, don't capture) | `VOID` the authorization |
| 4 | Commit inventory | Reservation → permanent deduction (`reserved -= q`) | Release stock back to `available` |
| 5 | Capture payment | Capture the authorized amount | `REFUND` (a *new* ledger entry — never delete) |
| 6 | Schedule fulfilment | Create picklist in WMS | Cancel picklist |
| 7 | Notify | Confirmation email/SMS/push | Send cancellation notice |

**Why authorize *before* committing inventory, then capture after:** authorization is cheap to reverse (a void, invisible to the customer, no fee); an inventory commit is expensive to reverse (the stock may already have been sold to someone else in between). **Order the saga so that the easily-compensated, most-likely-to-fail step runs first.** That's the general rule: *risky and reversible first, irreversible last.*

**The Transactional Outbox** — the mechanism that makes all of this safe:

```
BEGIN;
  INSERT INTO orders (id, user_id, state, total, ...) VALUES (...);
  INSERT INTO outbox (id, aggregate_id, topic, payload, created_at)
         VALUES (uuid(), :orderId, 'order.events', :json, now());
COMMIT;                       -- one ACID transaction, one database

-- a separate relay (Debezium on the outbox table, or a polling publisher)
-- reads committed outbox rows and publishes to Kafka, then marks them sent.
```

> ⚖️ **TRADE-OFF — why not just `save()` then `kafkaTemplate.send()`?**
> Because that's a **dual write**. If the DB commits and the process dies before the send, the order exists but nobody downstream knows — silent, permanent inconsistency. If the send succeeds and the DB rolls back, you've published a phantom order. The outbox makes the event part of the same ACID transaction as the state change; the relay then gives **at-least-once** delivery. Cost: an extra table, a relay to operate, and every consumer must be **idempotent** — which they must be anyway.

**Idempotency at the API edge:**

```sql
CREATE TABLE idempotency_keys (
  key            VARCHAR(64) PRIMARY KEY,
  user_id        BIGINT      NOT NULL,
  request_hash   CHAR(64)    NOT NULL,   -- reject same key + different body
  response_body  JSONB,
  state          VARCHAR(16) NOT NULL,   -- IN_PROGRESS | COMPLETED
  created_at     TIMESTAMPTZ NOT NULL,
  expires_at     TIMESTAMPTZ NOT NULL
);
```
First request inserts `IN_PROGRESS` (a unique-constraint violation on insert = a concurrent duplicate → return `409` or wait). On completion, store the response. A retry with the same key replays the stored response byte-for-byte, without re-executing. **This is what stands between you and double-charging a customer who tapped "Pay" twice on a train.**

> ⚖️ **TRADE-OFF — Orchestration vs. Choreography.**
> **Choreography** (each service listens for the previous service's event): no central component, maximum decoupling, services stay ignorant of each other. But there is no single place that knows the state of an order — debugging "why is order 12345 stuck?" means correlating logs across five services, and compensation logic gets smeared everywhere. It becomes unmaintainable past ~4 steps.
> **Orchestration** (a central saga coordinator): the state machine is explicit, persisted and queryable; timeouts, retries and compensations live in one place; you can build an ops dashboard over it. Cost: the orchestrator is a component that must itself be HA, and it's a coupling point.
> **Chosen: orchestration for checkout** (7 steps, money involved, needs auditability) **and choreography for the downstream fan-out** — notifications, analytics, loyalty points, search-index updates all just subscribe to `order.events` and the order service never learns they exist.

---

### Stage 7 — Payment

```
Browser ──(card details, NEVER touch your servers)──► PSP SDK / hosted iframe
   │                                                       │
   │◄──────────────── payment_method token ────────────────│
   ▼
Payment Service ──► PSP: authorize(intentId, token, idempotencyKey)
   │                    │
   │                    ├─ 3-D Secure challenge? ──► redirect user, async callback
   │                    │
   │◄── webhook: payment.authorized / payment.failed (SIGNED) ──
   ▼
 Ledger (double-entry, append-only):
   +──────────────┬──────────────┬─────────┬────────────┬───────────+
   | txn_id       | account      | debit   | credit     | order_id  |
   +──────────────┼──────────────┼─────────┼────────────┼───────────+
   | t1           | customer_ar  | 1299.00 |            | o-9001    |
   | t1           | revenue      |         | 1299.00    | o-9001    |
   +──────────────┴──────────────┴─────────┴────────────┴───────────+
```

**Non-negotiables:**

- **PCI scope minimisation.** Card data goes browser → PSP directly (hosted fields / SDK). Your servers only ever see an opaque token. This drops you from PCI-DSS Level 1 SAQ-D to SAQ-A and removes an entire class of breach.
- **Idempotency key on the PSP call**, derived from `orderId` — so a network timeout followed by a retry cannot double-charge. **A timeout is not a failure.** The single most dangerous bug in payments is treating a read timeout as "it didn't happen" and retrying without a key.
- **Webhooks are the source of truth, not the HTTP response.** The synchronous response can be lost; the webhook is retried by the PSP for hours. Webhooks must be: signature-verified (HMAC), idempotent (dedupe on the PSP event id), and **order-independent** — `payment.captured` can legitimately arrive before `payment.authorized`.
- **Auth then capture, not purchase.** Authorize at checkout (7-day hold), capture at *shipment*. In many jurisdictions you may not charge for goods you haven't shipped; it also makes cancellation free. Cost: authorizations expire — you need a re-auth path for slow fulfilment.
- **Reconciliation job**: nightly, pull the PSP settlement file and diff it against your ledger. Anything present in one and not the other is an alert. **Assume drift will happen; design the detector, not just the happy path.**
- **The ledger is append-only.** A refund is a new pair of entries, never an `UPDATE`. Financial records are immutable — this is both an audit requirement and what makes the ledger safely replayable.

---

### Stage 8 — Fulfilment & Shipping

```
order.PAID  ──► Fulfilment Service
                   │
                   ├─ 1. WAREHOUSE ALLOCATION  (optimizer)
                   │      inputs : stock per warehouse, distance to pincode,
                   │               shipping cost, SLA promise, warehouse capacity
                   │      output : split the order into 1..N shipments
                   │      ⚠ a 3-item order can legitimately become 3 shipments
                   │        from 3 warehouses — the data model must allow
                   │        Order 1───* Shipment 1───* ShipmentItem
                   │
                   ├─ 2. PICKLIST → WMS  (pick, pack, weigh, label)
                   ├─ 3. CARRIER ALLOCATION (rate shopping: cost vs SLA vs
                   │      carrier serviceability for that pincode)
                   ├─ 4. AWB / tracking number generated → shipment.created
                   └─ 5. Manifest handover → carrier scan
                             │
                             ▼
                    ┌─────────────────────────────┐
                    │  Carrier webhooks / polling │
                    │  PICKED_UP → IN_TRANSIT →   │
                    │  OUT_FOR_DELIVERY →         │
                    │  DELIVERED | FAILED_ATTEMPT │
                    └──────────────┬──────────────┘
                                   ▼
                         shipment.events → Kafka
                                   ├──► Order Service (update order state)
                                   ├──► Notification (SMS/push per transition)
                                   └──► Delivery-promise model (feeds back into
                                        PDP "delivery by" estimates)
```

**Deep-dive points:**

- **The carrier integration layer is an anti-corruption layer.** Every carrier has a different API, different status vocabulary, different auth, and different reliability. Normalize to *your* canonical status enum at the boundary; never leak `FedEx.status == "DL"` into the order state machine.
- **Poll *and* webhook.** Carrier webhooks get lost. Run a reconciling poller for shipments with no update in N hours.
- **Idempotent status ingestion with monotonic state.** Carriers resend and reorder events. Attach a sequence/timestamp and **never move a shipment backwards** — an `IN_TRANSIT` event arriving after `DELIVERED` must be dropped, not applied.
- **Capture payment on shipment**, and that's also when the inventory commit becomes truly final.
- **COD (cash on delivery)** inverts the flow: no auth at checkout, payment recorded at delivery, and a much higher fraud/RTO (return-to-origin) risk — so COD orders go through a stricter risk check and often an OTP-confirm step.

---

### Stage 9 — Delivery & post-delivery

```
DELIVERED event
   ├─► Order → state = DELIVERED, delivered_at set
   ├─► Payment → capture (if not already) / mark COD collected
   ├─► Inventory → reservation record archived; ledger closed
   ├─► Notification → "delivered" push + review request (delayed 24h)
   ├─► Loyalty → award points
   ├─► Returns window opens (T+7/T+30) — a scheduled state transition
   └─► Analytics → delivery SLA actual vs promised → feeds the promise model
```

**Failed delivery** is a first-class path, not an edge case: `FAILED_ATTEMPT` → retry (up to 3) → RTO → refund saga → inventory restocked *after physical inspection*, not on the RTO event. Say this — it shows you've thought past the happy path.

---

### Full sequence, one picture

```
User    GW     Identity  Cart   Checkout  Inventory  Payment   Order   Fulfil  Carrier
 │      │         │       │        │          │         │        │       │        │
 │─login─►───────►│       │        │          │         │        │       │        │
 │◄──JWT──────────│       │        │          │         │        │       │        │
 │─add to cart──►─────────►│       │          │         │        │       │        │
 │               │        │(Redis) │          │         │        │       │        │
 │─checkout────►──────────┼───────►│          │         │        │       │        │
 │               │        │        │─reserve─►│         │        │       │        │
 │               │        │        │◄─held,15m│         │        │       │        │
 │               │        │        │─intent──────────► │        │       │        │
 │◄─clientSecret──────────┼────────│          │         │        │       │        │
 │─pay (PSP SDK)─────────────────────────────────────► │        │       │        │
 │─POST /orders─►─────────┼───────►│          │         │        │       │        │
 │◄─202 orderId───────────┼────────│─────────────────────────►  │       │        │
 │               │        │        │          │         │  SAGA  │       │        │
 │               │        │        │          │◄─commit─────────│       │        │
 │               │        │        │          │         │◄capture│       │        │
 │               │        │        │          │         │        │──────►│        │
 │◄─push "confirmed"──────┼────────┼──────────┼─────────┼────────│       │─ship──►│
 │               │        │        │          │         │        │       │◄events─│
 │◄─push "out for delivery"────────┼──────────┼─────────┼────────┼───────│        │
 │◄─push "delivered"──────┼────────┼──────────┼─────────┼────────┼───────│        │
```

---

## 5. Deep dives (have all of these loaded; the interviewer will pick 2–3)

### DD-1. Flash sale / hot key — 1 M users, 1 000 units, one SKU

This is the hardest scaling problem in e-commerce, because it violates every assumption sharding relies on: **all the traffic goes to exactly one key.**

```
Layer 1  EDGE / ADMISSION CONTROL
         ├─ Virtual waiting room: users get a queue token, admitted at a controlled
         │  rate (say 5 000/s). Everyone else sees a position + ETA page served
         │  entirely from CDN. Protects everything downstream.
         └─ Rate limit per user / device / IP at the gateway (bot defence)

Layer 2  PRE-COMPUTE
         ├─ PDP fully rendered and pushed to CDN before the sale starts
         └─ Pre-warm every cache; never let the sale start with a cold cache

Layer 3  INVENTORY SHARDING (the key technique)
         Split 1 000 units into 10 logical buckets of 100:
             inv:{sku}:b0 … inv:{sku}:b9
         Each request hashes to a bucket → 10× the throughput on one SKU.
         On bucket exhaustion, fall through to a neighbour (steal), and when
         all are empty, flip a Redis flag `soldout:{sku}` that the edge reads
         so 99% of traffic is rejected at the CDN, never reaching the app.

Layer 4  ASYNC ORDER INTAKE
         Accept → push to Kafka (partitioned by sku) → 202 Accepted immediately.
         A bounded pool of consumers processes at the rate the DB can sustain.
         The queue absorbs the spike; the database never sees 1 200 writes/s.
         ⟵ "Queue-based load levelling: I trade user-visible latency for
             backend survival. The user waits 3 seconds instead of the site
             going down for everyone."

Layer 5  DEGRADE, DON'T DIE
         Feature flags to shed load: turn off recommendations, reviews,
         personalization, delivery-estimates. Serve a static PDP.
```

> ⚖️ **TRADE-OFF — bucketed inventory.** You gain ~10× write throughput on the hot key. You lose exactness of the "units remaining" display (you'd have to sum 10 keys) and you can hit a *false* sold-out when one bucket is empty but others aren't — which is why you need bucket-stealing. For 1 000 units and 1 M users, a rare false sold-out is far cheaper than a database meltdown.

### DD-2. Preventing oversell — the layered guarantee

Four defences, each catching what the previous one misses:

1. **Redis atomic Lua decrement** — the fast path, correct under concurrency.
2. **Postgres constraint as the backstop:** `CHECK (available >= 0)` plus `UPDATE inventory SET available = available - :q WHERE sku = :s AND available >= :q` — the conditional `WHERE` makes the *database itself* refuse to oversell even if Redis is wrong. `rowsAffected == 0` means "someone beat you to it".
3. **Reconciliation loop** — continuously diff Redis against the Postgres ledger; alarm on drift, auto-heal small drift, page a human on large drift.
4. **Business-level safety buffer** — never sell the last N% of physical stock online (covers shrinkage, damage, warehouse miscounts). This is the honest admission that software can't fix a warehouse floor.

> ⟵ SAY THIS: *"'Never oversell' is a business requirement, not a database property. The right answer is bounded oversell with automatic detection and a compensation path — because physical stock and the database will drift regardless of what I do in code."*

### DD-3. Sharding & data partitioning

| Data | Shard key | Why | The problem it creates |
|---|---|---|---|
| **Orders** | `user_id` (hash) | 95% of queries are "my orders" → single-shard | "All orders for seller X" becomes a scatter-gather → solve with a **separate seller-side read model** built from `order.events` |
| **Products** | `category` or `seller_id` | Natural locality for merchandising | Hot categories (Electronics) → sub-shard by `sku` hash |
| **Inventory** | `sku` | Contention isolated per SKU | Hot SKU → the bucketing in DD-1 |
| **Cart** | `cartId` (Redis hash slot) | Naturally uniform | Cross-slot ops need hash tags: `cart:{userId}:items` |
| **Payments** | `order_id` | Follows the order | Reporting → replicate to the warehouse |

**Use consistent hashing with virtual nodes**, not `hash % N` — with modulo, adding one shard remaps almost every key. With consistent hashing you remap only `1/N` of keys.

**Composite IDs to avoid a lookup service:** encode the shard into the ID itself, Snowflake-style — `[41 bits timestamp][10 bits shard][12 bits sequence]`. Given an `orderId` you know its shard without a directory lookup, and IDs are roughly time-sortable (good for B-tree insert locality, unlike random UUIDv4 which fragments the index).

### DD-4. Caching strategy (multi-layer)

```
L0  Client / browser        HTTP cache-control, ETag, service worker
L1  CDN edge                static + cacheable JSON (search, PDP)   TTL 60s–1h
L2  Application local       Caffeine, per-pod, for tiny hot sets    TTL 5–30s
     (config, feature flags, seller metadata)                        ← beware pod-to-pod skew
L3  Distributed cache       Redis Cluster — sessions, cart, prices, inventory counters
L4  Database buffer pool    Postgres shared_buffers
```

- **Pattern: cache-aside** (lazy load) for reads; **write-through** for prices (must be immediately correct); **write-behind** only for non-critical counters (view counts), because a crash loses writes.
- **Invalidation** by **event, not by TTL**, wherever correctness matters: `product.changed` on Kafka → every pod evicts its local entry. TTL is the safety net for missed events, never the primary mechanism.
- **Negative caching**: cache "not found" for 30 s, or a bot scanning random SKUs turns every request into a full DB miss.
- **The two hard cases:** *stampede* (see Stage 2) and *hot key* — solve hot keys by replicating the value across N Redis keys (`price:{sku}:r0..r3`) and having the client pick randomly, or by promoting the key into the L2 local cache on all pods.

### DD-5. Resilience patterns (Resilience4j — name them precisely)

| Pattern | Config that matters | What it prevents |
|---|---|---|
| **Circuit breaker** | 50% failure rate over a 100-call sliding window, 30 s open, 5 half-open probes | Hammering a dead dependency; cascading failure |
| **Bulkhead** | Separate thread pool / semaphore per dependency | One slow dependency exhausting the shared pool and taking down *everything* |
| **Timeout** | Always shorter than the caller's timeout, decreasing down the chain | Threads parked forever on a hung socket |
| **Retry** | Max 3, exponential backoff **with jitter**, only on idempotent ops | Retry storms; synchronized retries (thundering herd) |
| **Rate limiter** | Token bucket, per-tenant | A noisy client starving everyone else |
| **Fallback** | Stale cache, default value, or degraded response | A user-visible 500 |

**Two things people get wrong:**
- **Retrying non-idempotent operations.** Retrying `POST /orders` without an idempotency key creates duplicate orders. Retry is only safe with idempotency.
- **Retries without jitter.** Every client backs off by the same 2 s and hits the recovering service simultaneously, knocking it over again. Full jitter: `sleep = random(0, min(cap, base × 2^attempt))`.

### DD-6. Consistency model — per component, with justification

| Component | Model | Justification |
|---|---|---|
| Inventory reserve/commit | **Linearizable** (single-writer per SKU via Redis) | Overselling has direct financial and reputational cost |
| Payment ledger | **Serializable** transactions | Money must balance; double-entry invariants |
| Order state machine | **Strong within shard**, eventual across services | A single order lives in one shard; cross-service via saga |
| Cart | **Read-your-writes** (session affinity to a Redis replica) | User must see their own add-to-cart instantly; nobody else cares |
| Catalog → Search | **Eventual, seconds** | A price change visible 2 s late in search is fine; wrong at checkout is not — hence re-pricing at checkout |
| Recommendations | **Eventual, hours** | Nobody notices |

> ⟵ SAY THIS: *"I don't pick one consistency model for the system. I pick one per data domain and I'm explicit about where the boundary is — because every strong-consistency requirement I accept costs me availability and latency somewhere, so I only pay it where money moves."*

### DD-7. Observability — designed in, not bolted on

- **Distributed tracing (OpenTelemetry)**: one `traceId` generated at the gateway, propagated through every HTTP hop *and through Kafka headers*. Without the Kafka propagation, your traces break exactly where the saga is, which is where you need them.
- **The four golden signals per service**: latency (p50/p95/p99 — **never averages**, averages hide everything), traffic, errors, saturation.
- **Business metrics as SLIs**, not just infra metrics: `orders_placed_per_minute`, `checkout_conversion_rate`, `payment_failure_rate_by_psp`, `reservation_expiry_rate`. A 20% drop in orders/min is a sharper outage signal than any CPU graph, and it catches failures that are technically "200 OK".
- **Correlation:** every log line carries `traceId`, `orderId`, `userId` (hashed). Debugging a stuck saga must be one query, not five.
- **Alert on symptoms, page on customer impact.** "CPU 90%" is a dashboard. "Checkout p99 > 3 s for 5 minutes" is a page.

### DD-8. Multi-region

| Strategy | Read latency | Write complexity | When |
|---|---|---|---|
| **Active-passive** (single write region, async replicas, DNS failover) | Good for reads | Simple | Default. RPO seconds, RTO minutes |
| **Active-active with region-pinned users** | Excellent | Medium — user data lives in their home region | Global scale, distinct user bases |
| **Active-active, globally writable** | Excellent | **Hard** — conflict resolution, CRDTs, or a consensus store | Avoid unless genuinely required |

**Chosen: active-passive for transactional data + active-active for the read path.** Catalog, search and static content are replicated everywhere and served locally; orders/payments/inventory have one authoritative write region per user, with async replication for DR.

> ⟵ SAY THIS: *"Inventory is the reason I don't go globally-writable. A single SKU's stock count is a single logical counter — replicating it across regions means either consensus latency on every decrement, or partitioning the stock per region and accepting that one region sells out while another has units left."*

### DD-9. Search relevance & the read model

- **Query pipeline:** normalize → spell-correct → synonym expand → tokenize → BM25 candidate retrieval → business re-rank (availability, seller rating, margin, personalization) → facet aggregation.
- **Availability as a ranking signal, not a filter** — out-of-stock items rank lower but still appear (they convert to notify-me and preserve SEO), rather than vanishing.
- **Index freshness:** ES `refresh_interval` of 1 s costs write throughput; 30 s is usually right for catalog. For price changes that must be visible immediately, don't fix the index — hydrate price at read time from Redis.
- **Zero-result queries are a product metric.** Log them, they're your merchandising backlog.

### DD-10. Notification service (the quiet correctness trap)

At-least-once Kafka delivery means **duplicate notifications** — the user gets three "order shipped" SMS. Fix: a dedupe key `hash(userId, orderId, eventType)` in Redis with a 24 h TTL, checked before send. Also: per-user rate limiting, quiet hours, channel preference and fallback (push → SMS if push not delivered in 5 min), and template versioning so a bad template rollout doesn't corrupt the queue.

---

## 6. Java / Spring implementation details (they *will* ask, it's a Java interview)

### 6.1 Parallel PDP aggregation without blocking

```java
@GetMapping("/v1/products/{sku}")
public ProductPage getProduct(@PathVariable String sku, @RequestParam String pincode) {
    var catalog   = supplyAsync(() -> catalogClient.get(sku), ioPool);
    var price     = supplyAsync(() -> pricingClient.price(sku), ioPool)
                        .exceptionally(e -> Price.unavailable());          // degrade
    var stock     = supplyAsync(() -> inventoryClient.availability(sku), ioPool)
                        .exceptionally(e -> Availability.UNKNOWN);
    var reviews   = supplyAsync(() -> reviewClient.summary(sku), ioPool)
                        .exceptionally(e -> ReviewSummary.empty());        // non-essential
    var delivery  = supplyAsync(() -> deliveryClient.estimate(sku, pincode), ioPool)
                        .exceptionally(e -> DeliveryEstimate.atCheckout());

    // latency = max(calls), not sum(calls)
    CompletableFuture.allOf(catalog, price, stock, reviews, delivery)
                     .orTimeout(200, MILLISECONDS).join();

    return ProductPage.assemble(catalog.join(), price.join(), stock.join(),
                                reviews.join(), delivery.join());
}
```
Two talking points: **`exceptionally` per call, not one try/catch around the whole thing** — that's what makes degradation *partial* rather than total. And on Java 21, `ioPool` is `Executors.newVirtualThreadPerTaskExecutor()`, which removes the "how many threads do I size the pool to" problem entirely for I/O-bound fan-out.

### 6.2 Transactional outbox

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orders;
    private final OutboxRepository outbox;
    private final ObjectMapper mapper;

    @Transactional                                  // ONE local ACID transaction
    public Order placeOrder(PlaceOrderCommand cmd) {
        Order order = Order.create(cmd);            // state = CREATED
        orders.save(order);

        outbox.save(OutboxEvent.builder()
                .aggregateId(order.getId())
                .aggregateType("ORDER")
                .topic("order.events")
                .eventType("OrderCreated")
                .payload(mapper.writeValueAsString(OrderCreated.from(order)))
                .createdAt(Instant.now())
                .build());

        return order;                               // NO kafkaTemplate.send() here.
    }                                               // Debezium tails the outbox table.
}
```
> ⟵ SAY THIS: *"The moment you see `repository.save()` and `kafka.send()` in the same method, you have a dual-write bug. There is no ordering of those two statements that is crash-safe."*

### 6.3 Idempotent Kafka consumer

```java
@KafkaListener(topics = "order.events", groupId = "inventory-svc",
               concurrency = "6")                       // ≤ partition count
public void onOrderEvent(ConsumerRecord<String, String> rec, Acknowledgment ack) {
    String eventId = new String(rec.headers().lastHeader("eventId").value());

    // dedupe: unique index on processed_events(event_id) does the real work
    if (!processedEvents.tryInsert(eventId)) {
        ack.acknowledge();                              // already handled — drop
        return;
    }
    try {
        inventoryService.handle(parse(rec.value()));
        ack.acknowledge();                              // manual ack AFTER success
    } catch (RetryableException e) {
        throw e;                                        // → retry topic w/ backoff
    } catch (PoisonMessageException e) {
        dlqProducer.send(rec);                          // → DLQ, don't block partition
        ack.acknowledge();
    }
}
```
Key points: **manual ack after successful processing** (auto-commit loses messages on crash); **key by `orderId`** so all events for one order land on one partition and stay ordered; **DLQ for poison messages** so one bad record can't halt a partition forever; `concurrency` never exceeds partition count or you're paying for idle consumers.

### 6.4 Optimistic locking (the JPA version of "don't oversell")

```java
@Entity
public class InventoryItem {
    @Id private String sku;
    private int available;
    @Version private long version;                 // JPA optimistic lock

    public void reserve(int qty) {
        if (available < qty) throw new InsufficientStockException(sku);
        this.available -= qty;
    }
}

@Retryable(retryFor = OptimisticLockingFailureException.class,
           maxAttempts = 3, backoff = @Backoff(delay = 50, multiplier = 2, random = true))
@Transactional
public void reserve(String sku, int qty) { … }
```
And the SQL escape hatch when contention is high enough that JPA retries thrash — a single conditional statement, no read-modify-write, no lock held:
```sql
UPDATE inventory SET available = available - :qty, version = version + 1
 WHERE sku = :sku AND available >= :qty;      -- rowsAffected == 0 → out of stock
```

### 6.5 Connection pool sizing — the classic Java follow-up

`pool_size ≈ ((core_count × 2) + effective_spindle_count)`. For a typical 4-core pod that's **~10 connections, not 100**. Bigger pools are slower: more context switching, more lock contention in the DB, and — critically — `pods × pool_size` must stay under Postgres `max_connections`. 50 pods × 50 connections = 2 500 connections and a dead database. Put **PgBouncer** in transaction-pooling mode in front of it, and set `connectionTimeout` low (2 s) so pool exhaustion fails fast instead of parking every request thread.

---

## 7. Failure modes — "what happens when X dies?"

| Failure | Blast radius | Mitigation | Recovery |
|---|---|---|---|
| **Redis (cart) cluster lost** | Carts empty | Async Postgres snapshot | Rehydrate from snapshot; users lose recent items, not orders |
| **Redis (inventory) lost** | Cannot reserve → checkout stops | Postgres is the SoR | Rebuild counters from `inventory_ledger`; checkout in degraded DB-only mode meanwhile |
| **Kafka partition unavailable** | Saga steps stall | `min.insync.replicas=2`, `acks=all`; outbox rows persist | Relay drains the backlog on recovery — **nothing is lost, only delayed** |
| **PSP down** | No new payments | Multi-PSP with automatic failover routing | Failover; queue COD/retry-later; auth-only orders remain valid |
| **Order DB shard down** | 1/N of users can't order | Multi-AZ sync replica, automatic failover | Promote replica (~30 s); other shards unaffected — **this is the reason to shard** |
| **Search/ES down** | No search | Fall back to category browse from Postgres + CDN-cached popular queries | Site stays usable, revenue degrades ~30% rather than 100% |
| **Saga orchestrator crashes mid-order** | Orders stuck `IN_PROGRESS` | State persisted per step; a recovery scanner resumes any saga with no progress in N minutes | Resume from the last committed step — this is why each step is persisted before it's executed |
| **Duplicate webhook from PSP** | Double capture | Dedupe on PSP event id + idempotency key | No-op |
| **Clock skew across nodes** | Wrong reservation expiry | Never trust local clocks for TTL ordering; use the DB's `now()` or a logical clock | — |

---

## 8. Trade-off summary (the closing slide — say these out loud)

| # | Decision | Chosen | Rejected | Cost accepted |
|---|---|---|---|---|
| 1 | Order transaction | **Saga + outbox** | 2PC/XA | Eventual consistency, compensation logic, must design for partial failure |
| 2 | Saga style | **Orchestration** (checkout) | Choreography | A central component to keep HA |
| 3 | Inventory hot path | **Redis Lua + Postgres SoR** | `SELECT FOR UPDATE` | Redis on the correctness path; needs reconciliation |
| 4 | Reservation timing | **At checkout** | At add-to-cart | Late "out of stock" surprises for the user |
| 5 | Auth | **Short JWT + opaque refresh** | Pure session / pure JWT | ≤15 min revocation window |
| 6 | Search index | **CDC (Debezium)** | Dual-write | Seconds of lag; Kafka Connect to operate |
| 7 | Cart store | **Redis primary + async snapshot** | Postgres primary | Can lose ~seconds of cart mutations |
| 8 | Order API | **202 async** | 201 sync | Client must poll/subscribe; more complex UX |
| 9 | Payment | **Auth then capture-on-ship** | Immediate purchase | Auth expiry handling, re-auth path |
| 10 | Multi-region | **Active-passive writes** | Active-active writes | Higher write latency for far users; RPO of seconds |
| 11 | Flash sale stock | **Bucketed counters** | Single counter | Inexact "left in stock"; possible false sold-out |
| 12 | Microservices at all | **10 services** | Modular monolith | Operational complexity — *justified only by independent scaling of catalog vs. checkout* |

> ⟵ CLOSING LINE: *"If I were starting this company tomorrow with 10 engineers, I'd build a modular monolith with these same module boundaries and one Postgres, and I'd extract Search, Inventory and Payment first — in that order — because those are the three that scale on different axes from everything else. The architecture I've drawn is what that becomes at 10 million DAU, not what you should start with."*

That last line is worth more than any box on the diagram: it shows you understand that architecture is a function of scale and organization, not a fixed ideal.

---

## 9. Rapid-fire drill answers

**"How do you handle a user adding the last item to their cart while someone else checks out?"**
Add-to-cart doesn't reserve. Both succeed. Whoever hits `POST /checkout/sessions` first gets the atomic reservation; the second gets a 409 with the failing SKU. The UI shows "no longer available" and offers alternatives.

**"Two orders for the last unit arrive in the same millisecond."**
Both hit the same Redis key. Redis is single-threaded and the Lua script is atomic, so they serialize: one returns 1, the other returns -1. There is no race — that's precisely why the check-and-decrement must be one atomic operation and not a `GET` followed by a `SET`.

**"The payment succeeded but the order service crashed before recording it."**
The PSP webhook is retried for hours. On recovery, the webhook handler finds a payment with no completed order, and the reconciler either completes the saga forward (preferred — the customer paid, so give them the goods) or refunds. This is why payment state lives in the payment service's own ledger and not only inside the order row.

**"How do you migrate the order table schema with zero downtime?"**
Expand-contract: (1) add the nullable column, deploy; (2) dual-write old+new, deploy; (3) backfill in batches; (4) switch reads, deploy; (5) stop writing the old column; (6) drop it. Each step is independently reversible — never combine two.

**"Why not just use one big Postgres?"**
For 1 M orders/day, honestly, a well-tuned Postgres with read replicas *can* do it, and I'd say so. It breaks on: catalog search (wrong index type), inventory contention on hot SKUs (lock convoys), and the coupling of a deploy that risks the checkout path with one that risks the browse path. Those three are the actual reasons to split — not "microservices are best practice".

**"Where's your single point of failure?"**
The API gateway (mitigated: stateless, N replicas across AZs behind an ALB), Kafka (3 brokers, RF=3, `min.insync.replicas=2`), and the saga orchestrator (stateless workers, state in Postgres, any worker can resume any saga). The genuinely irreducible one is the write-primary of each Postgres shard — mitigated with synchronous standby and automatic failover, RTO ~30 s.

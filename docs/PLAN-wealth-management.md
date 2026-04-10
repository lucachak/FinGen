# PLAN: Wealth Management Module

Implement a decoupled **Wealth Management** module to track net worth across various asset types, integrated with external pricing APIs and a rule-based suggestion engine. Also, rename "Oficina" references to "Business".

## 🛠️ Architecture & Design

### Bounded Context: `lucas.basemodel.wealth`
- **Isolation:** No direct imports from `financeiro` or `gestor`. Uses `User` as a shared kernel.
- **Java 21:** Records for DTOs, Sealed Classes for Asset types, Virtual Threads for API calls.

### Data Model (PostgreSQL)

- **`wealth_assets`**: Base table (ID, User, Name, Type, CreatedAt).
- **`wealth_vehicles`**: `make`, `model`, `year`, `mileage`, `location`.
- **`wealth_real_estate`**: `type`, `size_m2`, `location`, `acquisition_price`.
- **`wealth_stocks`**: `ticker`, `quantity`, `avg_price`, `class`.
- **`wealth_valuations`**: Historical price logs (AssetID, Value, Source, Timestamp).
- **`wealth_snapshots`**: Aggregated net worth over time.
- **`wealth_suggestions`**: Rule-based advice.

---

## 📅 Implementation Roadmap

### Phase 1: Foundation & Schema (database-architect)
- [ ] Create Flyway migrations for all `wealth_*` tables.
- [ ] Define Entity hierarchy (JPA Inheritance: `JOINED`).
- [ ] Implement `User` relationship (Many-to-One).

### Phase 2: Core Asset Management (backend-specialist)
- [ ] Implement CRUD for each Asset type (Vehicles, Real Estate, Stocks, etc.).
- [ ] Build `AssetPricingService` with stubs for FIPE/B3/Brapi.
- [ ] Implement `@Scheduled` background task for valuation updates.

### Phase 3: Net Worth Intelligence (backend-specialist)
- [ ] Develop `NetWorthCalculator` to aggregate real-time valuation + currency conversion.
- [ ] Implement `HistoricalSnapshotService` for time-series data.
- [ ] Build the **Rule-Based Suggestion Engine** (Diversification, Depreciation, Opportunity flags).

### Phase 4: Integration & Rename (frontend-specialist)
- [ ] **Rename "Oficina" to "Business"**:
    - [ ] Update `EscopoTransacao.java`.
    - [ ] Update `home/index.html` (Tabs & Labels).
    - [ ] Update `contas/form.html` (Select options).
- [ ] Implement `GET /api/wealth/summary` for the dashboard.
- [ ] Add "Wealth" section to the main navigation/sidebar.

### Phase 5: Verification (test-engineer)
- [ ] Unit tests for `NetWorthCalculator` logic.
- [ ] Integration tests for Asset CRUD and Summary API.
- [ ] Security audit on cross-user data isolation.

---

## 🚀 Verification Checklist

- [ ] Net Worth matches the sum of last valuations.
- [ ] "Business" tab works and correctly filters transactions.
- [ ] Suggestion engine triggers "High Cash Ratio" when bank balance > 50% of total.
- [ ] External API failures handle `STALE_PRICE` gracefully.

// ============================================================================
// generate-mock-data.mjs — 7-year mock DML dataset for the Investomania tracker
// Target user: ankit (id = 3). ADDITIVE ONLY — existing rows (investments
// <= 107 / transactions <= 110 / expenses <= 8) are preserved.
//
//   node backend/sql/generate-mock-data.mjs
//   → backend/sql/mock-data-7yr.sql
//
// Deterministic (seeded PRNG). Cash-account current_value equals the exact
// CashLedgerService replay of the generated history (oldest DEPOSIT seeds the
// balance; DEPOSIT/TRANSFER_IN/SALARY/INTEREST credit; TRANSFER_OUT/
// WITHDRAWAL/EXPENSE debit). Equity/crypto quantity = net of BUY/SELL lots;
// purchase_price = volume-weighted average of buys.
// ============================================================================

import { writeFileSync, mkdirSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = dirname(fileURLToPath(import.meta.url));
const OUT = join(__dirname, "mock-data-7yr.sql");

const USER_ID = 3;
const AXIS = 88; // existing "Axis Salary ACC" (CASH, INR)
const SBI = 87; // existing "SBI Emergency ACC" (CASH, INR)

// ---------------------------------------------------------------------------
// Seeded PRNG + helpers
// ---------------------------------------------------------------------------
function mulberry32(seed) {
  let a = seed >>> 0;
  return function () {
    a |= 0; a = (a + 0x6d2b79f5) | 0;
    let t = Math.imul(a ^ (a >>> 15), 1 | a);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}
const rnd = mulberry32(20261004);
const jitter = (base, pct) => Math.round(base * (1 + (rnd() * 2 - 1) * pct));
const money = (n) => (Math.round(n * 100) / 100).toFixed(2);
const q4 = (n) => Number(n).toFixed(4);
const q8 = (n) => Number(n).toFixed(8);
const sq = (s) => String(s).replace(/'/g, "''");
const day = (ymStr, d) => `${ymStr}-${String(d).padStart(2, "0")}`;

const MONTHS = [...Array(12).keys()].map((i) => i + 1);
const ym = (y, m) => `${y}-${String(m).padStart(2, "0")}`;
const MONTH_SEQ = []; // 2019-10 … 2026-09 (84 months)
for (let y = 2019; y <= 2026; y++)
  for (const m of MONTHS)
    if (ym(y, m) >= "2019-10" && ym(y, m) <= "2026-09") MONTH_SEQ.push([y, m]);

const investments = [];
const transactions = [];
const expenses = [];
let invId = 200, txnId = 1000, expId = 3000;

function addInv(type, cols) { const id = invId++; investments.push({ id, type, cols }); return id; }
function addTxn(investmentId, type, { qty = null, price = null, amount, date, currency = "INR", notes = "", expenseId = null }) {
  transactions.push({ id: txnId++, investmentId, type, qty, price, amount, date, currency, notes, expenseId });
}
function addExpense(o) { const id = expId++; expenses.push({ id, paidFrom: null, ...o }); return id; }

// ---------------------------------------------------------------------------
// Cash ledger bookkeeping (mirrors CashLedgerService.replayBalance exactly)
// ---------------------------------------------------------------------------
const ledger = { [AXIS]: { balance: 0, opened: false }, [SBI]: { balance: 0, opened: false } };
function cashFlow(accountId, type, amount, date, notes) {
  const st = ledger[accountId];
  if (!st.opened && (type === "DEPOSIT" || type === "BUY")) { st.balance = amount; st.opened = true; }
  else {
    if (["DEPOSIT", "TRANSFER_IN", "SALARY", "INTEREST"].includes(type)) st.balance += amount;
    else if (["TRANSFER_OUT", "WITHDRAWAL", "EXPENSE"].includes(type)) st.balance -= amount;
    else return; // not a cash-flow type — don't post
    st.opened = true;
  }
  if (st.balance < -0.005) throw new Error(`Cash ${accountId} negative on ${date}: ${st.balance.toFixed(2)}`);
  addTxn(accountId, type, { amount, date, notes });
}
// Expense posting with the E10 expense_id backlink (mirrors CashLedgerService.postExpenseTransaction)
function postExpense(accountId, expenseId, amount, date, desc) {
  const st = ledger[accountId];
  st.balance -= amount; st.opened = true;
  if (st.balance < -0.005) throw new Error(`Cash ${accountId} negative on ${date}: ${st.balance.toFixed(2)}`);
  addTxn(accountId, "EXPENSE", { amount, date, notes: `Expense: ${desc}`, expenseId });
}

// ---------------------------------------------------------------------------
// 1) EQUITY — India listed (NSE)
// ---------------------------------------------------------------------------
const rel = addInv("EQUITY", { symbol: "RELIANCE", market: "NSE", name: "Reliance Industries", isListed: true });
addTxn(rel, "BUY", { qty: 60, price: 1180, amount: 70800, date: "2019-11-12", notes: "Initial lot — discount broker" });
addTxn(rel, "BUY", { qty: 40, price: 2200, amount: 88000, date: "2021-01-19", notes: "Added on retail momentum" });
for (const [d, a] of [["2020-07-14", 720], ["2021-07-15", 1200], ["2022-07-14", 1300], ["2023-07-17", 1350], ["2024-07-15", 1400], ["2025-07-14", 1450], ["2026-07-13", 1500]])
  addTxn(rel, "DIVIDEND", { amount: a, date: d, notes: "Annual dividend credit" });

const tcs = addInv("EQUITY", { symbol: "TCS", market: "NSE", name: "Tata Consultancy Services", isListed: true });
addTxn(tcs, "BUY", { qty: 30, price: 2150, amount: 64500, date: "2020-02-10", notes: "IT heavyweight starter lot" });
for (const [d, a] of [["2021-06-04", 1140], ["2022-06-03", 2250], ["2023-06-05", 2400], ["2024-06-04", 2550], ["2025-06-04", 2700]])
  addTxn(tcs, "DIVIDEND", { amount: a, date: d, notes: "Annual dividend credit" });

const tamo = addInv("EQUITY", { symbol: "TATAMOTORS", market: "NSE", name: "Tata Motors", isListed: true });
addTxn(tamo, "BUY", { qty: 200, price: 420, amount: 84000, date: "2019-12-06", notes: "JLR recovery bet" });
addTxn(tamo, "BUY", { qty: 100, price: 350, amount: 35000, date: "2020-03-20", notes: "COVID crash averaging down" });
addTxn(tamo, "SELL", { qty: 150, price: 290, amount: 43500, date: "2020-09-11", notes: "Booked partial loss — thesis broken" });
addTxn(tamo, "SELL", { qty: 150, price: 310, amount: 46500, date: "2021-02-24", notes: "Exited remaining at a loss" });

const yesb = addInv("EQUITY", { symbol: "YESBANK", market: "NSE", name: "Yes Bank", isListed: true });
addTxn(yesb, "BUY", { qty: 1000, price: 58, amount: 58000, date: "2019-11-04", notes: "Cheap-looking private bank" });
addTxn(yesb, "BUY", { qty: 2000, price: 26, amount: 52000, date: "2020-03-06", notes: "Averaged down before moratorium" });
addTxn(yesb, "SELL", { qty: 3000, price: 14, amount: 42000, date: "2020-06-18", notes: "Restructuring — exited at heavy loss" });

const hdfcb = addInv("EQUITY", { symbol: "HDFCBANK", market: "NSE", name: "HDFC Bank", isListed: true });
addTxn(hdfcb, "BUY", { qty: 40, price: 1220, amount: 48800, date: "2021-06-15", notes: "Core compounder allocation" });
for (const [d, a] of [["2022-07-08", 640], ["2023-07-10", 680], ["2024-07-09", 720], ["2025-07-09", 760], ["2026-07-10", 800]])
  addTxn(hdfcb, "DIVIDEND", { amount: a, date: d, notes: "Annual dividend credit" });

const gold = addInv("EQUITY", { symbol: "GOLDBEES", market: "NSE", name: "Nippon India Gold BeES ETF", isListed: true });
addTxn(gold, "BUY", { qty: 1500, price: 44, amount: 66000, date: "2020-07-22", notes: "Gold allocation via listed ETF" });
addTxn(gold, "BUY", { qty: 2000, price: 51, amount: 102000, date: "2021-05-11", notes: "Added as inflation hedge" });
addTxn(gold, "BUY", { qty: 1000, price: 58, amount: 58000, date: "2023-08-16", notes: "Topped up" });

// ---------------------------------------------------------------------------
// 2) EQUITY — India unlisted (pre-IPO counters)
// ---------------------------------------------------------------------------
const zer = addInv("EQUITY", { symbol: "ZERODHA-UNL", market: "UNLISTED", name: "Zerodha (Unlisted)", isListed: false });
addTxn(zer, "BUY", { qty: 120, price: 950, amount: 114000, date: "2021-08-27", notes: "Unlisted platform buy — ESOP secondary" });

const nse = addInv("EQUITY", { symbol: "NSE-UNL", market: "UNLISTED", name: "National Stock Exchange (Unlisted)", isListed: false });
addTxn(nse, "BUY", { qty: 25, price: 1450, amount: 36250, date: "2020-10-13", notes: "Exchange monopoly — pre-IPO" });

const oyo = addInv("EQUITY", { symbol: "OYO-UNL", market: "UNLISTED", name: "OYO Rooms (Unlisted)", isListed: false });
addTxn(oyo, "BUY", { qty: 500, price: 42, amount: 21000, date: "2022-02-08", notes: "Travel-recovery punt" });
addTxn(oyo, "SELL", { qty: 500, price: 28, amount: 14000, date: "2023-11-21", notes: "Exited at a loss after repeated IPO deferrals" });

// ---------------------------------------------------------------------------
// 3) EQUITY — US listed
// ---------------------------------------------------------------------------
const aapl = addInv("EQUITY", { symbol: "AAPL", market: "NASDAQ", name: "Apple Inc.", isListed: true, currency: "USD" });
addTxn(aapl, "BUY", { qty: 10, price: 175, amount: 1750, date: "2021-03-09", currency: "USD", notes: "Core US tech" });
addTxn(aapl, "SELL", { qty: 4, price: 190, amount: 760, date: "2022-06-14", currency: "USD", notes: "Trimmed after run-up" });
for (const [d, a] of [["2022-05-13", 8.80], ["2023-05-15", 5.28], ["2024-05-14", 5.88], ["2025-05-15", 5.88], ["2026-05-15", 5.88]])
  addTxn(aapl, "DIVIDEND", { amount: a, date: d, currency: "USD", notes: "Dividends accumulated" });

const pton = addInv("EQUITY", { symbol: "PTON", market: "NASDAQ", name: "Peloton Interactive", isListed: true, currency: "USD" });
addTxn(pton, "BUY", { qty: 30, price: 28, amount: 840, date: "2021-05-17", currency: "USD", notes: "Pandemic-favourite fad" });
addTxn(pton, "SELL", { qty: 30, price: 8.5, amount: 255, date: "2022-09-13", currency: "USD", notes: "Exited at a heavy loss" });

const goog = addInv("EQUITY", { symbol: "GOOGL", market: "NASDAQ", name: "Alphabet Inc.", isListed: true, currency: "USD" });
addTxn(goog, "BUY", { qty: 8, price: 120, amount: 960, date: "2023-06-20", currency: "USD", notes: "Post-split entry" });

// ---------------------------------------------------------------------------
// 4) CRYPTO (USD custody)
// ---------------------------------------------------------------------------
const btc = addInv("CRYPTO", { symbol: "BTC", exchange: "Coinbase", name: "Bitcoin", isListed: true, currency: "USD" });
addTxn(btc, "BUY", { qty: 0.02, price: 38000, amount: 760, date: "2021-02-16", currency: "USD", notes: "First sats" });
addTxn(btc, "BUY", { qty: 0.01, price: 64000, amount: 640, date: "2021-11-09", currency: "USD", notes: "Bought the top, alas" });
addTxn(btc, "SELL", { qty: 0.015, price: 21000, amount: 315, date: "2022-06-15", currency: "USD", notes: "Panic-trimmed in the bear market" });

const eth = addInv("CRYPTO", { symbol: "ETH", exchange: "Coinbase", name: "Ethereum", isListed: true, currency: "USD" });
addTxn(eth, "BUY", { qty: 0.5, price: 2300, amount: 1150, date: "2021-04-07", currency: "USD", notes: "Smart-contract bet" });
addTxn(eth, "BUY", { qty: 1.2, price: 1250, amount: 1500, date: "2022-06-19", currency: "USD", notes: "Accumulated the capitulation" });

const doge = addInv("CRYPTO", { symbol: "DOGE", exchange: "Binance", name: "Dogecoin", isListed: true, currency: "USD" });
addTxn(doge, "BUY", { qty: 10000, price: 0.32, amount: 3200, date: "2021-05-04", currency: "USD", notes: "Momentum FOMO entry" });
addTxn(doge, "SELL", { qty: 10000, price: 0.09, amount: 900, date: "2022-11-15", currency: "USD", notes: "Lesson learned — exited at a loss" });

// ---------------------------------------------------------------------------
// 5) BONDS (coupons hand-written; couponFrequency NULL so BondCouponScheduler
//    never duplicates them)
// ---------------------------------------------------------------------------
const nhai = addInv("BOND", { issuer: "NHAI", name: "NHAI Tax-Free Bond", faceValue: 1000, couponRate: 7.35, maturity: "2031-01-15", rating: "AAA" });
addTxn(nhai, "BUY", { qty: 50, price: 1020, amount: 51000, date: "2019-11-20", notes: "Tax-free infra bond" });
for (const d of ["2021-01-15", "2022-01-17", "2023-01-16", "2024-01-15", "2025-01-15", "2026-01-15"])
  addTxn(nhai, "INTEREST", { amount: 3675, date: d, notes: "Annual coupon — NHAI bond" });

const tataNcd = addInv("BOND", { issuer: "Tata Capital", name: "Tata Capital NCD 9.25%", faceValue: 1000, couponRate: 9.25, maturity: "2027-03-16", rating: "AAA" });
addTxn(tataNcd, "BUY", { qty: 20, price: 1005, amount: 20100, date: "2023-03-16", notes: "Public-issue allotment" });
for (const d of ["2024-03-16", "2025-03-17", "2026-03-16"])
  addTxn(tataNcd, "INTEREST", { amount: 1850, date: d, notes: "Annual coupon — Tata Capital NCD" });

const sgb = addInv("BOND", { issuer: "RBI", name: "Sovereign Gold Bond 2032 Series II", faceValue: 5076, couponRate: 2.5, maturity: "2032-08-15", rating: "SOV" });
addTxn(sgb, "BUY", { qty: 40, price: 5076, amount: 203040, date: "2022-08-29", notes: "40 g gold-linked — tranche subscription" });
for (const d of ["2023-08-29", "2024-08-29", "2025-08-29", "2026-08-28"])
  addTxn(sgb, "INTEREST", { amount: 5076, date: d, notes: "SGB interest (2.5% on issue price)" });

// ---------------------------------------------------------------------------
// 6) FD / RD
// ---------------------------------------------------------------------------
const fd1 = addInv("FD", { bank: "HDFC Bank", name: "HDFC Bank FD 7.10% (36m)", principal: 200000, rate: 7.10, tenure: 36, maturity: "2025-04-01" });
addTxn(fd1, "BUY", { amount: 200000, date: "2022-04-01", notes: "Cumulative FD booking" });
addTxn(fd1, "INTEREST", { amount: 46530, date: "2025-04-01", notes: "Cumulative interest credited at maturity" });
addTxn(fd1, "WITHDRAWAL", { amount: 246530, date: "2025-04-03", notes: "FD matured — proceeds swept to Axis Salary ACC" });

const fd2 = addInv("FD", { bank: "SBI", name: "SBI Tax Saver FD 6.90% (5y)", principal: 150000, rate: 6.90, tenure: 60, maturity: "2028-01-15" });
addTxn(fd2, "BUY", { amount: 150000, date: "2023-01-16", notes: "80C tax-saver booking (locked 5y)" });

const rd1 = addInv("RD", { bank: "HDFC Bank", name: "HDFC RD 6.80% (60m)", monthly: 5000, rate: 6.80, tenure: 60, maturity: "2026-07-01" });
for (const [y, m] of MONTH_SEQ)
  if (ym(y, m) >= "2021-07" && ym(y, m) <= "2026-06")
    addTxn(rd1, "CONTRIBUTION", { amount: 5000, date: day(ym(y, m), 5), notes: "Auto-debit instalment" });
addTxn(rd1, "INTEREST", { amount: 54900, date: "2026-07-01", notes: "Cumulative RD interest at maturity" });
addTxn(rd1, "WITHDRAWAL", { amount: 354900, date: "2026-07-02", notes: "RD matured — proceeds moved to NPS and gold" });

const rd2 = addInv("RD", { bank: "ICICI Bank", name: "ICICI RD 7.00% (36m)", monthly: 7500, rate: 7.00, tenure: 36, maturity: "2027-10-01" });
for (const [y, m] of MONTH_SEQ)
  if (ym(y, m) >= "2024-10" && ym(y, m) <= "2026-09")
    addTxn(rd2, "CONTRIBUTION", { amount: 7500, date: day(ym(y, m), 10), notes: "Auto-debit instalment" });

// ---------------------------------------------------------------------------
// 7) PPF / NPS
// ---------------------------------------------------------------------------
const ppf = addInv("PPF", { account: "PPF-SBI-4471", name: "SBI PPF Account", freq: "YEARLY", contributed: 1050000 });
for (const y of [2020, 2021, 2022, 2023, 2024, 2025, 2026])
  addTxn(ppf, "CONTRIBUTION", { amount: 150000, date: `${y}-04-04`, notes: "Annual PPF contribution (before Apr 5 cutoff)" });

const nps = addInv("NPS", { account: "NPS-44710099", name: "NPS Tier-I (Aggressive)", freq: "QUARTERLY", contributed: 625000 });
for (const [y, m] of MONTH_SEQ)
  if (m % 3 === 1 && ym(y, m) >= "2020-07")
    addTxn(nps, "CONTRIBUTION", { amount: 25000, date: day(ym(y, m), 25), notes: "Quarterly NPS contribution" });

// ---------------------------------------------------------------------------
// 8) REAL ESTATE
// ---------------------------------------------------------------------------
const flat = addInv("REAL_ESTATE", { propertyType: "RESIDENTIAL", location: "Whitefield, Bengaluru, Karnataka", name: "2BHK Flat — Whitefield", price: 6200000 });
addTxn(flat, "BUY", { amount: 6200000, date: "2020-12-15", notes: "Registration and stamp duty included" });

const plot = addInv("REAL_ESTATE", { propertyType: "LAND", location: "Hosur Road, Bengaluru, Karnataka", name: "Residential Plot — Hosur Road", price: 1850000 });
addTxn(plot, "BUY", { amount: 1850000, date: "2021-10-20", notes: "BDA-approved 30x40 site" });

// ---------------------------------------------------------------------------
// 9) EXPENSES — one-offs + 84 monthly baselines with deliberate variation
// ---------------------------------------------------------------------------
// Big one-offs: [date, category, amount, description, notes, target(AXIS|SBI|null)]
const ONE_OFFS = [
  ["2020-03-18", "HEALTHCARE", 42000, "Emergency appendectomy — Manipal Hospital", null, AXIS],
  ["2020-04-03", "HEALTHCARE", 2500, "Teleconsultation and medicines (lockdown)", null, AXIS],
  ["2020-11-15", "EDUCATION", 85000, "Data-science bootcamp fee", null, AXIS],
  ["2021-05-10", "HEALTHCARE", 110000, "Father's cardiac surgery — Apollo", "Paid from emergency fund", SBI],
  ["2022-02-20", "OTHER", 68000, "Sister's wedding gift", null, AXIS],
  ["2022-12-05", "ENTERTAINMENT", 54000, "Kashmir family trip", null, AXIS],
  ["2023-06-01", "EDUCATION", 175000, "Executive programme — IIM Bangalore", null, AXIS],
  ["2023-10-02", "OTHER", 34000, "Laptop repair and accessories", null, AXIS],
  ["2024-01-15", "TRANSPORT", 940000, "Car purchase — on-road price", "Loan financed — EMIs not tracked", null],
  ["2024-11-10", "HOUSING", 210000, "Home renovation — modular kitchen", "12-month EMI — not from linked account", null],
  ["2025-10-20", "ENTERTAINMENT", 145000, "Japan trip — flights and stay", null, AXIS],
  ["2026-03-28", "INSURANCE", 62000, "Annual term and health premium", null, AXIS],
  ["2026-08-22", "EDUCATION", 25000, "Tech conference and workshop", null, AXIS],
];
const INSURANCE_ANNUAL = { "2020": 14000, "2021": 16000, "2022": 18500, "2023": 22000, "2024": 28000, "2025": 41000 };

// Expense ids that should post a cash debit, grouped by target account:
const linked = []; // {id, amount, date, account}
for (const [date, cat, amt, desc, notes, target] of ONE_OFFS) {
  const id = addExpense({ description: desc, category: cat, amount: amt, date, notes, recurring: false });
  if (target) linked.push({ id, amount: amt, date, account: target, fixed: true });
}
for (const [y, amt] of Object.entries(INSURANCE_ANNUAL)) {
  const date = `${y}-09-05`;
  const id = addExpense({ description: "Annual insurance premium", category: "INSURANCE", amount: amt, date, recurring: true, freq: "YEARLY" });
  linked.push({ id, amount: amt, date, account: AXIS, fixed: true });
}

// Monthly baseline per month; COVID months 2020-04/05 deliberately tiny.
const SALARY = { 2019: 95000, 2020: 95000, 2021: 105000, 2022: 122000, 2023: 135000, 2024: 152000, 2025: 168000, 2026: 185000 };
const BONUS = { 2021: 60000, 2022: 75000, 2023: 90000, 2024: 110000, 2025: 125000, 2026: 150000 };
const salaryFlows = []; // {date, amount, bonus}
const monthPlan = new Map(); // ym -> [{id, amount, date, desc}]

for (const [y, m] of MONTH_SEQ) {
  const key = ym(y, m);
  const yr = y - 2019;
  const covid = key === "2020-04" || key === "2020-05";

  const rent = Math.min(18000 + 1000 * yr, 26000);
  const food = covid ? 2300 : jitter(6500 + 220 * yr, 0.12);
  const transport = covid ? 350 : jitter(2200 + 130 * yr, 0.2);
  const utilities = covid ? 1450 : jitter(1800 + 70 * yr, 0.18);
  const entertainment = covid || rnd() > 0.55 ? null : jitter(900 + 180 * yr, 0.35);
  const healthcare = covid ? null : rnd() < 0.3 ? jitter(700 + 120 * yr, 0.5) : m === 1 ? jitter(3200, 0.3) : null;

  const rows = [
    ["Rent — Whitefield 2BHK", "HOUSING", rent, true, 3],
    ["Groceries and dining", "FOOD", food, false, 7 + Math.floor(rnd() * 20)],
    ["Commute and fuel", "TRANSPORT", transport, false, 5 + Math.floor(rnd() * 22)],
    ["Electricity, water, internet", "UTILITIES", utilities, false, 5 + Math.floor(rnd() * 22)],
  ];
  if (entertainment) rows.push(["Streaming, movies, outings", "ENTERTAINMENT", entertainment, false, 5 + Math.floor(rnd() * 22)]);
  if (healthcare) rows.push(["Doctor visits and pharmacy", "HEALTHCARE", healthcare, false, 5 + Math.floor(rnd() * 22)]);

  for (const [desc, cat, amt, rec, d] of rows) {
    const amt2 = rnd() < 0.25 ? Math.round(amt) + Math.floor(rnd() * 90) / 100 : Math.round(amt); // some rows carry paise
    const date = day(key, d);
    const id = addExpense({ description: desc, category: cat, amount: amt2, date, recurring: rec, freq: rec ? "MONTHLY" : null });
    (monthPlan.get(key) ?? monthPlan.set(key, []).get(key)).push({ id, amount: amt2, date, desc });
  }

  salaryFlows.push({ date: day(key, 1), amount: SALARY[y], bonus: false });
  if (m === 4 && BONUS[y]) salaryFlows.push({ date: day(key, 10), amount: BONUS[y], bonus: true });
}

// ---------------------------------------------------------------------------
// 10) CASH-ACCOUNT HISTORY (posted in date order; ledger guards non-negative)
// ---------------------------------------------------------------------------
cashFlow(SBI, "DEPOSIT", 150000, "2019-10-05", "Emergency fund — initial transfer");
cashFlow(AXIS, "DEPOSIT", 120000, "2019-10-01", "Account opening — transferred from old bank");

// SBI hand flows + the surgery EXPENSE (linked to SBI) in date order:
const surgery = expenses.find((e) => e.date === "2021-05-10");
const sbiFlows = [];
for (const d of ["2020-04-06", "2020-10-06", "2021-04-06", "2021-10-06", "2022-04-06", "2022-10-06", "2023-04-06", "2023-10-06", "2024-04-05", "2024-10-07", "2025-04-07", "2025-10-06", "2026-04-06"])
  sbiFlows.push({ date: d, type: "INTEREST", amount: Math.round(340 + rnd() * 700), notes: "Savings interest — emergency fund" });
sbiFlows.push({ date: "2021-08-02", type: "DEPOSIT", amount: 125000, notes: "Refill after bonus" });
sbiFlows.push({ date: "2023-12-15", type: "WITHDRAWAL", amount: 40000, notes: "Family support — one-off" });
sbiFlows.sort((a, b) => a.date.localeCompare(b.date));
for (const f of sbiFlows) {
  if (surgery && f.date > surgery.date && surgery.paidFrom === null) {
    surgery.paidFrom = SBI; // post the surgery debit at its own date, before later credits
    postExpense(SBI, surgery.id, surgery.amount, surgery.date, surgery.description);
  }
  cashFlow(SBI, f.type, f.amount, f.date, f.notes);
}
if (surgery && surgery.paidFrom === null) { surgery.paidFrom = SBI; postExpense(SBI, surgery.id, surgery.amount, surgery.date, surgery.description); }

// Axis: monthly salary/bonus → interest → withdrawal → expense postings, date-ordered
for (const [y, m] of MONTH_SEQ) {
  const key = ym(y, m);
  const flows = [];
  for (const s of salaryFlows) if (s.date.startsWith(key)) flows.push({ date: s.date, type: "SALARY", amount: s.amount, notes: s.bonus ? "Annual performance bonus" : "Monthly salary credit — InfoEdge" });
  if ([3, 6, 9, 12].includes(m)) flows.push({ date: day(key, 28), type: "INTEREST", amount: 300 + 5 * (MONTH_SEQ.indexOf([y, m]) + 1), notes: "Savings account interest" });

  const monthExpenses = (monthPlan.get(key) || []).concat(linked.filter((l) => l.date.startsWith(key)).map((l) => ({ amount: l.amount, date: l.date, id: l.id })));
  const expenseTotal = monthExpenses.reduce((s, e) => s + e.amount, 0);
  const salaryTotal = flows.filter((f) => f.type === "SALARY").reduce((s, f) => s + f.amount, 0);
  const surplus = salaryTotal - expenseTotal;
  if (surplus > 8000) {
    // base sweep + balance-proportional extra so idle cash stays ~1.5–2.5L
    const extra = 0.10 * Math.max(0, ledger[AXIS].balance - 150000);
    const w = Math.min(surplus * 0.55 + extra, ledger[AXIS].balance + salaryTotal - expenseTotal - 20000);
    if (w >= 5000) flows.push({ date: day(key, 5), type: "WITHDRAWAL", amount: Math.round(w / 100) * 100, notes: "Auto-sweep to investment funding" });
  }
  flows.sort((a, b) => a.date.localeCompare(b.date));
  for (const f of flows) cashFlow(AXIS, f.type, f.amount, f.date, f.notes);

  // Expense postings for the month (date order); unlink any the buffer can't take
  for (const e of monthExpenses.sort((a, b) => a.date.localeCompare(b.date))) {
    const exp = expenses.find((x) => x.id === e.id);
    if (exp.paidFrom !== null) continue; // already posted (e.g. SBI surgery)
    if (ledger[AXIS].balance - e.amount >= 10000) {
      exp.paidFrom = AXIS;
      postExpense(AXIS, e.id, e.amount, e.date, e.desc);
    }
    // else: stays unlinked (legacy-style row, no cash movement) — deliberate fallback
  }
}

// Insurance + one-off rows that fall inside months already processed above are
// covered via monthPlan concat; nothing further to post.

// Book the EXISTING (pre-seed) cash flows into the ledger so the final
// current_value updates match the app's full-history replay. These rows are
// already in the DB (txn ids 82/83/109) — bookkeeping only, NOT re-inserted.
ledger[AXIS].balance += 300774.34; // existing DEPOSIT 2026-07-10 (txn 83)
ledger[AXIS].balance -= 7000.00;   // existing EXPENSE 2026-10-02 (txn 109, expense 8)
ledger[SBI].balance += 295598.00;  // existing DEPOSIT 2026-07-10 (txn 82)

// ---------------------------------------------------------------------------
// 11) Final state: stats (net qty / avg price), purchase dates, cash values
// ---------------------------------------------------------------------------
const stats = {};
for (const t of transactions) {
  const inv = investments.find((i) => i.id === t.investmentId);
  if (!inv || !["EQUITY", "CRYPTO"].includes(inv.type)) continue;
  const s = (stats[t.investmentId] ??= { boughtQty: 0, spent: 0, soldQty: 0 });
  if (t.type === "BUY") { s.boughtQty += t.qty; s.spent += t.amount; }
  if (t.type === "SELL") { s.soldQty += t.qty; }
}
for (const inv of investments) {
  const dates = transactions.filter((t) => t.investmentId === inv.id).map((t) => t.date).sort();
  inv.cols.purchaseDate = dates[0];
  inv.cols.createdTs = `${dates[0]} 09:30:00`;
  const s = stats[inv.id];
  if (s) {
    inv.cols.netQty = Math.round((s.boughtQty - s.soldQty) * 1e8) / 1e8;
    inv.cols.avgPrice = s.boughtQty > 0 ? Math.round((s.spent / s.boughtQty) * 100) / 100 : 0;
  }
}

const DESIGNED_CV = {
  [rel]: 130000, [tcs]: 94500, [tamo]: 0, [yesb]: 0, [hdfcb]: 68800, [gold]: 301500,
  [zer]: 336000, [nse]: 105000, [oyo]: 0,
  [aapl]: 1530, [pton]: 0, [goog]: 1520,
  [btc]: 900, [eth]: 4080, [doge]: 0,
  [nhai]: 52250, [tataNcd]: 20400, [sgb]: 296000,
  [fd1]: 0, [fd2]: 178300, [rd1]: 0, [rd2]: 186800,
  [ppf]: 1312400, [nps]: 742000,
  [flat]: 8900000, [plot]: 3100000,
};

// ---------------------------------------------------------------------------
// 12) Emit SQL
// ---------------------------------------------------------------------------
const L = [];
const p = (s = "") => L.push(s);

p("-- ============================================================================");
p("-- mock-data-7yr.sql — 7-year DML mock dataset for user 'ankit' (id 3)");
p("-- Generated by backend/sql/generate-mock-data.mjs (deterministic, seed 20261004).");
p("-- ADDITIVE ONLY: existing rows (investments <= 107, transactions <= 110,");
p("-- expenses <= 8) are untouched. Cash current_value updates at the end equal the");
p("-- CashLedgerService replay of the history inserted here.");
p("-- Load while the backend is STOPPED:");
p("--   java -cp ~/.m2/repository/com/h2database/h2/2.1.214/h2-2.1.214.jar org.h2.tools.RunScript ^");
p("--     -url \"jdbc:h2:file:./data/investmentdb;NON_KEYWORDS=VALUE,TYPE\" -user sa -script sql/mock-data-7yr.sql");
p("-- ============================================================================");
p("SET AUTOCOMMIT FALSE;");
p();

p("-- ----------------------------------------------------------------------------");
p("-- INVESTMENTS (explicit ids 200+; identity columns are GENERATED BY DEFAULT)");
p("-- ----------------------------------------------------------------------------");
for (const inv of investments) {
  const c = inv.cols;
  const cv = money(DESIGNED_CV[inv.id]);
  const common = `${inv.id}, '${inv.type}', ${USER_ID}, '${inv.type}', '${sq(c.name)}', '${c.currency ?? "INR"}', ${cv}, ${c.isListed ? "TRUE" : "FALSE"}, '${c.purchaseDate}', '${c.createdTs}', '${c.createdTs}'`;
  const add = (n, v) => ({ n, v });
  let cols = [];
  switch (inv.type) {
    case "EQUITY":
      cols = [add("symbol", `'${sq(c.symbol)}'`), add("quantity", q4(c.netQty)), add("purchase_price", money(c.avgPrice)), add("market", `'${c.market}'`)];
      break;
    case "CRYPTO":
      cols = [add("symbol", `'${sq(c.symbol)}'`), add("quantity", q8(c.netQty)), add("purchase_price", money(c.avgPrice)), add("exchange", `'${c.exchange}'`)];
      break;
    case "BOND":
      cols = [add("issuer", `'${sq(c.issuer)}'`), add("face_value", money(c.faceValue)), add("coupon_rate", c.couponRate.toFixed(2)),
              add("maturity_date", `'${c.maturity}'`), add("coupon_frequency", "NULL"), add("first_coupon_date", "NULL"),
              add("last_coupon_date", "NULL"), add("auto_generate_income", "TRUE"), add("credit_rating", `'${c.rating}'`)];
      break;
    case "FD":
      cols = [add("bank_name", `'${c.bank}'`), add("principal", money(c.principal)), add("interest_rate", c.rate.toFixed(2)),
              add("tenure_months", String(c.tenure)), add("maturity_date", `'${c.maturity}'`)];
      break;
    case "RD":
      cols = [add("bank_name", `'${c.bank}'`), add("monthly_contribution", money(c.monthly)), add("interest_rate", c.rate.toFixed(2)),
              add("tenure_months", String(c.tenure)), add("maturity_date", `'${c.maturity}'`)];
      break;
    case "NPS": case "PPF":
      cols = [add("account_number", `'${c.account}'`), add("contribution_frequency", `'${c.freq}'`), add("total_contributed", money(c.contributed))];
      break;
    case "REAL_ESTATE":
      cols = [add("property_type", `'${c.propertyType}'`), add("location", `'${sq(c.location)}'`), add("purchase_price", money(c.price))];
      break;
  }
  const colSql = cols.length ? ", " + cols.map((x) => x.n).join(", ") : "";
  const valSql = cols.length ? ", " + cols.map((x) => x.v).join(", ") : "";
  p(`INSERT INTO investments (id, investment_type, user_id, type, name, currency, current_value, is_listed, purchase_date, created_at, updated_at${colSql}) VALUES (${common}${valSql});`);
}
p();

p("-- ----------------------------------------------------------------------------");
p("-- EXPENSES (ids 3000+; paid_from_investment_id set where the cash ledger could");
p("-- take the debit; NULL = legacy-style unlinked row)");
p("-- ----------------------------------------------------------------------------");
for (const e of expenses)
  p(`INSERT INTO expenses (id, user_id, description, category, amount, currency, expense_date, notes, is_recurring, paid_from_investment_id, recurrence_frequency, created_at, updated_at) VALUES (${e.id}, ${USER_ID}, '${sq(e.description)}', '${e.category}', ${money(e.amount)}, 'INR', '${e.date}', ${e.notes ? `'${sq(e.notes)}'` : "NULL"}, ${e.recurring ? "TRUE" : "FALSE"}, ${e.paidFrom ?? "NULL"}, ${e.freq ? `'${e.freq}'` : "NULL"}, '${e.date} 09:30:00', '${e.date} 09:30:00');`);
p();

p("-- ----------------------------------------------------------------------------");
p("-- TRANSACTIONS (ids 1000+; EXPENSE rows carry the expense_id backlink)");
p("-- ----------------------------------------------------------------------------");
const isCrypto = new Set(investments.filter((i) => i.type === "CRYPTO").map((i) => i.id));
for (const t of transactions) {
  const qty = t.qty === null ? "NULL" : isCrypto.has(t.investmentId) ? q8(t.qty) : q4(t.qty);
  const price = t.price === null ? "NULL" : q4(t.price);
  p(`INSERT INTO transactions (id, investment_id, type, quantity, price_per_unit, amount, transaction_date, currency, notes, expense_id, created_at) VALUES (${t.id}, ${t.investmentId}, '${t.type}', ${qty}, ${price}, ${money(t.amount)}, '${t.date}', '${t.currency}', '${sq(t.notes)}', ${t.expenseId ?? "NULL"}, '${t.date} 10:15:00');`);
}
p();

p("-- Cash accounts: current_value := replay of the full history above");
p(`UPDATE investments SET current_value = ${money(ledger[AXIS].balance)}, updated_at = CURRENT_TIMESTAMP WHERE id = ${AXIS};`);
p(`UPDATE investments SET current_value = ${money(ledger[SBI].balance)}, updated_at = CURRENT_TIMESTAMP WHERE id = ${SBI};`);
p("COMMIT;");
p();

mkdirSync(dirname(OUT), { recursive: true });
// ASCII-sanitize: RunScript reads with the platform charset (CP1252 on Windows),
// so non-ASCII punctuation would mangle. Em-dashes → " - ".
const ascii = (s) => s.replace(/\u2014/g, " - ").replace(/[^\x00-\x7F]/g, "-");
writeFileSync(OUT, L.map(ascii).join("\r\n") + "\r\n", "utf8");

// ---------------------------------------------------------------------------
// Summary
// ---------------------------------------------------------------------------
const byType = {};
investments.forEach((i) => (byType[i.type] = (byType[i.type] || 0) + 1));
const exited = investments.filter((i) => ["EQUITY", "CRYPTO"].includes(i.type) && i.cols.netQty === 0).map((i) => i.cols.name);
const unlinkedCount = expenses.filter((e) => e.paidFrom === null).length;
console.log("Wrote", OUT);
console.log(`investments=${investments.length} expenses=${expenses.length} transactions=${transactions.length}`);
console.log("New investments by type:", JSON.stringify(byType));
console.log("Axis final balance:", money(ledger[AXIS].balance));
console.log("SBI final balance :", money(ledger[SBI].balance));
console.log("Fully-exited (loss) positions:", exited.join(", ") || "none");
console.log("Unlinked expenses:", unlinkedCount);
const catCount = {};
expenses.forEach((e) => (catCount[e.category] = (catCount[e.category] || 0) + 1));
console.log("Expense categories:", JSON.stringify(catCount));

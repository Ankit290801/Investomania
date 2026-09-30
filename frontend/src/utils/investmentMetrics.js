// Shared investment metric helpers used by the table, card, and details views.

// Cost basis used to compute P&L. EQUITY/CRYPTO use weighted lot cost; other
// types fall back to their type-specific principal so the metric stays useful.
export const getCostBasis = (inv) => {
  switch (inv.type) {
    case 'EQUITY':
    case 'CRYPTO':
      if (inv.quantity != null && inv.avgPrice != null) {
        return Number(inv.quantity) * Number(inv.avgPrice);
      }
      return null;
    case 'FD':
      return inv.principal != null ? Number(inv.principal) : null;
    case 'RD':
      if (inv.monthlyContribution != null && inv.tenureMonths != null) {
        return Number(inv.monthlyContribution) * Number(inv.tenureMonths);
      }
      return null;
    case 'PPF':
    case 'NPS':
      return inv.totalContributed != null ? Number(inv.totalContributed) : null;
    case 'BOND':
      return inv.faceValue != null ? Number(inv.faceValue) : null;
    case 'REAL_ESTATE':
      return inv.purchasePrice != null ? Number(inv.purchasePrice) : null;
    case 'CASH':
      return inv.currentValue != null ? Number(inv.currentValue) : null;
    default:
      return null;
  }
};

/**
 * P&L for an investment: { pnl, pnlPct } with nulls where the inputs are
 * missing (e.g. listed securities still waiting on their first live price).
 */
export const computePnl = (inv) => {
  const cost = getCostBasis(inv);
  const cv = inv.currentValue != null ? Number(inv.currentValue) : null;
  const pnl = cost != null && cv != null ? cv - cost : null;
  const pnlPct =
    cost != null && cost > 0 && pnl != null ? (pnl / cost) * 100 : null;
  return { cost, currentValue: cv, pnl, pnlPct };
};

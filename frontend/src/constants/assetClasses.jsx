import TrendingUp from '@mui/icons-material/TrendingUp';
import AccountBalance from '@mui/icons-material/AccountBalance';
import Savings from '@mui/icons-material/Savings';
import CalendarMonth from '@mui/icons-material/CalendarMonth';
import Shield from '@mui/icons-material/Shield';
import Home from '@mui/icons-material/Home';
import CurrencyBitcoin from '@mui/icons-material/CurrencyBitcoin';
import LocalAtm from '@mui/icons-material/LocalAtm';
import AccountBalanceWallet from '@mui/icons-material/AccountBalanceWallet';

/**
 * E2 Investments Revamp — single registry of asset classes.
 *
 * The hub (`/investments`) renders one card per entry; each class's
 * sub-screen lives at `/investments/:type`. Adding a new investment type
 * means adding an entry here (plus the DTO plumbing that already exists).
 */
export const ASSET_CLASSES = [
  {
    type: 'EQUITY',
    label: 'Equity',
    path: '/investments/equity',
    icon: TrendingUp,
    description: 'Listed stocks and ETFs'
  },
  {
    type: 'BOND',
    label: 'Bonds',
    path: '/investments/bonds',
    icon: AccountBalance,
    description: 'Coupons, issuers and maturities'
  },
  {
    type: 'CASH',
    label: 'Cash & Bank',
    path: '/investments/cash',
    icon: AccountBalanceWallet,
    description: 'Liquid balances across banks'
  },
  {
    type: 'FD',
    label: 'Fixed Deposits',
    path: '/investments/fd',
    icon: Savings,
    description: 'Term deposits and their rates'
  },
  {
    type: 'RD',
    label: 'Recurring Deposits',
    path: '/investments/rd',
    icon: CalendarMonth,
    description: 'Monthly deposit plans'
  },
  {
    type: 'NPS',
    label: 'NPS',
    path: '/investments/nps',
    icon: Shield,
    description: 'Retirement contributions'
  },
  {
    type: 'PPF',
    label: 'PPF',
    path: '/investments/ppf',
    icon: LocalAtm,
    description: 'Provident fund contributions'
  },
  {
    type: 'REAL_ESTATE',
    label: 'Real Estate',
    path: '/investments/real-estate',
    icon: Home,
    description: 'Property holdings'
  },
  {
    type: 'CRYPTO',
    label: 'Crypto',
    path: '/investments/crypto',
    icon: CurrencyBitcoin,
    description: 'Digital assets by exchange'
  }
];

export const getAssetClass = (type) =>
  ASSET_CLASSES.find((c) => c.type === type);

export const getAssetClassByPath = (pathType) =>
  ASSET_CLASSES.find(
    (c) => c.path.endsWith(`/${pathType}`)
  );

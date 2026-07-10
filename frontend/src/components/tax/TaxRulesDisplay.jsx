import React, { useState } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Tabs,
  Tab,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Divider,
  Chip
} from '@mui/material';

/**
 * Component to display tax rules for India and USA (2026)
 */
const TaxRulesDisplay = () => {
  const [selectedCountry, setSelectedCountry] = useState(0); // 0 = India, 1 = USA

  const handleCountryChange = (event, newValue) => {
    setSelectedCountry(newValue);
  };

  // India Tax Rules (2026)
  const indiaTaxRules = {
    stcg: {
      title: 'Short-Term Capital Gains (STCG)',
      description: 'Capital gains on assets held for less than the specified holding period',
      rules: [
        {
          category: 'Equity & Equity Mutual Funds',
          holdingPeriod: 'Less than 12 months',
          taxRate: '20%',
          notes: 'Listed equity shares and equity-oriented mutual funds. Changed from 15% in Budget 2024.'
        },
        {
          category: 'Other Assets (Debt, Property, Gold)',
          holdingPeriod: 'Varies by asset class',
          taxRate: 'As per applicable income tax slab',
          notes: 'Added to regular income and taxed at slab rates'
        }
      ]
    },
    ltcg: {
      title: 'Long-Term Capital Gains (LTCG)',
      description: 'Capital gains on assets held for the specified holding period or more',
      rules: [
        {
          category: 'Equity & Equity Mutual Funds',
          holdingPeriod: '12 months or more',
          taxRate: '12.5%',
          exemption: 'No exemption limit',
          notes: 'Changed from 10% (with ₹1L exemption) in Budget 2024. Exemption removed.'
        },
        {
          category: 'Debt Mutual Funds',
          holdingPeriod: '24 months or more',
          taxRate: 'As per income tax slab (indexation removed)',
          exemption: 'None',
          notes: 'Indexation benefit removed from April 1, 2023'
        },
        {
          category: 'Real Estate',
          holdingPeriod: '24 months or more',
          taxRate: '12.5% (without indexation)',
          exemption: 'Section 54/54F exemptions available',
          notes: 'Indexation benefit removed for properties bought after July 23, 2024'
        },
        {
          category: 'Gold & Other Assets',
          holdingPeriod: '24 months or more',
          taxRate: '12.5% (without indexation)',
          exemption: 'Limited exemptions available',
          notes: 'Indexation benefit removed'
        }
      ]
    },
    interest: {
      title: 'Interest Income',
      description: 'Income from interest-bearing instruments',
      rules: [
        {
          category: 'Savings Account Interest',
          taxRate: 'As per income tax slab',
          exemption: '₹10,000 under Section 80TTA (for non-senior citizens)',
          tds: 'No TDS',
          notes: 'Interest from all savings accounts combined'
        },
        {
          category: 'Fixed Deposits (Banks/NBFCs)',
          taxRate: 'As per income tax slab',
          exemption: 'None',
          tds: '10% TDS if interest > ₹40,000 (₹50,000 for senior citizens)',
          notes: 'TDS @ 20% if PAN not furnished'
        },
        {
          category: 'Senior Citizens Savings Scheme',
          taxRate: 'As per income tax slab',
          exemption: '₹50,000 under Section 80TTB (for senior citizens)',
          tds: '10% TDS if interest > ₹50,000',
          notes: 'Available only for senior citizens (60+ years)'
        },
        {
          category: 'Post Office Deposits & Bonds',
          taxRate: 'As per income tax slab',
          exemption: 'Varies by instrument',
          tds: 'No TDS on most instruments',
          notes: 'Some instruments have EEE benefits'
        }
      ]
    },
    dividend: {
      title: 'Dividend Income',
      description: 'Income from dividends received from equity shares and mutual funds',
      rules: [
        {
          category: 'Equity Shares',
          taxRate: 'As per income tax slab',
          exemption: 'None',
          tds: '10% TDS if dividend > ₹5,000 per year',
          notes: 'DDT (Dividend Distribution Tax) abolished from April 1, 2020. Taxable in hands of investor.'
        },
        {
          category: 'Mutual Funds',
          taxRate: 'As per income tax slab',
          exemption: 'None',
          tds: '10% TDS if dividend > ₹5,000 per year',
          notes: 'Includes both equity and debt mutual funds'
        },
        {
          category: 'REITs/InvITs',
          taxRate: 'As per income tax slab',
          exemption: 'None',
          tds: '10% TDS (varies by distribution type)',
          notes: 'Different components may have different tax treatment'
        }
      ]
    },
    fo: {
      title: 'Futures & Options (F&O) Trading Income',
      description: 'Income from trading in derivatives',
      rules: [
        {
          category: 'F&O Trading (Speculative)',
          taxRate: 'As per income tax slab',
          classification: 'Speculative Business Income (if intraday equity)',
          notes: 'Losses can be carried forward for 4 years, set off only against speculative income'
        },
        {
          category: 'F&O Trading (Non-Speculative)',
          taxRate: 'As per income tax slab',
          classification: 'Non-Speculative Business Income',
          notes: 'F&O on recognized exchanges - losses can be carried forward for 8 years and set off against business income'
        },
        {
          category: 'Turnover Requirement',
          taxRate: 'Audit required if turnover > ₹10 crores',
          classification: 'Presumptive taxation under 44AD not available',
          notes: 'ITR-3 mandatory; maintain books of accounts if turnover exceeds threshold'
        },
        {
          category: 'STT & Transaction Charges',
          taxRate: 'Allowed as deduction',
          classification: 'Business expense',
          notes: 'Securities Transaction Tax and brokerage are deductible'
        }
      ]
    }
  };

  // USA Tax Rules (2026)
  const usaTaxRules = {
    stcg: {
      title: 'Short-Term Capital Gains (STCG)',
      description: 'Gains on assets held for one year or less',
      rules: [
        {
          category: 'All Assets (Stocks, Bonds, Real Estate)',
          holdingPeriod: '1 year or less',
          taxRate: 'Ordinary income tax rates (10% - 37%)',
          notes: 'Taxed as ordinary income based on tax bracket. Marginal rates apply.'
        },
        {
          category: 'Tax Brackets (2026 - Single Filer)',
          holdingPeriod: 'N/A',
          taxRate: '10%: $0-$11,600 | 12%: $11,601-$47,150 | 22%: $47,151-$100,525 | 24%: $100,526-$191,950 | 32%: $191,951-$243,725 | 35%: $243,726-$609,350 | 37%: $609,351+',
          notes: 'Rates adjusted annually for inflation'
        }
      ]
    },
    ltcg: {
      title: 'Long-Term Capital Gains (LTCG)',
      description: 'Gains on assets held for more than one year',
      rules: [
        {
          category: 'Stocks & Equity Securities',
          holdingPeriod: 'More than 1 year',
          taxRate: '0%, 15%, or 20% based on income',
          notes: '0%: Income up to $47,025 (single) / $94,050 (married)\n15%: Income $47,026-$518,900 (single) / $94,051-$583,750 (married)\n20%: Income above thresholds'
        },
        {
          category: 'Real Estate',
          holdingPeriod: 'More than 1 year',
          taxRate: '0%, 15%, or 20% + 3.8% NIIT (if applicable)',
          exemption: 'Primary residence: $250,000 (single) / $500,000 (married) exclusion',
          notes: 'Must have lived in home 2 out of last 5 years for exclusion'
        },
        {
          category: 'Collectibles & Small Business Stock',
          holdingPeriod: 'More than 1 year',
          taxRate: '28% maximum (collectibles) | Special rates for QSBS',
          notes: 'Qualified Small Business Stock (QSBS) may have exclusions'
        },
        {
          category: 'Net Investment Income Tax (NIIT)',
          holdingPeriod: 'Applies to investment income',
          taxRate: '3.8% surtax',
          notes: 'Applies if MAGI exceeds $200,000 (single) / $250,000 (married)'
        }
      ]
    },
    interest: {
      title: 'Interest Income',
      description: 'Income from interest-bearing accounts and securities',
      rules: [
        {
          category: 'Savings & Checking Accounts',
          taxRate: 'Ordinary income tax rates (10% - 37%)',
          exemption: 'None',
          reporting: '1099-INT if > $10',
          notes: 'Fully taxable as ordinary income'
        },
        {
          category: 'Certificates of Deposit (CDs)',
          taxRate: 'Ordinary income tax rates (10% - 37%)',
          exemption: 'None',
          reporting: '1099-INT',
          notes: 'Taxed in year earned, even if not withdrawn'
        },
        {
          category: 'Corporate Bonds',
          taxRate: 'Ordinary income tax rates (10% - 37%)',
          exemption: 'None',
          reporting: '1099-INT',
          notes: 'Fully taxable at federal level'
        },
        {
          category: 'Municipal Bonds',
          taxRate: 'Generally tax-free at federal level',
          exemption: 'May be exempt from state/local taxes',
          reporting: '1099-INT',
          notes: 'Some muni bonds subject to AMT. State-issued bonds may be state-tax-free for residents.'
        },
        {
          category: 'Treasury Securities',
          taxRate: 'Ordinary income tax rates (federal only)',
          exemption: 'Exempt from state and local taxes',
          reporting: '1099-INT',
          notes: 'T-bills, T-notes, T-bonds, TIPS, Series I & EE bonds'
        }
      ]
    },
    dividend: {
      title: 'Dividend Income',
      description: 'Income from stock dividends',
      rules: [
        {
          category: 'Qualified Dividends',
          taxRate: '0%, 15%, or 20% (same as LTCG rates)',
          requirements: 'Held > 60 days during 121-day period around ex-dividend date',
          reporting: '1099-DIV (Box 1b)',
          notes: 'Must be from U.S. corporations or qualified foreign corporations'
        },
        {
          category: 'Ordinary (Non-Qualified) Dividends',
          taxRate: 'Ordinary income tax rates (10% - 37%)',
          requirements: 'N/A',
          reporting: '1099-DIV (Box 1a)',
          notes: 'REITs, money market funds, and dividends on employee stock options'
        },
        {
          category: 'Capital Gain Distributions',
          taxRate: 'Long-term capital gains rates (0%, 15%, 20%)',
          requirements: 'From mutual funds or REITs',
          reporting: '1099-DIV (Box 2a)',
          notes: 'Treated as LTCG regardless of how long you held the fund'
        },
        {
          category: 'Return of Capital',
          taxRate: 'Not taxable (reduces cost basis)',
          requirements: 'N/A',
          reporting: '1099-DIV (Box 3)',
          notes: 'Not taxed until basis reaches zero, then treated as capital gain'
        }
      ]
    },
    fo: {
      title: 'Futures & Options (F&O) Trading Income',
      description: 'Income from derivatives trading',
      rules: [
        {
          category: 'Section 1256 Contracts',
          taxRate: '60% LTCG (0/15/20%) + 40% STCG (ordinary rates)',
          classification: 'Regulated futures, broad-based index options, dealer equity options',
          notes: 'Mark-to-market at year-end even if positions are open. Blended rate ~27% for higher earners.'
        },
        {
          category: 'Equity Options (Non-Section 1256)',
          taxRate: 'ST or LT rates based on holding period',
          classification: 'Treated as capital gains/losses',
          notes: 'Options on individual stocks are not Section 1256 contracts'
        },
        {
          category: 'Trader Tax Status (TTS)',
          taxRate: 'If qualified: deduct trading expenses, avoid wash sale rules partially',
          classification: 'Must trade substantially, regularly, and continuously',
          notes: 'Can elect Mark-to-Market (MTM) accounting under Section 475(f). Must elect by filing deadline.'
        },
        {
          category: 'Investor Status (Default)',
          taxRate: 'Capital gains treatment, limited deductions',
          classification: 'Investment interest deduction limited to investment income',
          notes: 'Subject to wash sale rules, $3,000 annual capital loss deduction limit'
        },
        {
          category: 'Wash Sale Rules',
          taxRate: 'Loss disallowed if repurchased within 30 days',
          classification: 'Applies to substantially identical securities',
          notes: 'Does not apply to Section 1256 contracts or if TTS with MTM election'
        }
      ]
    }
  };

  const currentRules = selectedCountry === 0 ? indiaTaxRules : usaTaxRules;
  const countryName = selectedCountry === 0 ? 'India' : 'USA';

  const renderRuleSection = (ruleData) => (
    <Card sx={{ mb: 3 }}>
      <CardContent>
        <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
          <Typography variant="h6" sx={{ flexGrow: 1 }}>
            {ruleData.title}
          </Typography>
          <Chip label={countryName} color="primary" size="small" />
        </Box>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
          {ruleData.description}
        </Typography>
        <TableContainer>
          <Table size="small">
            <TableHead>
              <TableRow sx={{ backgroundColor: 'action.hover' }}>
                <TableCell><strong>Category</strong></TableCell>
                <TableCell><strong>Tax Rate</strong></TableCell>
                <TableCell><strong>Details</strong></TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {ruleData.rules.map((rule, index) => (
                <TableRow key={index} hover>
                  <TableCell sx={{ verticalAlign: 'top', minWidth: 200 }}>
                    <Typography variant="body2" sx={{ fontWeight: 500 }}>
                      {rule.category}
                    </Typography>
                    {rule.holdingPeriod && (
                      <Typography variant="caption" color="text.secondary" display="block" sx={{ mt: 0.5 }}>
                        Holding: {rule.holdingPeriod}
                      </Typography>
                    )}
                  </TableCell>
                  <TableCell sx={{ verticalAlign: 'top', minWidth: 150 }}>
                    <Typography variant="body2" sx={{ fontWeight: 500, color: 'primary.main' }}>
                      {rule.taxRate}
                    </Typography>
                    {rule.exemption && (
                      <Typography variant="caption" color="success.main" display="block" sx={{ mt: 0.5 }}>
                        Exemption: {rule.exemption}
                      </Typography>
                    )}
                    {rule.tds && (
                      <Typography variant="caption" color="warning.main" display="block" sx={{ mt: 0.5 }}>
                        {rule.tds}
                      </Typography>
                    )}
                    {rule.classification && (
                      <Typography variant="caption" color="info.main" display="block" sx={{ mt: 0.5 }}>
                        {rule.classification}
                      </Typography>
                    )}
                  </TableCell>
                  <TableCell sx={{ verticalAlign: 'top' }}>
                    <Typography variant="body2" sx={{ whiteSpace: 'pre-line' }}>
                      {rule.notes}
                    </Typography>
                    {rule.requirements && (
                      <Typography variant="caption" color="text.secondary" display="block" sx={{ mt: 0.5 }}>
                        Requirements: {rule.requirements}
                      </Typography>
                    )}
                    {rule.reporting && (
                      <Typography variant="caption" color="text.secondary" display="block" sx={{ mt: 0.5 }}>
                        Reporting: {rule.reporting}
                      </Typography>
                    )}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      </CardContent>
    </Card>
  );

  return (
    <Box>
      <Box sx={{ mb: 3 }}>
        <Typography variant="h5" gutterBottom>
          Tax Rules Reference (2026)
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Comprehensive tax rules for investment income in India and USA. 
          Tax calculation features will be implemented in future phases.
        </Typography>
      </Box>

      <Paper sx={{ mb: 3 }}>
        <Tabs
          value={selectedCountry}
          onChange={handleCountryChange}
          indicatorColor="primary"
          textColor="primary"
          centered
        >
          <Tab label="India 🇮🇳" />
          <Tab label="USA 🇺🇸" />
        </Tabs>
      </Paper>

      {renderRuleSection(currentRules.stcg)}
      {renderRuleSection(currentRules.ltcg)}
      {renderRuleSection(currentRules.interest)}
      {renderRuleSection(currentRules.dividend)}
      {renderRuleSection(currentRules.fo)}

      <Card sx={{ mt: 3, backgroundColor: 'info.light' }}>
        <CardContent>
          <Typography variant="subtitle2" gutterBottom>
            ⚠️ Important Notes:
          </Typography>
          <Typography variant="body2" component="div">
            <ul style={{ margin: 0, paddingLeft: 20 }}>
              <li>These are general tax rules as of 2026. Always consult a tax professional for specific situations.</li>
              <li>Tax laws are subject to change. Verify current rates before filing.</li>
              <li>State/local taxes may apply in addition to federal taxes (USA).</li>
              <li>Tax calculation and report generation features are planned for future implementation.</li>
            </ul>
          </Typography>
        </CardContent>
      </Card>
    </Box>
  );
};

export default TaxRulesDisplay;

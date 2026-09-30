import * as yup from 'yup';

// Date helpers shared by every entry form.
//
// Bug INV-4: `<input type="date">` yields plain 'YYYY-MM-DD' strings, but
// yup's date() schema coerced them into JS Date objects at UTC midnight.
// Axios then serialized those Dates as UTC ISO timestamps and Jackson mapped
// them back to a LocalDate one day early (and outright rejected them in
// UTC-negative timezones). Rules to keep this from regressing:
//   1. Date values crossing the API boundary must stay 'YYYY-MM-DD' strings.
//   2. Never call .toISOString() to build a default date input value.

export const DATE_INPUT_PATTERN = /^\d{4}-\d{2}-\d{2}$/;

/** Today's date as a 'YYYY-MM-DD' string in the user's local timezone. */
export const todayInputValue = () => {
  const now = new Date();
  const month = `${now.getMonth() + 1}`.padStart(2, '0');
  const day = `${now.getDate()}`.padStart(2, '0');
  return `${now.getFullYear()}-${month}-${day}`;
};

/**
 * Yup schema for a required date-input field. Kept as a string on purpose:
 * the wire format and <input type="date"> are both 'YYYY-MM-DD'.
 */
export const requiredDateString = (label) =>
  yup
    .string()
    .required(`${label} is required`)
    .matches(DATE_INPUT_PATTERN, `${label} must be a valid date`);

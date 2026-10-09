import { useEffect } from 'react';
import SettingsPage from '../../pages/SettingsPage';

/**
 * E8 — tour anchors for the Settings page.
 *
 * `pages/SettingsPage.jsx` is stakeholder-owned and must not be edited, so
 * its two site-guide landmarks (Preferences, Backup & Restore) are stamped
 * from the outside: this wrapper renders the page untouched and tags the
 * matching cards by their stable heading text. Purely additive DOM
 * attributes — zero behaviour or content change to the page itself.
 */
const ANCHORS = [
  { heading: 'Preferences', attr: 'settings-preferences' },
  { heading: 'Backup & Restore', attr: 'settings-backup' }
];

const SettingsWithTourAnchors = () => {
  useEffect(() => {
    const stamp = () => {
      for (const { heading, attr } of ANCHORS) {
        const paper = Array.from(document.querySelectorAll('.MuiPaper-root')).find((el) =>
          // Typography variant="h6" renders as <p class="MuiTypography-h6 …">,
          // so match the class, not a tag.
          Array.from(el.querySelectorAll('.MuiTypography-h6')).some(
            (h) => h.textContent === heading
          )
        );
        if (paper) paper.setAttribute('data-tour', attr);
      }
    };
    stamp(); // headings render synchronously with the page shell
    const timer = setTimeout(stamp, 400); // cards may mount after data loads
    return () => clearTimeout(timer);
  }, []);

  return <SettingsPage />;
};

export default SettingsWithTourAnchors;

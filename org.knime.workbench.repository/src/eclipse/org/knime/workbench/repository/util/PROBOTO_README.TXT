... funny story: If we load the Roboto family TTF fonts into SWT by hand, then the HTML rendering of CSS for the node descriptions does not load the WOFF2 versions and then uses whatever it can find; should it be insufficient (for example, we have loaded a single Roboto-Bold.ttf into SWT) it will use only the Roboto fonts it can find having been loaded into SWT.
At the same time, if we let the HTML renderer load the WOFF2 fonts and render a view with them, and then ask SWT for Roboto fonts, SWT says it has not heard of them.

.. sigh.

The "Proboto" fonts in this directory are the Google Roboto TTF fonts which have been modified via FontForge to have the name Proboto instead; this keep the HTML rendering loading the WOFF2 fonts, and allows us to use Roboto TTF fonts in SWT widgets.


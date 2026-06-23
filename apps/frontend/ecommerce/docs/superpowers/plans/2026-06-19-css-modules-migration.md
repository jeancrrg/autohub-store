# CSS Modules Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Convert all 38 components in `components/` from Tailwind inline classes / inline `style` props to co-located CSS Modules using only CSS variables for colors.

**Architecture:** Each component gets a `ComponentName.module.css` in the same folder as its `.tsx`. All colors reference CSS variables defined in `app/globals.css`. Visual output stays identical.

**Tech Stack:** Next.js CSS Modules, CSS custom properties (variables), TypeScript/React.

## Global Constraints

- CSS class names in modules: camelCase (e.g., `.cardWrapper`, `.titleText`)
- No hardcoded hex values or `rgba()` in component CSS files — use only `var(--color-*)` 
- No Tailwind utility classes remaining in converted components (exception: external `className` props on `ui/` components may still accept Tailwind from callers)
- Visual output must be pixel-identical to current implementation
- Today's date: 2026-06-19

---

## New CSS Variables Reference (all added in Task 1)

```css
/* whites */
--color-white: #ffffff;

/* dark scale extensions */
--color-dark-4: #222222;
--color-dark-5: #252525;
--color-dark-6: #2a2a2a;
--color-dark-7: #333333;

/* gray text scale */
--color-gray-900: #444444;
--color-gray-800: #555555;
--color-gray-700: #666666;
--color-gray-600: #888888;
--color-gray-500: #9ca3af;
--color-gray-400: #aaaaaa;
--color-gray-350: #bbbbbb;
--color-gray-300: #cccccc;
--color-gray-250: #d0d0d0;
--color-gray-200: #e5e5e5;
--color-gray-100: #eeeeee;
--color-gray-img: #f4f4f4;

/* status: success */
--color-success: #22c55e;
--color-success-dark: #16a34a;
--color-success-darker: #15803d;
--color-success-light: #dcfce7;
--color-success-muted: #4ade80;
--color-success-muted-bg: rgba(74, 222, 128, 0.1);

/* status: error */
--color-error: #ef4444;
--color-error-dark: #dc2626;
--color-error-darker: #b91c1c;
--color-error-light: #fee2e2;
--color-error-muted: #f87171;
--color-error-muted-bg: rgba(248, 113, 113, 0.1);

/* status: info */
--color-info: #1d4ed8;
--color-info-mid: #2563eb;
--color-info-light: #dbeafe;

/* status: warning */
--color-warning: #a16207;
--color-warning-light: #fef9c3;

/* status: notice (orange) */
--color-notice: #c2410c;
--color-notice-light: #ffedd5;

/* semi-transparent overlays */
--color-primary-35: rgba(225, 6, 0, 0.35);
--color-primary-20: rgba(225, 6, 0, 0.2);
--color-primary-10: rgba(225, 6, 0, 0.1);
--color-primary-08: rgba(225, 6, 0, 0.08);
--color-primary-04: rgba(225, 6, 0, 0.04);
--color-primary-border: rgba(225, 6, 0, 0.2);
--color-dark-overlay-60: rgba(11, 11, 11, 0.6);
--color-dark-overlay-10: rgba(11, 11, 11, 0.1);
--color-white-grid: rgba(255, 255, 255, 0.012);
```

---

### Task 1: Extend globals.css with new CSS variables

**Files:**
- Modify: `app/globals.css`

- [ ] Open `app/globals.css` and add all variables from the "New CSS Variables Reference" section above inside the `@theme {}` block, after the existing variables.

- [ ] Verify the file now has the full set and no syntax errors (check for missing semicolons).

---

### Task 2: layout/ group — Header, Footer, AdminSidebar

**Files:**
- Create: `components/layout/Header.module.css`
- Modify: `components/layout/Header.tsx`
- Create: `components/layout/Footer.module.css`
- Modify: `components/layout/Footer.tsx`
- Create: `components/layout/AdminSidebar.module.css`
- Modify: `components/layout/AdminSidebar.tsx`

**Key notes:**
- `Header.tsx` uses inline `style={}` props AND a `<style>` JSX tag for `.header-icon:hover` and `.nav-link:hover`. Both move to the CSS module. Remove the `<style>` tag entirely.
- `Header.tsx` cart badge uses conditional rendering with inline styles — convert to CSS module classes.
- `AdminSidebar.tsx` has conditional active/inactive nav item classes — use two CSS classes (`.navItemActive`, `.navItemInactive`) and pick in TSX with a ternary.
- No `<style>` JSX tags should remain anywhere in these files after conversion.

**CSS class naming for Header:**
```
.announcementBar, .announcementContent, .announcementText
.mainHeader, .headerInner, .logo, .logoName, .logoAccent, .logoSub
.searchForm, .searchInput, .searchButton
.iconGroup, .headerIcon, .iconLabel
.cartIconWrapper, .cartBadge
.nav, .navInner, .categoriesBtn, .categoriesIcon, .categoriesLabel, .navLink, .navLinkOffers
```

**CSS class naming for Footer:**
```
.footer, .footerGrid, .brandCol, .brandName, .brandAccent, .brandDesc
.colTitle, .footerLink, .footerBottom, .footerCopy
```

**CSS class naming for AdminSidebar:**
```
.sidebar, .logoArea, .logoTitle, .logoSub
.nav, .navItemActive, .navItemInactive, .navIcon
.footerArea, .backLink
```

- [ ] Create all 3 CSS module files with correct CSS using `var(--color-*)` variables only.
- [ ] Update all 3 TSX files: add `import styles from './ComponentName.module.css'` and replace all `style={}` props and `className` strings with `styles.xxx`.
- [ ] Remove all `<style>` JSX tags from Header.tsx.
- [ ] Verify no hardcoded hex colors remain in TSX files.

---

### Task 3: home/ group — 7 components

**Files (create each .module.css + modify each .tsx):**
- `components/home/HeroBanner.module.css` / `HeroBanner.tsx`
- `components/home/CategoryGrid.module.css` / `CategoryGrid.tsx`
- `components/home/ProductSection.module.css` / `ProductSection.tsx`
- `components/home/PromoBanner.module.css` / `PromoBanner.tsx`
- `components/home/BrandsGrid.module.css` / `BrandsGrid.tsx`
- `components/home/Newsletter.module.css` / `Newsletter.tsx`
- `components/home/TrustBadges.module.css` / `TrustBadges.tsx`

**Key notes:**
- `HeroBanner.tsx` uses complex inline styles including gradients and `mix-blend-mode`. Move ALL to CSS module. Gradients that use rgba: use the `--color-primary-*` overlay variables.
- `CategoryGrid.tsx` has a `<style>` JSX tag (`.category-card:hover`). Remove it; add `.categoryCard:hover` rule in CSS module.
- `ProductSection.tsx` has an internal `ProductCard` component AND two `<style>` JSX tags. Remove style tags; add `:hover` rules in CSS module for `.productCard:hover` and `.addBtn:hover`.
- `BrandsGrid.tsx` has a `<style>` tag for `.brand-card:hover .brand-name`. Remove; use CSS module `.brandCard:hover .brandName {}`.
- `Newsletter.tsx` has a `<style>` tag for `.newsletter-btn:hover`. Remove; add `.newsletterBtn:hover` in CSS module.
- `PromoBanner.tsx` has a `<style>` tag for `.promo-btn:hover`. Remove; add `.promoBtn:hover` in CSS module.

**Representative CSS example (HeroBanner):**
```css
.section { background: var(--color-dark); min-height: 560px; position: relative; overflow: hidden; display: flex; align-items: center; }
.gridPattern { position: absolute; inset: 0; background: repeating-linear-gradient(90deg, transparent 0, transparent 119px, var(--color-white-grid) 119px, var(--color-white-grid) 120px); }
.diagonalLine { position: absolute; left: 47%; top: 0; bottom: 0; width: 2px; background: linear-gradient(to bottom, transparent 0%, var(--color-primary) 15%, var(--color-primary) 85%, transparent 100%); transform: skewX(-5deg); z-index: 2; }
```

- [ ] Create all 7 CSS module files.
- [ ] Update all 7 TSX files: add import, replace styles, remove `<style>` tags.
- [ ] Verify no hardcoded hex colors in TSX files.

---

### Task 4: catalog/ group — ProductCard, ProductGrid, FilterSidebar, SortBar, Pagination

**Files:**
- Create/modify: `components/catalog/ProductCard.module.css` + `.tsx`
- Create/modify: `components/catalog/ProductGrid.module.css` + `.tsx`
- Create/modify: `components/catalog/FilterSidebar.module.css` + `.tsx`
- Create/modify: `components/catalog/SortBar.module.css` + `.tsx`
- Create/modify: `components/catalog/Pagination.module.css` + `.tsx`

**Key notes:**
- `ProductCard.tsx` currently uses Tailwind classes. Convert to CSS module. Hover state on the card (`:hover { border-color: var(--color-primary); }`) is handled in CSS module.
- `ProductGrid.tsx` uses Tailwind for layout (`flex gap-8`, `flex-1`, `grid grid-cols-3 gap-6`, `py-20 text-center`). Convert to CSS module.
- `FilterSidebar.tsx`: conditional active/inactive button styles → `.filterBtnActive` / `.filterBtnInactive` classes.
- `Pagination.tsx`: active/inactive page button → `.pageActive` / `.pageInactive` classes.

**CSS class naming for ProductCard:**
```
.card, .imageArea, .tagBadge, .soldOutBadge, .imagePlaceholder
.body, .brand, .name, .stars, .reviewCount
.priceArea, .oldPrice, .price, .installments
.button
```

- [ ] Create all 5 CSS module files.
- [ ] Update all 5 TSX files.

---

### Task 5: product/ group — ImageGallery, ProductInfo, ProductTabs

**Files:**
- Create/modify: `components/product/ImageGallery.module.css` + `.tsx`
- Create/modify: `components/product/ProductInfo.module.css` + `.tsx`
- Create/modify: `components/product/ProductTabs.module.css` + `.tsx`

**Key notes:**
- `ProductInfo.tsx` uses `text-green-600` and `text-red-500` for stock status → `.inStock { color: var(--color-success-dark); }` / `.outOfStock { color: var(--color-error); }`.
- `ProductInfo.tsx` also has `text-green-600` for PIX price → `.pixPrice { color: var(--color-success-dark); }`.
- `ProductTabs.tsx` has active/inactive tab styles → `.tabActive` / `.tabInactive`.
- `ProductTabs.tsx` has alternating table rows (`bg-gray-light` / `bg-white`) → `.rowEven` / `.rowOdd`.

- [ ] Create all 3 CSS module files.
- [ ] Update all 3 TSX files.

---

### Task 6: cart/ group — CartTable, OrderSummary

**Files:**
- Create/modify: `components/cart/CartTable.module.css` + `.tsx`
- Create/modify: `components/cart/OrderSummary.module.css` + `.tsx`

**Key notes:**
- `CartTable.tsx`: remove button uses `hover:text-red-500` → `.removeBtn:hover { color: var(--color-error); }`.
- `OrderSummary.tsx`: shipping "GRÁTIS" is green → `.shippingFree { color: var(--color-success-dark); }`. Discount row also green → `.discountText { color: var(--color-success-dark); }`. Coupon error is primary red → `.couponError { color: var(--color-primary); }`.

- [ ] Create both CSS module files.
- [ ] Update both TSX files.

---

### Task 7: checkout/ group — 5 components

**Files:**
- Create/modify: `components/checkout/CheckoutStepper.module.css` + `.tsx`
- Create/modify: `components/checkout/AddressForm.module.css` + `.tsx`
- Create/modify: `components/checkout/DeliveryStep.module.css` + `.tsx`
- Create/modify: `components/checkout/PaymentStep.module.css` + `.tsx`
- Create/modify: `components/checkout/ConfirmationStep.module.css` + `.tsx`

**Key notes:**
- `CheckoutStepper.tsx` step indicator has 3 states: done (green), active (primary), pending → `.stepDone`, `.stepActive`, `.stepPending`.
- Connector line has 2 states: done (green), pending → `.connectorDone`, `.connectorPending`.
- `ConfirmationStep.tsx` success icon circle: `bg-green-100` → `.successCircle { background: var(--color-success-light); }`.
- Confirmation status "Confirmado" text is green → `.statusConfirmed { color: var(--color-success-dark); }`.
- `AddressForm.tsx` error text for UF field → `.fieldError { color: var(--color-primary); }`.
- `DeliveryStep.tsx` selected/unselected shipping option borders.
- `PaymentStep.tsx` active/inactive method tab buttons.

- [ ] Create all 5 CSS module files.
- [ ] Update all 5 TSX files.

---

### Task 8: account/ group — AccountSidebar, OrdersTab, ProfileTab

**Files:**
- Create/modify: `components/account/AccountSidebar.module.css` + `.tsx`
- Create/modify: `components/account/OrdersTab.module.css` + `.tsx`
- Create/modify: `components/account/ProfileTab.module.css` + `.tsx`

**Key notes:**
- `AccountSidebar.tsx`: nav item active/inactive → `.navItemActive`, `.navItemInactive`. Logout hover red → `.logoutBtn:hover { color: var(--color-error); }`.
- `OrdersTab.tsx` has `STATUS_STYLES` mapping that uses Tailwind color classes. Replace with CSS module classes:
  ```tsx
  const STATUS_CLASSES: Record<string, string> = {
    'Entregue': styles.statusSuccess,
    'Em trânsito': styles.statusInfo,
    'Processando': styles.statusWarning,
    'Cancelado': styles.statusError,
    'Aguardando pagamento': styles.statusNotice,
  }
  ```
  CSS module:
  ```css
  .statusSuccess { color: var(--color-success-darker); background: var(--color-success-light); }
  .statusInfo { color: var(--color-info); background: var(--color-info-light); }
  .statusWarning { color: var(--color-warning); background: var(--color-warning-light); }
  .statusError { color: var(--color-error-darker); background: var(--color-error-light); }
  .statusNotice { color: var(--color-notice); background: var(--color-notice-light); }
  ```

- [ ] Create all 3 CSS module files.
- [ ] Update all 3 TSX files.

---

### Task 9: admin/ group — 7 components

**Files:**
- Create/modify: `components/admin/MetricsCards.module.css` + `.tsx`
- Create/modify: `components/admin/RevenueChart.module.css` + `.tsx`
- Create/modify: `components/admin/TopProducts.module.css` + `.tsx`
- Create/modify: `components/admin/RecentOrders.module.css` + `.tsx`
- Create/modify: `components/admin/ProductsTable.module.css` + `.tsx`
- Create/modify: `components/admin/OrdersTable.module.css` + `.tsx`
- Create/modify: `components/admin/UsersTable.module.css` + `.tsx`

**Key notes:**
- `MetricsCards.tsx`: metric badge has positive/negative states (`text-green-400 bg-green-400/10` / `text-red-400 bg-red-400/10`) → `.badgePositive` / `.badgeNegative`.
- `RecentOrders.tsx`: `order.statusBg` and `order.statusColor` come from data as Tailwind classes. Replace with local CSS module status mapping (same pattern as OrdersTab above, based on `order.status` string).
- `ProductsTable.tsx`: stock text color `text-green-400` / `text-red-400` → `.stockIn` / `.stockOut`. Status badge same → `.badgeActive` / `.badgeInactive`.
- `OrdersTable.tsx`: status from `order.statusBg`/`order.statusColor` — same pattern as RecentOrders.
- `UsersTable.tsx`: toggle switch has active/inactive state → `.toggleActive` / `.toggleInactive`. Status badge same.
- `AdminSidebar.tsx` has `bg-[#080808]` (non-standard Tailwind). Variable: define as `--color-dark-bg: #080808` in globals.css OR use `var(--color-dark)` (`#0b0b0b`) — they're visually almost identical; use `--color-dark`.

**RecentOrders/OrdersTable status mapping:**
Check `adminOrders` data to see available status strings, then create matching CSS module classes using same color pattern as OrdersTab.

- [ ] Create all 7 CSS module files.
- [ ] Update all 7 TSX files.

---

### Task 10: ui/ group — Badge, Button, Input

**Files:**
- Create/modify: `components/ui/Badge.module.css` + `.tsx`
- Create/modify: `components/ui/Button.module.css` + `.tsx`
- Create/modify: `components/ui/Input.module.css` + `.tsx`

**Key notes:**
- These components accept external `className` prop — keep the `cn()` merge for backward compat. Convert internal base styles to CSS module class.
- `Badge.tsx`: base class (`font-rajdhani font-600 text-xs tracking-wider px-2 py-0.5`) → `.badge` in module. Keep `bgClass` and `className` props for external use (callers pass Tailwind color classes).
- `Button.tsx`: base + variant + size classes → `.base`, `.variantPrimary`, `.variantOutline`, `.variantGhost`, `.sizeSm`, `.sizeMd`, `.sizeLg`. Keep `className` prop for merge.
- `Input.tsx`: base input class + error state → `.wrapper`, `.label`, `.input`, `.inputError`, `.errorMsg`.

```css
/* Button.module.css example */
.base { font-family: var(--font-rajdhani); font-weight: 600; letter-spacing: 0.1em; transition: background-color 0.2s, color 0.2s, border-color 0.2s; }
.base:disabled { opacity: 0.5; cursor: not-allowed; }
.variantPrimary { background: var(--color-primary); color: var(--color-white); }
.variantPrimary:hover { background: var(--color-primary-dark); }
.variantOutline { border: 1px solid var(--color-gray-border); color: var(--color-dark); }
.variantOutline:hover { border-color: var(--color-dark); }
.variantGhost { color: var(--color-gray-500); }
.variantGhost:hover { color: var(--color-dark); }
.sizeSm { padding: 8px 16px; font-size: 0.875rem; }
.sizeMd { padding: 12px 24px; font-size: 0.875rem; }
.sizeLg { padding: 16px 32px; font-size: 1rem; }
```

- [ ] Create all 3 CSS module files.
- [ ] Update all 3 TSX files.
- [ ] Verify `cn()` still works for callers that pass extra className.

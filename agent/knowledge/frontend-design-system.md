# Frontend Design System

## Platform baseline (Angular 21)
- Standalone-first components; NgModule only when required by 3rd-party libs
- Signals for local view state; computed for derived values
- Control flow blocks (@if/@for/@switch) in templates
- Typed reactive forms
- `provideHttpClient` + interceptor chain in `app.config.ts`

## Visual direction
- Theme: Enterprise Dark Command Center
- Colors: deep navy base, amber accent
- Fonts: IBM Plex Sans (UI), IBM Plex Mono (data)

## Design tokens
- All colors via CSS variables in styles/_tokens.scss
- Spacing scale: --spacing-1..8, radius scale: --radius-1..6
- Shadows: --shadow-1..3, borders: --border-subtle/strong
- Motion: --motion-fast/normal/slow (respects prefers-reduced-motion)

## Component rules
- Component prefix: ep-
- No hardcoded colors in components; use CSS variables
- 100% text via ngx-translate (vi default, en toggle)
- Icons render through `ep-icon`, backed by `@lucide/angular` for consistent SVG paths

## Shared components (core)
- ep-button, ep-badge, ep-card, ep-table, ep-modal
- ep-form-field, ep-avatar, ep-sla-bar, ep-stat-card
- ep-amount, ep-approval-action, ep-filter-bar
- ep-breadcrumb, ep-lang-switcher, ep-empty-state, ep-skeleton
-...

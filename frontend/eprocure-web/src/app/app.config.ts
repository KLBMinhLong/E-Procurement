import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import {
  LucideActivity,
  LucideBell,
  LucideBox,
  LucideChartNoAxesCombined,
  LucideCheck,
  LucideChevronRight,
  LucideCircleAlert,
  LucideClock,
  LucideClipboardList,
  LucideFilter,
  LucideGlobe,
  LucideHome,
  LucideInbox,
  LucideLanguages,
  LucideLayoutDashboard,
  LucideLoaderCircle,
  LucideLock,
  LucideLogIn,
  LucideLogOut,
  LucideMenu,
  LucidePackageCheck,
  LucidePlus,
  LucideSearch,
  LucideSettings,
  LucideShieldCheck,
  LucideShoppingCart,
  LucideTruck,
  LucideUser,
  LucideUsers,
  LucideWarehouse,
  LucideX,
  provideLucideConfig,
  provideLucideIcons
} from '@lucide/angular';
import { provideTranslateService } from '@ngx-translate/core';
import { provideTranslateHttpLoader } from '@ngx-translate/http-loader';

import { routes } from './app.routes';
import { environment } from '../environments/environment';
import { API_BASE_URL, ENCRYPTION_ENABLED, WS_BASE_URL } from './core/http/api-tokens';
import { credentialsInterceptor } from './core/http/credentials.interceptor';
import { errorInterceptor } from './core/http/error.interceptor';
import { idempotencyInterceptor } from './core/http/idempotency.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(
      withInterceptors([
        credentialsInterceptor,
        idempotencyInterceptor,
        errorInterceptor
      ])
    ),
    provideTranslateService({
      fallbackLang: 'vi',
      lang: 'vi',
      loader: provideTranslateHttpLoader({
        prefix: '/assets/i18n/',
        suffix: '.json'
      })
    }),
    provideLucideIcons(
      LucideActivity,
      LucideBell,
      LucideBox,
      LucideChartNoAxesCombined,
      LucideCheck,
      LucideChevronRight,
      LucideCircleAlert,
      LucideClock,
      LucideClipboardList,
      LucideFilter,
      LucideGlobe,
      LucideHome,
      LucideInbox,
      LucideLanguages,
      LucideLayoutDashboard,
      LucideLoaderCircle,
      LucideLock,
      LucideLogIn,
      LucideLogOut,
      LucideMenu,
      LucidePackageCheck,
      LucidePlus,
      LucideSearch,
      LucideSettings,
      LucideShieldCheck,
      LucideShoppingCart,
      LucideTruck,
      LucideUser,
      LucideUsers,
      LucideWarehouse,
      LucideX
    ),
    provideLucideConfig({
      strokeWidth: 1.8,
      absoluteStrokeWidth: true
    }),
    { provide: API_BASE_URL, useValue: environment.apiBaseUrl },
    { provide: WS_BASE_URL, useValue: environment.wsBaseUrl },
    { provide: ENCRYPTION_ENABLED, useValue: environment.encryptionEnabled }
  ]
};

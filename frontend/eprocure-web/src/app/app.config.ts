import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import {
  LucideActivity,
  LucideArrowLeft,
  LucideBell,
  LucideBox,
  LucideChartNoAxesCombined,
  LucideCheck,
  LucideChevronRight,
  LucideChevronDown,
  LucideCircleAlert,
  LucideClock,
  LucideClipboardList,
  LucideCopy,
  LucideDownload,
  LucideEye,
  LucideEyeOff,
  LucideFilter,
  LucideFolder,
  LucideGlobe,
  LucideHistory,
  LucideHome,
  LucideInbox,
  LucideKey,
  LucideKeyRound,
  LucideLanguages,
  LucideLayoutDashboard,
  LucideLoaderCircle,
  LucideLock,
  LucideLogIn,
  LucideLogOut,
  LucideMapPinOff,
  LucideMenu,
  LucideNetwork,
  LucidePackageCheck,
  LucidePaperclip,
  LucidePencil,
  LucidePlus,
  LucideRefreshCw,
  LucideSave,
  LucideSearch,
  LucideSend,
  LucideSettings,
  LucideShield,
  LucideShieldAlert,
  LucideShieldCheck,
  LucideShieldOff,
  LucideShoppingCart,
  LucideTruck,
  LucideUploadCloud,
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
import { encryptionInterceptor } from './core/http/encryption.interceptor';
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
        encryptionInterceptor,
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
      LucideArrowLeft,
      LucideBell,
      LucideBox,
      LucideChartNoAxesCombined,
      LucideCheck,
      LucideChevronRight,
      LucideChevronDown,
      LucideCircleAlert,
      LucideClock,
      LucideClipboardList,
      LucideCopy,
      LucideDownload,
      LucideEye,
      LucideEyeOff,
      LucideFilter,
      LucideFolder,
      LucideGlobe,
      LucideHistory,
      LucideHome,
      LucideInbox,
      LucideKey,
      LucideKeyRound,
      LucideLanguages,
      LucideLayoutDashboard,
      LucideLoaderCircle,
      LucideLock,
      LucideLogIn,
      LucideLogOut,
      LucideMapPinOff,
      LucideMenu,
      LucideNetwork,
      LucidePackageCheck,
      LucidePaperclip,
      LucidePencil,
      LucidePlus,
      LucideRefreshCw,
      LucideSave,
      LucideSearch,
      LucideSend,
      LucideSettings,
      LucideShield,
      LucideShieldAlert,
      LucideShieldCheck,
      LucideShieldOff,
      LucideShoppingCart,
      LucideTruck,
      LucideUploadCloud,
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

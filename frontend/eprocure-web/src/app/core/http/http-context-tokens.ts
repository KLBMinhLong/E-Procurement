import { HttpContextToken } from '@angular/common/http';

export const BYPASS_ERROR_INTERCEPTOR_TOKEN = new HttpContextToken(() => false);

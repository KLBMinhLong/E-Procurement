import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { AuthService } from './auth.service';

export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // Nếu đã hydrate đầy đủ (permissions từ /users/me), cho qua ngay
  if (authService.isFullyHydrated()) {
    return true;
  }

  // Trường hợp vừa login (currentUser có nhưng chưa fully hydrated)
  // hoặc reload trang → luôn gọi hydrateUserContext để lấy đủ permissions
  return authService.hydrateUserContext().pipe(
    map(() => true),
    catchError(() => of(router.createUrlTree(['/login'])))
  );
};


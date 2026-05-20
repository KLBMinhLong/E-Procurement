import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { AuthService } from './auth.service';

export const loginGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // Nếu user đã đăng nhập, redirect tới dashboard
  if (authService.currentUser()) {
    return router.createUrlTree(['/dashboard']);
  }

  // Kiểm tra xem có session hợp lệ từ backend không
  return authService.hydrateUserContext().pipe(
    map(() => {
      // Nếu hydrate thành công (user đã đăng nhập), redirect tới dashboard
      return router.createUrlTree(['/dashboard']);
    }),
    catchError(() => {
      // Nếu hydrate thất bại (user chưa đăng nhập), allow access tới login page
      return of(true);
    })
  );
};

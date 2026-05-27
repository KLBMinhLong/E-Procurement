import { Injectable, signal } from '@angular/core';

export type ToastKind = 'info' | 'success' | 'warning' | 'error';

export interface ToastMessage {
  id: string;
  kind: ToastKind;
  message: string;
  isTranslationKey: boolean;
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  readonly messages = signal<ToastMessage[]>([]);

  successKey(message: string): void {
    this.push('success', message, true);
  }

  success(message: string): void {
    this.push('success', message, false);
  }

  warningKey(message: string): void {
    this.push('warning', message, true);
  }

  error(message: string): void {
    this.push('error', message, !message.includes(' '));
  }

  errorKey(message: string): void {
    this.push('error', message, true);
  }

  dismiss(id: string): void {
    this.messages.update((messages) => messages.filter((message) => message.id !== id));
  }

  private push(kind: ToastKind, message: string, isTranslationKey: boolean): void {
    const id = crypto.randomUUID();
    this.messages.update((messages) => [
      ...messages,
      {
        id,
        kind,
        message,
        isTranslationKey
      }
    ]);

    setTimeout(() => {
      this.dismiss(id);
    }, 5000);
  }
}

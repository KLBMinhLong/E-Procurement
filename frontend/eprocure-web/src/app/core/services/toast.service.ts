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

  warningKey(message: string): void {
    this.push('warning', message, true);
  }

  error(message: string): void {
    this.push('error', message, !message.includes(' '));
  }

  dismiss(id: string): void {
    this.messages.update((messages) => messages.filter((message) => message.id !== id));
  }

  private push(kind: ToastKind, message: string, isTranslationKey: boolean): void {
    this.messages.update((messages) => [
      ...messages,
      {
        id: crypto.randomUUID(),
        kind,
        message,
        isTranslationKey
      }
    ]);
  }
}

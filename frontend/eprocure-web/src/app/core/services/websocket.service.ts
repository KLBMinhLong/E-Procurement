import { inject, Injectable, signal } from '@angular/core';
import { Client, IMessage } from '@stomp/stompjs';
import { WS_BASE_URL } from '../http/api-tokens';

@Injectable({ providedIn: 'root' })
export class WebsocketService {
  private readonly wsBaseUrl = inject(WS_BASE_URL);
  private client: Client | null = null;

  readonly isConnected = signal(false);

  connect(onMessage: (message: IMessage) => void): void {
    if (this.client?.active) {
      return;
    }

    this.client = new Client({
      brokerURL: this.wsBaseUrl,
      reconnectDelay: 5000,
      debug: () => undefined,
      onConnect: () => {
        this.isConnected.set(true);
        this.client?.subscribe('/user/queue/notifications', onMessage);
      },
      onDisconnect: () => this.isConnected.set(false),
      onStompError: () => this.isConnected.set(false),
      onWebSocketClose: () => this.isConnected.set(false)
    });

    this.client.activate();
  }

  disconnect(): void {
    this.client?.deactivate();
    this.client = null;
    this.isConnected.set(false);
  }
}

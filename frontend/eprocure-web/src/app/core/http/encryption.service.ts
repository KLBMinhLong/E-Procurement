import { HttpBackend, HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ApiResponse } from '../models/api-response.model';
import { EncryptedRequest, PublicKeyResponse } from '../models/user-context.model';
import { API_BASE_URL } from './api-tokens';

const RSA_OAEP_ALGORITHM = 'RSA-OAEP';
const RSA_HASH = 'SHA-256';
const AES_GCM_ALGORITHM = 'AES-GCM';
const AES_KEY_LENGTH = 256;
const AES_IV_LENGTH = 12;

@Injectable({ providedIn: 'root' })
export class EncryptionService {
  private readonly rawHttp = new HttpClient(inject(HttpBackend));
  private readonly baseUrl = inject(API_BASE_URL);
  private publicKeyPromise: Promise<PublicKeyResponse> | null = null;

  encryptBody(body: unknown): Promise<EncryptedRequest> {
    return this.loadPublicKey()
      .then((publicKeyInfo) => this.encryptWithPublicKey(body, publicKeyInfo))
      .catch((error) => {
        throw error;
      });
  }

  loadPublicKey(): Promise<PublicKeyResponse> {
    this.publicKeyPromise ??= firstValueFrom(
      this.rawHttp.get<ApiResponse<PublicKeyResponse>>(`${this.baseUrl}/auth/public-key`)
    )
      .then((response) => response.data)
      .catch((error: unknown) => {
        this.clearPublicKeyCache();
        throw error;
      });

    return this.publicKeyPromise;
  }

  clearPublicKeyCache(): void {
    this.publicKeyPromise = null;
  }

  private async encryptWithPublicKey(body: unknown, publicKeyInfo: PublicKeyResponse): Promise<EncryptedRequest> {
    const publicKey = await this.importPublicKey(publicKeyInfo.publicKey);
    const aesKey = await crypto.subtle.generateKey(
      { name: AES_GCM_ALGORITHM, length: AES_KEY_LENGTH },
      true,
      ['encrypt']
    );
    const iv = crypto.getRandomValues(new Uint8Array(AES_IV_LENGTH));
    const encodedBody = new TextEncoder().encode(JSON.stringify(body));
    const encryptedPayload = await crypto.subtle.encrypt(
      { name: AES_GCM_ALGORITHM, iv },
      aesKey,
      encodedBody
    );
    const rawAesKey = await crypto.subtle.exportKey('raw', aesKey);
    const encryptedAesKey = await crypto.subtle.encrypt(
      { name: RSA_OAEP_ALGORITHM },
      publicKey,
      rawAesKey
    );

    return {
      encryptedPayload: this.toBase64(encryptedPayload),
      encryptedAesKey: this.toBase64(encryptedAesKey),
      iv: this.toBase64(iv),
      keyVersion: publicKeyInfo.keyVersion
    };
  }

  private importPublicKey(pem: string): Promise<CryptoKey> {
    // 1. Remove the headers and footers clean and safely
    const cleanHeaders = pem
      .replace(/-----BEGIN[^-]*-----/g, '')
      .replace(/-----END[^-]*-----/g, '');

    // 2. Strip all backslashes and all whitespaces (including newlines)
    const sanitized = cleanHeaders
      .replace(/\\/g, '')
      .replace(/\s/g, '');

    const der = this.fromBase64(sanitized);

    return crypto.subtle.importKey(
      'spki',
      der,
      {
        name: RSA_OAEP_ALGORITHM,
        hash: RSA_HASH
      },
      false,
      ['encrypt']
    );
  }

  private toBase64(value: ArrayBuffer | Uint8Array): string {
    const bytes = value instanceof Uint8Array ? value : new Uint8Array(value);
    let binary = '';

    for (let index = 0; index < bytes.length; index += 0x8000) {
      binary += String.fromCharCode(...bytes.subarray(index, index + 0x8000));
    }

    return btoa(binary);
  }

  private fromBase64(value: string): ArrayBuffer {
    const binary = atob(value);
    const bytes = new Uint8Array(binary.length);

    for (let index = 0; index < binary.length; index += 1) {
      bytes[index] = binary.charCodeAt(index);
    }

    return bytes.buffer;
  }
}

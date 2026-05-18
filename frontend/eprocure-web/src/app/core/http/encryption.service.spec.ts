import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_BASE_URL } from './api-tokens';
import { EncryptionService } from './encryption.service';

const RSA_OAEP_KEY = {
  name: 'RSA-OAEP',
  modulusLength: 2048,
  publicExponent: new Uint8Array([1, 0, 1]),
  hash: 'SHA-256'
} as const;

describe('EncryptionService', () => {
  let service: EncryptionService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: API_BASE_URL, useValue: '/api/v1' }
      ]
    });

    service = TestBed.inject(EncryptionService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should encrypt a request body using the backend public key contract', async () => {
    const keyPair = await crypto.subtle.generateKey(RSA_OAEP_KEY, true, ['encrypt', 'decrypt']);
    const publicKeyPem = await exportPublicKeyPem(keyPair.publicKey);
    const payload = { username: 'alice', password: 'correct horse battery staple' };
    const encryptedBodyPromise = service.encryptBody(payload);

    const publicKeyRequest = httpTestingController.expectOne('/api/v1/auth/public-key');
    expect(publicKeyRequest.request.method).toBe('GET');
    publicKeyRequest.flush({
      success: true,
      code: 'IAM_000',
      message: null,
      data: {
        publicKey: publicKeyPem,
        keyVersion: 'v-test',
        algorithm: 'RSA/ECB/OAEPWithSHA-256AndMGF1Padding'
      },
      meta: null,
      timestamp: new Date().toISOString(),
      requestId: crypto.randomUUID()
    });

    const encryptedBody = await encryptedBodyPromise;
    const rawAesKey = await crypto.subtle.decrypt(
      { name: 'RSA-OAEP' },
      keyPair.privateKey,
      fromBase64(encryptedBody.encryptedAesKey)
    );
    const aesKey = await crypto.subtle.importKey('raw', rawAesKey, 'AES-GCM', false, ['decrypt']);
    const plainPayload = await crypto.subtle.decrypt(
      { name: 'AES-GCM', iv: new Uint8Array(fromBase64(encryptedBody.iv)) },
      aesKey,
      fromBase64(encryptedBody.encryptedPayload)
    );

    expect(encryptedBody.keyVersion).toBe('v-test');
    expect(new TextDecoder().decode(plainPayload)).toBe(JSON.stringify(payload));
  });
});

async function exportPublicKeyPem(publicKey: CryptoKey): Promise<string> {
  const spki = await crypto.subtle.exportKey('spki', publicKey);
  const base64 = toBase64(spki);
  const lines = base64.match(/.{1,64}/g) ?? [];

  return `-----BEGIN PUBLIC KEY-----\n${lines.join('\n')}\n-----END PUBLIC KEY-----`;
}

function toBase64(value: ArrayBuffer): string {
  const bytes = new Uint8Array(value);
  let binary = '';

  for (let index = 0; index < bytes.length; index += 0x8000) {
    binary += String.fromCharCode(...bytes.subarray(index, index + 0x8000));
  }

  return btoa(binary);
}

function fromBase64(value: string): ArrayBuffer {
  const binary = atob(value);
  const bytes = new Uint8Array(binary.length);

  for (let index = 0; index < binary.length; index += 1) {
    bytes[index] = binary.charCodeAt(index);
  }

  return bytes.buffer;
}

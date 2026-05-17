## SK-21 · Angular i18n (ngx-translate)

### Trigger
Agent cần thêm hoặc cập nhật i18n cho UI Angular.

### Inputs Required
- Danh sách ngôn ngữ (vi/en)
- Key naming convention
- defaultLanguage

### Rules
```
[R1] Không hardcode string tiếng Việt trong template — dùng | translate
[R2] Keys dùng dot-notation rõ ràng: feature.section.action
[R3] Tất cả key phải có trong vi.json và en.json
[R4] Không dùng translate.instant cho nội dung async — dùng translate.get
[R5] Truyền params qua pipe hoặc service (VD: { count: total })
[R6] Fallback: defaultLanguage phải được set
```

### Template — i18n Files
```json
// assets/i18n/vi.json
{
	"common": {
		"button": {
			"save": "Luu",
			"cancel": "Huy"
		}
	},
	"purchaseRequest": {
		"title": "De nghi mua sam"
	}
}
```

```json
// assets/i18n/en.json
{
	"common": {
		"button": {
			"save": "Save",
			"cancel": "Cancel"
		}
	},
	"purchaseRequest": {
		"title": "Purchase Request"
	}
}
```

### Template — Module Setup
```typescript
import { HttpClient } from '@angular/common/http';
import { TranslateLoader, TranslateModule } from '@ngx-translate/core';
import { TranslateHttpLoader } from '@ngx-translate/http-loader';

export function httpLoaderFactory(http: HttpClient) {
	return new TranslateHttpLoader(http, '/assets/i18n/', '.json');
}

@NgModule({
	imports: [
		TranslateModule.forRoot({
			defaultLanguage: 'vi',
			loader: {
				provide: TranslateLoader,
				useFactory: httpLoaderFactory,
				deps: [HttpClient]
			}
		})
	]
})
export class AppModule {}
```

### Template — Usage
```html
<h1>{{ 'purchaseRequest.title' | translate }}</h1>
<button>{{ 'common.button.save' | translate }}</button>
<span>{{ 'common.items' | translate:{ count: total } }}</span>
```

```typescript
this.translateService.get('common.button.save').subscribe((label) => {
	this.saveLabel = label;
});
```

### Checklist
```
[ ] Tất cả string UI dùng translate pipe hoặc service
[ ] Key có đủ trong vi.json và en.json
[ ] defaultLanguage được cấu hình
[ ] Không dùng translate.instant cho dữ liệu async
```

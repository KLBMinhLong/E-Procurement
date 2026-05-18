import { Directive, effect, inject, input, TemplateRef, ViewContainerRef } from '@angular/core';
import { PermissionService } from './permission.service';

@Directive({
  selector: '[epHasPermission]',
  standalone: true
})
export class HasPermissionDirective {
  readonly epHasPermission = input.required<string | string[]>();

  private readonly templateRef = inject(TemplateRef<unknown>);
  private readonly viewContainerRef = inject(ViewContainerRef);
  private readonly permissionService = inject(PermissionService);
  private isRendered = false;

  constructor() {
    effect(() => {
      const requirement = this.epHasPermission();
      const permissions = Array.isArray(requirement) ? requirement : [requirement];
      const canRender = this.permissionService.hasAnyPermission(permissions);

      if (canRender && !this.isRendered) {
        this.viewContainerRef.createEmbeddedView(this.templateRef);
        this.isRendered = true;
      } else if (!canRender && this.isRendered) {
        this.viewContainerRef.clear();
        this.isRendered = false;
      }
    });
  }
}

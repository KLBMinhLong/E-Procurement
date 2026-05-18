import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'ep-form-field',
  standalone: true,
  templateUrl: './ep-form-field.component.html',
  styleUrl: './ep-form-field.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class EpFormFieldComponent {
  readonly label = input.required<string>();
  readonly error = input<string | null>(null);
}

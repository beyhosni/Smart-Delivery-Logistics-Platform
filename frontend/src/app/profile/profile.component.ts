import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-profile',
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss']
})
export class ProfileComponent implements OnInit {
  profileForm: FormGroup;
  passwordForm: FormGroup;
  submittedProfile = false;
  submittedPassword = false;
  loading = false;
  error = '';
  success = '';
  user: any;

  constructor(
    private formBuilder: FormBuilder,
    private authService: AuthService
  ) { }

  ngOnInit(): void {
    this.user = this.authService.currentUserValue;
    this.initForms();
  }

  initForms(): void {
    this.profileForm = this.formBuilder.group({
      firstName: [this.user.firstName, Validators.required],
      lastName: [this.user.lastName, Validators.required],
      email: [this.user.email, [Validators.required, Validators.email]],
      phone: [this.user.phone, [Validators.required, Validators.pattern('^[0-9]{10}$')]],
      address: [this.user.address, Validators.required]
    });

    this.passwordForm = this.formBuilder.group({
      currentPassword: ['', Validators.required],
      newPassword: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', Validators.required]
    }, {
      validator: this.mustMatch('newPassword', 'confirmPassword')
    });
  }

  // Accesseur pratique pour accéder facilement aux champs du formulaire
  get fProfile() { return this.profileForm.controls; }
  get fPassword() { return this.passwordForm.controls; }

  // Validateur personnalisé pour vérifier que les mots de passe correspondent
  mustMatch(controlName: string, matchingControlName: string) {
    return (formGroup: FormGroup) => {
      const control = formGroup.controls[controlName];
      const matchingControl = formGroup.controls[matchingControlName];

      if (matchingControl.errors && !matchingControl.errors['mustMatch']) {
        return;
      }

      if (control.value !== matchingControl.value) {
        matchingControl.setErrors({ mustMatch: true });
      } else {
        matchingControl.setErrors(null);
      }
    };
  }

  onSubmitProfile(): void {
    this.submittedProfile = true;
    this.error = '';
    this.success = '';

    // Arrêter ici si le formulaire est invalide
    if (this.profileForm.invalid) {
      return;
    }

    this.loading = true;

    // Simulation d'appel à un service de mise à jour du profil
    setTimeout(() => {
      this.user = {
        ...this.user,
        ...this.profileForm.value
      };

      this.success = 'Profil mis à jour avec succès';
      this.loading = false;
    }, 1000);
  }

  onSubmitPassword(): void {
    this.submittedPassword = true;
    this.error = '';
    this.success = '';

    // Arrêter ici si le formulaire est invalide
    if (this.passwordForm.invalid) {
      return;
    }

    this.loading = true;

    // Simulation d'appel à un service de changement de mot de passe
    setTimeout(() => {
      this.success = 'Mot de passe changé avec succès';
      this.passwordForm.reset();
      this.submittedPassword = false;
      this.loading = false;
    }, 1000);
  }
}

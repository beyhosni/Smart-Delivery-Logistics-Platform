import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { DeliveryService } from '../../services/delivery.service';

@Component({
  selector: 'app-create-delivery',
  templateUrl: './create-delivery.component.html',
  styleUrls: ['./create-delivery.component.scss']
})
export class CreateDeliveryComponent implements OnInit {
  deliveryForm: FormGroup;
  submitted = false;
  loading = false;
  error = '';

  constructor(
    private formBuilder: FormBuilder,
    private router: Router,
    private deliveryService: DeliveryService
  ) { }

  ngOnInit(): void {
    this.deliveryForm = this.formBuilder.group({
      recipientName: ['', Validators.required],
      recipientPhone: ['', [Validators.required, Validators.pattern('^[0-9]{10}$')]],
      recipientEmail: ['', [Validators.required, Validators.email]],
      pickupStreet: ['', Validators.required],
      pickupCity: ['', Validators.required],
      pickupPostalCode: ['', [Validators.required, Validators.pattern('^[0-9]{5}$')]],
      pickupCountry: ['France', Validators.required],
      deliveryStreet: ['', Validators.required],
      deliveryCity: ['', Validators.required],
      deliveryPostalCode: ['', [Validators.required, Validators.pattern('^[0-9]{5}$')]],
      deliveryCountry: ['France', Validators.required],
      weight: ['', [Validators.required, Validators.min(0.1)]],
      length: ['', [Validators.required, Validators.min(1)]],
      width: ['', [Validators.required, Validators.min(1)]],
      height: ['', [Validators.required, Validators.min(1)]],
      description: [''],
      fragile: [false],
      specialInstructions: [''],
      priority: ['NORMAL', Validators.required],
      requestedDeliveryTime: ['']
    });
  }

  // Accesseur pratique pour accéder facilement aux champs du formulaire
  get f() { return this.deliveryForm.controls; }

  onSubmit(): void {
    this.submitted = true;

    // Arrêter ici si le formulaire est invalide
    if (this.deliveryForm.invalid) {
      return;
    }

    this.loading = true;

    const delivery = {
      recipientName: this.f['recipientName'].value,
      recipientPhone: this.f['recipientPhone'].value,
      recipientEmail: this.f['recipientEmail'].value,
      pickupAddress: {
        street: this.f['pickupStreet'].value,
        city: this.f['pickupCity'].value,
        postalCode: this.f['pickupPostalCode'].value,
        country: this.f['pickupCountry'].value,
        coordinates: {
          latitude: 48.8566, // Par défaut, dans un vrai projet, on utiliserait un service de géocodage
          longitude: 2.3522
        }
      },
      deliveryAddress: {
        street: this.f['deliveryStreet'].value,
        city: this.f['deliveryCity'].value,
        postalCode: this.f['deliveryPostalCode'].value,
        country: this.f['deliveryCountry'].value,
        coordinates: {
          latitude: 48.8584, // Par défaut, dans un vrai projet, on utiliserait un service de géocodage
          longitude: 2.2945
        }
      },
      packageDetails: {
        weight: this.f['weight'].value,
        dimensions: {
          length: this.f['length'].value,
          width: this.f['width'].value,
          height: this.f['height'].value
        },
        description: this.f['description'].value,
        fragile: this.f['fragile'].value,
        specialInstructions: this.f['specialInstructions'].value
      },
      priority: this.f['priority'].value,
      requestedDeliveryTime: this.f['requestedDeliveryTime'].value ? new Date(this.f['requestedDeliveryTime'].value) : null
    };

    this.deliveryService.createDelivery(delivery)
      .subscribe({
        next: (response) => {
          this.router.navigate(['/deliveries', response.id]);
        },
        error: (err) => {
          this.error = err.message || 'Une erreur est survenue lors de la création de la livraison';
          this.loading = false;
        }
      });
  }

  cancel(): void {
    this.router.navigate(['/deliveries']);
  }
}

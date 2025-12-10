import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { DeliveryService } from '../../services/delivery.service';

@Component({
  selector: 'app-delivery-detail',
  templateUrl: './delivery-detail.component.html',
  styleUrls: ['./delivery-detail.component.scss']
})
export class DeliveryDetailComponent implements OnInit {
  deliveryId: string;
  delivery: any;
  loading = true;
  error = '';
  currentLocation: any = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private deliveryService: DeliveryService
  ) { }

  ngOnInit(): void {
    this.deliveryId = this.route.snapshot.paramMap.get('id') || '';
    if (this.deliveryId) {
      this.loadDeliveryDetails();
    } else {
      this.error = 'ID de livraison invalide';
      this.loading = false;
    }
  }

  loadDeliveryDetails(): void {
    this.loading = true;

    this.deliveryService.getDeliveryById(this.deliveryId).subscribe({
      next: (data) => {
        this.delivery = data;

        // Si la livraison est en transit, charger la position actuelle
        if (this.delivery.status === 'IN_TRANSIT') {
          this.loadCurrentLocation();
        }

        this.loading = false;
      },
      error: (err) => {
        this.error = 'Erreur lors du chargement des détails de la livraison';
        this.loading = false;
        console.error(err);
      }
    });
  }

  loadCurrentLocation(): void {
    // Simulation d'appel à un service de suivi
    // Dans un vrai projet, ce serait un appel à un endpoint dédié
    setTimeout(() => {
      this.currentLocation = {
        lat: 48.8570,
        lng: 2.3400
      };
    }, 1000);
  }

  getStatusBadgeClass(status: string): string {
    switch (status) {
      case 'CREATED':
        return 'bg-secondary';
      case 'ASSIGNED':
        return 'bg-info';
      case 'IN_TRANSIT':
        return 'bg-primary';
      case 'DELIVERED':
        return 'bg-success';
      case 'CANCELLED':
        return 'bg-danger';
      default:
        return 'bg-secondary';
    }
  }

  getStatusText(status: string): string {
    switch (status) {
      case 'CREATED':
        return 'Créée';
      case 'ASSIGNED':
        return 'Assignée';
      case 'IN_TRANSIT':
        return 'En transit';
      case 'DELIVERED':
        return 'Livrée';
      case 'CANCELLED':
        return 'Annulée';
      default:
        return status;
    }
  }

  getPriorityBadgeClass(priority: string): string {
    switch (priority) {
      case 'LOW':
        return 'bg-success';
      case 'NORMAL':
        return 'bg-info';
      case 'HIGH':
        return 'bg-warning';
      case 'URGENT':
        return 'bg-danger';
      default:
        return 'bg-secondary';
    }
  }

  getPriorityText(priority: string): string {
    switch (priority) {
      case 'LOW':
        return 'Basse';
      case 'NORMAL':
        return 'Normale';
      case 'HIGH':
        return 'Haute';
      case 'URGENT':
        return 'Urgente';
      default:
        return priority;
    }
  }

  goBack(): void {
    this.router.navigate(['/deliveries']);
  }

  trackDelivery(): void {
    this.router.navigate(['/tracking', this.deliveryId]);
  }

  editDelivery(): void {
    this.router.navigate(['/deliveries', this.deliveryId, 'edit']);
  }

  cancelDelivery(): void {
    if (confirm('Êtes-vous sûr de vouloir annuler cette livraison ?')) {
      this.deliveryService.cancelDelivery(this.deliveryId, 'Annulation par le client')
        .subscribe({
          next: () => {
            this.loadDeliveryDetails(); // Recharger les détails après l'annulation
          },
          error: (err) => {
            alert('Erreur lors de l'annulation de la livraison: ' + (err.message || 'Erreur inconnue'));
          }
        });
    }
  }
}

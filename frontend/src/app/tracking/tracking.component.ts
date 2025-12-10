import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { DeliveryService } from '../services/delivery.service';

@Component({
  selector: 'app-tracking',
  templateUrl: './tracking.component.html',
  styleUrls: ['./tracking.component.scss']
})
export class TrackingComponent implements OnInit {
  deliveryId: string;
  delivery: any;
  trackingEvents: any[] = [];
  loading = true;
  error = '';
  mapCenter = { lat: 48.8566, lng: 2.3522 }; // Paris par défaut
  mapZoom = 12;
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
        this.loadTrackingEvents();
      },
      error: (err) => {
        this.error = 'Erreur lors du chargement des détails de la livraison';
        this.loading = false;
        console.error(err);
      }
    });
  }

  loadTrackingEvents(): void {
    // Simulation d'appel à un service de suivi
    // Dans un vrai projet, ce serait un appel à un service dédié
    setTimeout(() => {
      this.trackingEvents = [
        {
          id: 1,
          eventType: 'CREATED',
          timestamp: new Date(this.delivery.createdAt),
          location: this.delivery.pickupAddress,
          description: 'Livraison créée'
        },
        {
          id: 2,
          eventType: 'ASSIGNED',
          timestamp: new Date(this.delivery.createdAt),
          timestamp.setHours(this.delivery.createdAt.getHours() + 1),
          location: this.delivery.pickupAddress,
          description: 'Livraison assignée à un livreur',
          courierName: 'Jean Dupont'
        },
        {
          id: 3,
          eventType: 'PICKED_UP',
          timestamp: new Date(this.delivery.createdAt),
          timestamp.setHours(this.delivery.createdAt.getHours() + 2),
          location: this.delivery.pickupAddress,
          description: 'Colis ramassé par le livreur',
          courierName: 'Jean Dupont'
        }
      ];

      // Si la livraison est en transit ou livrée, ajouter plus d'événements
      if (this.delivery.status === 'IN_TRANSIT' || this.delivery.status === 'DELIVERED') {
        this.trackingEvents.push({
          id: 4,
          eventType: 'IN_TRANSIT',
          timestamp: new Date(this.delivery.createdAt),
          timestamp.setHours(this.delivery.createdAt.getHours() + 3),
          location: {
            address: 'En route vers la destination',
            coordinates: {
              latitude: 48.8570,
              longitude: 2.3400
            }
          },
          description: 'En route vers la destination',
          courierName: 'Jean Dupont'
        });

        // Mettre à jour la position actuelle pour la carte
        this.currentLocation = {
          lat: 48.8570,
          lng: 2.3400
        };
        this.mapCenter = this.currentLocation;
      }

      if (this.delivery.status === 'DELIVERED') {
        this.trackingEvents.push({
          id: 5,
          eventType: 'DELIVERED',
          timestamp: new Date(this.delivery.createdAt),
          timestamp.setHours(this.delivery.createdAt.getHours() + 5),
          location: this.delivery.deliveryAddress,
          description: 'Livraison effectuée avec succès',
          courierName: 'Jean Dupont',
          recipientName: this.delivery.recipientName
        });

        // Mettre à jour la position actuelle pour la carte
        this.currentLocation = {
          lat: this.delivery.deliveryAddress.coordinates.latitude,
          lng: this.delivery.deliveryAddress.coordinates.longitude
        };
        this.mapCenter = this.currentLocation;
      }

      this.loading = false;
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

  getEventIcon(eventType: string): string {
    switch (eventType) {
      case 'CREATED':
        return 'fas fa-plus-circle';
      case 'ASSIGNED':
        return 'fas fa-user-check';
      case 'PICKED_UP':
        return 'fas fa-box';
      case 'IN_TRANSIT':
        return 'fas fa-truck';
      case 'DELIVERED':
        return 'fas fa-check-circle';
      default:
        return 'fas fa-info-circle';
    }
  }

  getEventIconClass(eventType: string): string {
    switch (eventType) {
      case 'CREATED':
        return 'text-secondary';
      case 'ASSIGNED':
        return 'text-info';
      case 'PICKED_UP':
        return 'text-warning';
      case 'IN_TRANSIT':
        return 'text-primary';
      case 'DELIVERED':
        return 'text-success';
      default:
        return 'text-secondary';
    }
  }

  goBack(): void {
    this.router.navigate(['/deliveries']);
  }
}

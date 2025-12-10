import { Component, OnInit } from '@angular/core';
import { DeliveryService } from '../services/delivery.service';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit {
  user: any;
  stats: any = {
    totalDeliveries: 0,
    pendingDeliveries: 0,
    inTransitDeliveries: 0,
    completedDeliveries: 0
  };
  recentDeliveries: any[] = [];
  loading = true;
  error = '';

  constructor(
    private authService: AuthService,
    private deliveryService: DeliveryService
  ) { }

  ngOnInit(): void {
    this.user = this.authService.currentUserValue;
    this.loadDashboardData();
  }

  loadDashboardData(): void {
    this.loading = true;

    // Récupérer les statistiques des livraisons
    this.deliveryService.getDeliveryStats().subscribe({
      next: (stats) => {
        this.stats = stats;
      },
      error: (err) => {
        this.error = 'Erreur lors du chargement des statistiques';
        console.error(err);
      }
    });

    // Récupérer les livraisons récentes
    this.deliveryService.getRecentDeliveries(5).subscribe({
      next: (deliveries) => {
        this.recentDeliveries = deliveries;
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Erreur lors du chargement des livraisons récentes';
        this.loading = false;
        console.error(err);
      }
    });
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
}

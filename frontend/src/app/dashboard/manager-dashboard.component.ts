import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { DeliveryService } from '../services/delivery.service';
import { CourierService } from '../services/courier.service';
import { WebSocketService } from '../services/websocket.service';

@Component({
  selector: 'app-manager-dashboard',
  templateUrl: './manager-dashboard.component.html',
  styleUrls: ['./manager-dashboard.component.scss']
})
export class ManagerDashboardComponent implements OnInit {
  deliveries: any[] = [];
  couriers: any[] = [];
  stats = {
    totalDeliveries: 0,
    pendingDeliveries: 0,
    inTransitDeliveries: 0,
    completedDeliveries: 0,
    totalCouriers: 0,
    availableCouriers: 0,
    busyCouriers: 0,
    offlineCouriers: 0
  };
  loading = true;
  error = '';

  private subscriptions: any[] = [];
  private wsUrl = 'ws://localhost:8083/ws/manager';

  constructor(
    private router: Router,
    private deliveryService: DeliveryService,
    private courierService: CourierService,
    private webSocketService: WebSocketService
  ) {}

  ngOnInit(): void {
    this.loadDashboardData();
    this.setupWebSocketConnection();
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
    this.webSocketService.disconnect();
  }

  /**
   * Charge les données du tableau de bord
   */
  private loadDashboardData(): void {
    this.loading = true;

    // Charger les livraisons
    this.deliveryService.getAllDeliveries().subscribe({
      next: (data) => {
        this.deliveries = data;
        this.calculateDeliveryStats();
      },
      error: (err) => {
        this.error = 'Erreur lors du chargement des livraisons';
        this.loading = false;
        console.error(err);
      }
    });

    // Charger les livreurs
    this.courierService.getAllCouriers().subscribe({
      next: (data) => {
        this.couriers = data;
        this.calculateCourierStats();
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Erreur lors du chargement des livreurs';
        this.loading = false;
        console.error(err);
      }
    });
  }

  /**
   * Configure la connexion WebSocket pour les mises à jour en temps réel
   */
  private setupWebSocketConnection(): void {
    // Se connecter au WebSocket
    this.webSocketService.connect(this.wsUrl);

    // S'abonner aux mises à jour de livraison
    const deliverySubscription = this.webSocketService
      .subscribeToTopic('manager/deliveries')
      .subscribe({
        next: (data) => {
          this.handleDeliveryUpdate(data);
        },
        error: (error) => {
          console.error('Error receiving delivery updates:', error);
        }
      });

    // S'abonner aux mises à jour de livreur
    const courierSubscription = this.webSocketService
      .subscribeToTopic('manager/couriers')
      .subscribe({
        next: (data) => {
          this.handleCourierUpdate(data);
        },
        error: (error) => {
          console.error('Error receiving courier updates:', error);
        }
      });

    this.subscriptions.push(deliverySubscription, courierSubscription);
  }

  /**
   * Gère les mises à jour de livraison
   * @param data Données de mise à jour
   */
  private handleDeliveryUpdate(data: any): void {
    // Mettre à jour la livraison correspondante
    const index = this.deliveries.findIndex(d => d.id === data.id);

    if (index !== -1) {
      this.deliveries[index] = { ...this.deliveries[index], ...data };
    } else {
      // Si c'est une nouvelle livraison, l'ajouter à la liste
      this.deliveries.unshift(data);
    }

    // Recalculer les statistiques
    this.calculateDeliveryStats();
  }

  /**
   * Gère les mises à jour de livreur
   * @param data Données de mise à jour
   */
  private handleCourierUpdate(data: any): void {
    // Mettre à jour le livreur correspondant
    const index = this.couriers.findIndex(c => c.id === data.id);

    if (index !== -1) {
      this.couriers[index] = { ...this.couriers[index], ...data };
    } else {
      // Si c'est un nouveau livreur, l'ajouter à la liste
      this.couriers.unshift(data);
    }

    // Recalculer les statistiques
    this.calculateCourierStats();
  }

  /**
   * Calcule les statistiques des livraisons
   */
  private calculateDeliveryStats(): void {
    this.stats.totalDeliveries = this.deliveries.length;
    this.stats.pendingDeliveries = this.deliveries.filter(d => d.status === 'CREATED' || d.status === 'ASSIGNED').length;
    this.stats.inTransitDeliveries = this.deliveries.filter(d => d.status === 'PICKED_UP' || d.status === 'IN_TRANSIT').length;
    this.stats.completedDeliveries = this.deliveries.filter(d => d.status === 'DELIVERED').length;
  }

  /**
   * Calcule les statistiques des livreurs
   */
  private calculateCourierStats(): void {
    this.stats.totalCouriers = this.couriers.length;
    this.stats.availableCouriers = this.couriers.filter(c => c.status === 'AVAILABLE').length;
    this.stats.busyCouriers = this.couriers.filter(c => c.status === 'BUSY').length;
    this.stats.offlineCouriers = this.couriers.filter(c => c.status === 'OFFLINE').length;
  }

  /**
   * Navigue vers les détails d'une livraison
   * @param deliveryId ID de la livraison
   */
  viewDeliveryDetails(deliveryId: string): void {
    this.router.navigate(['/tracking', deliveryId]);
  }

  /**
   * Navigue vers les détails d'un livreur
   * @param courierId ID du livreur
   */
  viewCourierDetails(courierId: string): void {
    this.router.navigate(['/courier', courierId]);
  }

  /**
   * Crée une nouvelle livraison
   */
  createDelivery(): void {
    this.router.navigate(['/deliveries/create']);
  }

  /**
   * Affiche la liste des livreurs
   */
  viewCouriers(): void {
    this.router.navigate(['/couriers']);
  }

  /**
   * Affiche la liste des livraisons
   */
  viewDeliveries(): void {
    this.router.navigate(['/deliveries']);
  }

  /**
   * Formate le statut pour l'affichage
   * @param status Statut à formater
   */
  getStatusBadgeClass(status: string): string {
    switch (status) {
      case 'AVAILABLE':
        return 'bg-success';
      case 'BUSY':
        return 'bg-warning';
      case 'OFFLINE':
        return 'bg-danger';
      default:
        return 'bg-secondary';
    }
  }

  /**
   * Formate le statut de livraison pour l'affichage
   * @param status Statut à formater
   */
  getDeliveryStatusBadgeClass(status: string): string {
    switch (status) {
      case 'CREATED':
        return 'bg-secondary';
      case 'ASSIGNED':
        return 'bg-info';
      case 'PICKED_UP':
        return 'bg-warning';
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
}

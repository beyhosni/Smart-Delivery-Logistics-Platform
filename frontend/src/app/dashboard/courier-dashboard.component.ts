import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { WebSocketService } from '../services/websocket.service';
import { CourierService } from '../services/courier.service';
import { DeliveryService } from '../services/delivery.service';

@Component({
  selector: 'app-courier-dashboard',
  templateUrl: './courier-dashboard.component.html',
  styleUrls: ['./courier-dashboard.component.scss']
})
export class CourierDashboardComponent implements OnInit {
  courierId: string;
  courier: any = null;
  activeDeliveries: any[] = [];
  completedDeliveries: any[] = [];
  loading = true;
  error = '';
  stats = {
    totalDeliveries: 0,
    completedToday: 0,
    inProgress: 0,
    averageDeliveryTime: 0,
    earnings: 0
  };

  private subscriptions: any[] = [];
  private wsUrl = 'ws://localhost:8083/ws/courier';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private webSocketService: WebSocketService,
    private courierService: CourierService,
    private deliveryService: DeliveryService
  ) {}

  ngOnInit(): void {
    this.courierId = this.route.snapshot.paramMap.get('id') || '';
    if (this.courierId) {
      this.loadCourierData();
      this.setupWebSocketConnection();
    } else {
      this.error = 'ID de livreur invalide';
      this.loading = false;
    }
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
    this.webSocketService.disconnect();
  }

  /**
   * Charge les données du livreur
   */
  private loadCourierData(): void {
    this.loading = true;

    // Charger les informations du livreur
    this.courierService.getCourierById(this.courierId).subscribe({
      next: (data) => {
        this.courier = data;
        this.loadActiveDeliveries();
        this.loadCompletedDeliveries();
        this.calculateStats();
      },
      error: (err) => {
        this.error = 'Erreur lors du chargement des informations du livreur';
        this.loading = false;
        console.error(err);
      }
    });
  }

  /**
   * Charge les livraisons actives du livreur
   */
  private loadActiveDeliveries(): void {
    this.deliveryService.getDeliveriesByCourierId(this.courierId, 'ACTIVE').subscribe({
      next: (data) => {
        this.activeDeliveries = data;
        this.stats.inProgress = data.length;
      },
      error: (err) => {
        console.error('Error loading active deliveries:', err);
      }
    });
  }

  /**
   * Charge les livraisons complétées du livreur
   */
  private loadCompletedDeliveries(): void {
    this.deliveryService.getDeliveriesByCourierId(this.courierId, 'COMPLETED').subscribe({
      next: (data) => {
        this.completedDeliveries = data.slice(0, 10); // Limiter aux 10 plus récentes
        this.stats.totalDeliveries = data.length;
        this.calculateCompletedToday(data);
        this.calculateAverageDeliveryTime(data);
      },
      error: (err) => {
        console.error('Error loading completed deliveries:', err);
      }
    });
  }

  /**
   * Configure la connexion WebSocket pour les mises à jour en temps réel
   */
  private setupWebSocketConnection(): void {
    // Se connecter au WebSocket
    this.webSocketService.connect(`${this.wsUrl}/${this.courierId}`);

    // S'abonner aux mises à jour de livraison
    const deliverySubscription = this.webSocketService
      .subscribeToTopic(`courier/${this.courierId}/deliveries`)
      .subscribe({
        next: (data) => {
          this.handleDeliveryUpdate(data);
        },
        error: (error) => {
          console.error('Error receiving delivery updates:', error);
        }
      });

    // S'abonner aux mises à jour de position
    const locationSubscription = this.webSocketService
      .subscribeToTopic(`courier/${this.courierId}/location`)
      .subscribe({
        next: (data) => {
          this.handleLocationUpdate(data);
        },
        error: (error) => {
          console.error('Error receiving location updates:', error);
        }
      });

    this.subscriptions.push(deliverySubscription, locationSubscription);
  }

  /**
   * Gère les mises à jour de livraison
   * @param data Données de mise à jour
   */
  private handleDeliveryUpdate(data: any): void {
    // Mettre à jour la livraison correspondante dans la liste des livraisons actives
    const index = this.activeDeliveries.findIndex(d => d.id === data.id);

    if (index !== -1) {
      this.activeDeliveries[index] = { ...this.activeDeliveries[index], ...data };

      // Si la livraison est maintenant complétée, la déplacer vers la liste des livraisons complétées
      if (data.status === 'DELIVERED') {
        this.activeDeliveries.splice(index, 1);
        this.completedDeliveries.unshift(data);
        this.stats.completedToday++;
        this.stats.inProgress--;
      }
    } else if (data.status === 'ASSIGNED' || data.status === 'IN_TRANSIT') {
      // Si c'est une nouvelle livraison, l'ajouter à la liste des livraisons actives
      if (!this.activeDeliveries.find(d => d.id === data.id)) {
        this.activeDeliveries.push(data);
        this.stats.inProgress++;
      }
    }
  }

  /**
   * Gère les mises à jour de position
   * @param data Données de position
   */
  private handleLocationUpdate(data: any): void {
    if (this.courier) {
      this.courier.currentLocation = {
        latitude: data.latitude,
        longitude: data.longitude,
        address: data.address
      };
    }
  }

  /**
   * Calcule le nombre de livraisons complétées aujourd'hui
   * @param deliveries Liste des livraisons complétées
   */
  private calculateCompletedToday(deliveries: any[]): void {
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    this.stats.completedToday = deliveries.filter(d => {
      const deliveryDate = new Date(d.completedAt);
      return deliveryDate >= today;
    }).length;
  }

  /**
   * Calcule le temps moyen de livraison
   * @param deliveries Liste des livraisons complétées
   */
  private calculateAverageDeliveryTime(deliveries: any[]): void {
    if (deliveries.length === 0) {
      this.stats.averageDeliveryTime = 0;
      return;
    }

    const totalMinutes = deliveries.reduce((sum, d) => {
      const startTime = new Date(d.createdAt);
      const endTime = new Date(d.completedAt);
      return sum + (endTime.getTime() - startTime.getTime()) / (1000 * 60); // en minutes
    }, 0);

    this.stats.averageDeliveryTime = Math.round(totalMinutes / deliveries.length);
  }

  /**
   * Calcule les statistiques générales
   */
  private calculateStats(): void {
    if (this.courier) {
      // Calculer les gains (exemple basé sur les livraisons complétées)
      this.stats.earnings = this.stats.totalDeliveries * 10; // 10€ par livraison (exemple)
    }
  }

  /**
   * Formate le temps en minutes
   * @param minutes Temps en minutes
   */
  formatMinutes(minutes: number): string {
    if (minutes < 60) {
      return `${minutes} min`;
    }

    const hours = Math.floor(minutes / 60);
    const mins = minutes % 60;
    return `${hours}h ${mins}min`;
  }

  /**
   * Navigue vers les détails d'une livraison
   * @param deliveryId ID de la livraison
   */
  viewDeliveryDetails(deliveryId: string): void {
    this.router.navigate(['/tracking', deliveryId]);
  }

  /**
   * Met à jour le statut du livreur
   * @param status Nouveau statut
   */
  updateStatus(status: string): void {
    if (this.courier) {
      this.courierService.updateCourierStatus(this.courierId, status).subscribe({
        next: () => {
          this.courier.status = status;
        },
        error: (err) => {
          console.error('Error updating courier status:', err);
        }
      });
    }
  }
}

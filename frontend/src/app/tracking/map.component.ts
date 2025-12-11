import { Component, Input, OnInit, OnDestroy } from '@angular/core';
import { WebSocketService } from '../services/websocket.service';
import { Subscription } from 'rxjs';

declare var ol: any; // OpenLayers

@Component({
  selector: 'app-tracking-map',
  templateUrl: './map.component.html',
  styleUrls: ['./map.component.scss']
})
export class TrackingMapComponent implements OnInit, OnDestroy {
  @Input() deliveryId: string;
  @Input() initialCenter: { lat: number; lng: number } = { lat: 48.8566, lng: 2.3522 }; // Paris par défaut
  @Input() initialZoom: number = 13;

  map: any;
  currentLocationLayer: any;
  routeLayer: any;
  pickupMarker: any;
  deliveryMarker: any;
  courierMarker: any;
  currentPosition: { lat: number; lng: number } | null = null;
  routeCoordinates: { lat: number; lng: number }[] = [];

  private subscriptions: Subscription[] = [];
  private wsUrl = 'ws://localhost:8083/ws/tracking';

  constructor(private webSocketService: WebSocketService) {}

  ngOnInit(): void {
    this.initializeMap();
    this.setupWebSocketConnection();
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
    this.webSocketService.disconnect();
  }

  /**
   * Initialise la carte OpenLayers
   */
  private initializeMap(): void {
    // Créer la carte
    this.map = new ol.Map({
      target: 'tracking-map',
      layers: [
        new ol.layer.Tile({
          source: new ol.source.OSM()
        })
      ],
      view: new ol.View({
        center: ol.proj.fromLonLat([this.initialCenter.lng, this.initialCenter.lat]),
        zoom: this.initialZoom
      })
    });

    // Créer les couches pour les marqueurs et la route
    this.currentLocationLayer = new ol.layer.Vector({
      source: new ol.source.Vector()
    });

    this.routeLayer = new ol.layer.Vector({
      source: new ol.source.Vector()
    });

    // Ajouter les couches à la carte
    this.map.addLayer(this.routeLayer);
    this.map.addLayer(this.currentLocationLayer);
  }

  /**
   * Configure la connexion WebSocket pour les mises à jour en temps réel
   */
  private setupWebSocketConnection(): void {
    // Se connecter au WebSocket
    this.webSocketService.connect(`${this.wsUrl}/${this.deliveryId}`);

    // S'abonner aux mises à jour de position
    const locationSubscription = this.webSocketService
      .subscribeToTopic(`tracking/${this.deliveryId}/location`)
      .subscribe({
        next: (data) => {
          this.updateCourierLocation(data);
        },
        error: (error) => {
          console.error('Error receiving location updates:', error);
        }
      });

    // S'abonner aux mises à jour de statut
    const statusSubscription = this.webSocketService
      .subscribeToTopic(`tracking/${this.deliveryId}/status`)
      .subscribe({
        next: (data) => {
          this.updateDeliveryStatus(data);
        },
        error: (error) => {
          console.error('Error receiving status updates:', error);
        }
      });

    // S'abonner aux mises à jour complètes
    const trackingSubscription = this.webSocketService
      .subscribeToTopic(`tracking/${this.deliveryId}`)
      .subscribe({
        next: (data) => {
          this.updateTrackingData(data);
        },
        error: (error) => {
          console.error('Error receiving tracking updates:', error);
        }
      });

    this.subscriptions.push(locationSubscription, statusSubscription, trackingSubscription);
  }

  /**
   * Met à jour la position du livreur sur la carte
   * @param location Données de position
   */
  private updateCourierLocation(location: any): void {
    this.currentPosition = {
      lat: location.latitude,
      lng: location.longitude
    };

    // Supprimer le marqueur précédent
    if (this.courierMarker) {
      this.currentLocationLayer.getSource().removeFeature(this.courierMarker);
    }

    // Créer un nouveau marqueur pour la position actuelle
    this.courierMarker = new ol.Feature({
      geometry: new ol.geom.Point(ol.proj.fromLonLat([location.longitude, location.latitude])),
      properties: {
        name: 'Position actuelle',
        type: 'courier'
      }
    });

    // Style pour le marqueur du livreur
    this.courierMarker.setStyle(new ol.style.Style({
      image: new ol.style.Icon({
        anchor: [0.5, 1],
        src: 'assets/courier-icon.png',
        scale: 0.5
      })
    }));

    // Ajouter le marqueur à la couche
    this.currentLocationLayer.getSource().addFeature(this.courierMarker);

    // Centrer la carte sur la position actuelle
    this.map.getView().animate({
      center: ol.proj.fromLonLat([location.longitude, location.latitude]),
      duration: 1000
    });
  }

  /**
   * Met à jour le statut de la livraison
   * @param status Nouveau statut
   */
  private updateDeliveryStatus(status: string): void {
    console.log('Delivery status updated:', status);
    // Ici, on pourrait mettre à jour l'interface en fonction du statut
    // Par exemple, afficher un message spécial lorsque la livraison est terminée
  }

  /**
   * Met à jour les données de suivi complètes
   * @param data Données de suivi
   */
  private updateTrackingData(data: any): void {
    // Mettre à jour la route si elle est disponible
    if (data.route && data.route.coordinates) {
      this.updateRoute(data.route.coordinates);
    }

    // Mettre à jour les marqueurs de ramassage et de livraison
    if (data.pickupLocation) {
      this.updatePickupMarker(data.pickupLocation);
    }

    if (data.deliveryLocation) {
      this.updateDeliveryMarker(data.deliveryLocation);
    }
  }

  /**
   * Met à jour la route sur la carte
   * @param coordinates Coordonnées de la route
   */
  private updateRoute(coordinates: { lat: number; lng: number }[]): void {
    // Supprimer la route précédente
    this.routeLayer.getSource().clear();

    // Convertir les coordonnées pour OpenLayers
    const routeCoordinates = coordinates.map(coord => 
      ol.proj.fromLonLat([coord.lng, coord.lat])
    );

    // Créer une ligne pour la route
    const routeFeature = new ol.Feature({
      geometry: new ol.geom.LineString(routeCoordinates)
    });

    // Style pour la route
    routeFeature.setStyle(new ol.style.Style({
      stroke: new ol.style.Stroke({
        color: '#3498db',
        width: 4
      })
    }));

    // Ajouter la route à la couche
    this.routeLayer.getSource().addFeature(routeFeature);
  }

  /**
   * Met à jour le marqueur de ramassage
   * @param location Coordonnées du point de ramassage
   */
  private updatePickupMarker(location: { lat: number; lng: number; address?: string }): void {
    // Supprimer le marqueur précédent
    if (this.pickupMarker) {
      this.currentLocationLayer.getSource().removeFeature(this.pickupMarker);
    }

    // Créer un nouveau marqueur
    this.pickupMarker = new ol.Feature({
      geometry: new ol.geom.Point(ol.proj.fromLonLat([location.lng, location.lat])),
      properties: {
        name: 'Point de ramassage',
        type: 'pickup',
        address: location.address
      }
    });

    // Style pour le marqueur de ramassage
    this.pickupMarker.setStyle(new ol.style.Style({
      image: new ol.style.Icon({
        anchor: [0.5, 1],
        src: 'assets/pickup-icon.png',
        scale: 0.5
      })
    }));

    // Ajouter le marqueur à la couche
    this.currentLocationLayer.getSource().addFeature(this.pickupMarker);
  }

  /**
   * Met à jour le marqueur de livraison
   * @param location Coordonnées du point de livraison
   */
  private updateDeliveryMarker(location: { lat: number; lng: number; address?: string }): void {
    // Supprimer le marqueur précédent
    if (this.deliveryMarker) {
      this.currentLocationLayer.getSource().removeFeature(this.deliveryMarker);
    }

    // Créer un nouveau marqueur
    this.deliveryMarker = new ol.Feature({
      geometry: new ol.geom.Point(ol.proj.fromLonLat([location.lng, location.lat])),
      properties: {
        name: 'Point de livraison',
        type: 'delivery',
        address: location.address
      }
    });

    // Style pour le marqueur de livraison
    this.deliveryMarker.setStyle(new ol.style.Style({
      image: new ol.style.Icon({
        anchor: [0.5, 1],
        src: 'assets/delivery-icon.png',
        scale: 0.5
      })
    }));

    // Ajouter le marqueur à la couche
    this.currentLocationLayer.getSource().addFeature(this.deliveryMarker);
  }
}

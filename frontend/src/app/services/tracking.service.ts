import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class TrackingService {
  private apiUrl = 'http://localhost:8083/api/tracking';

  constructor(private http: HttpClient, private authService: AuthService) { }

  getTrackingEvents(deliveryId: string): Observable<any[]> {
    const headers = this.authService.getHeaders();
    return this.http.get<any[]>(`${this.apiUrl}/events/${deliveryId}`, { headers });
  }

  getCurrentLocation(deliveryId: string): Observable<any> {
    const headers = this.authService.getHeaders();
    return this.http.get<any>(`${this.apiUrl}/location/${deliveryId}`, { headers });
  }

  getDeliveryHistory(deliveryId: string): Observable<any[]> {
    const headers = this.authService.getHeaders();
    return this.http.get<any[]>(`${this.apiUrl}/history/${deliveryId}`, { headers });
  }

  subscribeToUpdates(deliveryId: string): Observable<any> {
    // Dans un vrai projet, ceci utiliserait WebSocket pour des mises à jour en temps réel
    // Pour cet exemple, nous simulons avec des intervalles
    return new Observable(observer => {
      const interval = setInterval(() => {
        this.getCurrentLocation(deliveryId).subscribe({
          next: (location) => {
            observer.next({
              type: 'LOCATION_UPDATE',
              deliveryId: deliveryId,
              location: location,
              timestamp: new Date()
            });
          },
          error: (err) => {
            observer.error(err);
          }
        });
      }, 10000); // Mise à jour toutes les 10 secondes

      // Nettoyage lors de la désinscription
      return {
        unsubscribe() {
          clearInterval(interval);
        }
      };
    });
  }
}
